package com.line1.booktalk.domain.speech;

import com.line1.booktalk.domain.ai.LearningAiClient;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.common.HashUtils;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 음성 합성(TTS)과 음성 인식(STT).
 * app.ai.features=remote면 AI 서버(Gemini)를 쓰고, fake면 무음 파일 / 안내 문구를 돌려준다.
 */
@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {
    /** 만든 음성은 서버 메모리에 잠깐 보관한다 (오래된 것부터 지움) */
    private static final int CACHE_SIZE = 200;
    private static final Set<String> AUDIO_TYPES = Set.of("audio/wav", "audio/mp3", "audio/mpeg", "audio/aiff",
            "audio/aac", "audio/ogg", "audio/flac");

    private final ChildService childService;
    private final LearningAiClient aiClient;
    private final Map<String, byte[]> audioCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) { return size() > CACHE_SIZE; }
    });

    /** 한국어 문장을 읽어 준다. audioUrl로 GET 하면 wav 파일이 나온다 */
    @PostMapping("/synthesis")
    ApiResponse<SynthesisResponse> synthesis(@LoginUser String userId, @Valid @RequestBody SynthesisRequest r) {
        String text = r.text().trim();
        if (text.matches("(?i).*(kill|suicide|자살|죽여).*")) throw new BusinessException(ErrorCode.TEXT_NOT_ALLOWED);
        double speed = r.speed() == null ? 1.0 : r.speed();
        String key = HashUtils.sha256(text);
        boolean cached = audioCache.containsKey(key);
        byte[] wav = cached ? audioCache.get(key)
                : aiClient.isRemote() ? aiClient.synthesize(text, speed) : silentWav(Math.max(400, text.length() * 120));
        audioCache.put(key, wav);
        // 같은 음성 파일을 쓰고, 속도는 앱에서 audio.playbackRate = speed 로 맞춘다
        return ApiResponse.of(new SynthesisResponse("/v1/speech/audio/" + key + ".wav", durationMs(wav), cached, speed,
                Instant.now().plus(Duration.ofHours(6))));
    }

    /** 만든 음성 파일. <audio src>에서 바로 쓰도록 로그인 없이 열려 있다 */
    @GetMapping("/audio/{key}.wav")
    ResponseEntity<byte[]> audio(@PathVariable String key) {
        byte[] wav = audioCache.get(key);
        if (wav == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6))).body(wav);
    }

    /** 녹음 파일(multipart의 audio)을 글자로 바꾼다. 결과는 바로 돌려준다 */
    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<TranscriptionResponse> transcription(@LoginUser String userId,
                                                     @RequestPart("audio") MultipartFile audio,
                                                     @RequestParam(required = false) String childId) throws IOException {
        if (childId != null && !childId.isBlank()) childService.getOwnedChild(userId, childId);
        if (audio.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "녹음 파일이 비어 있어요.");
        String mimeType = audio.getContentType() == null ? "" : audio.getContentType().split(";")[0].trim().toLowerCase();
        if (mimeType.equals("audio/x-wav") || mimeType.equals("audio/wave")) mimeType = "audio/wav";
        if (!AUDIO_TYPES.contains(mimeType))
            throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "wav, mp3, aac, ogg, flac 음성만 인식할 수 있어요.");
        String id = "stt_" + UUID.randomUUID().toString().replace("-", "");
        if (!aiClient.isRemote())
            return ApiResponse.of(new TranscriptionResponse(id, "COMPLETED", "(테스트) AI 서버를 켜면 실제로 인식해요."));
        LearningAiClient.Transcription t = aiClient.transcribe(audio.getBytes(), mimeType);
        boolean noSpeech = "no_speech".equals(t.status()) || t.text() == null || t.text().isBlank();
        return ApiResponse.of(new TranscriptionResponse(id, noSpeech ? "NO_SPEECH" : "COMPLETED", noSpeech ? "" : t.text().trim()));
    }

    /** wav 길이 계산 (AI 서버 음성: 24kHz, 16bit, 1채널) */
    private static int durationMs(byte[] wav) {
        if (wav.length <= 44) return 0;
        ByteBuffer b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int byteRate = b.getInt(28);
        return byteRate <= 0 ? 0 : (int) ((wav.length - 44) * 1000L / byteRate);
    }

    /** AI 서버 없이 테스트할 때 쓰는 무음 wav */
    private static byte[] silentWav(int ms) {
        int rate = 24_000; int dataSize = rate * 2 * ms / 1000;
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + dataSize).put("WAVE".getBytes()).put("fmt ".getBytes())
                .putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
                .putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(dataSize);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(h.array()); out.writeBytes(new byte[dataSize]);
        return out.toByteArray();
    }

    public record SynthesisRequest(@NotBlank @Size(max = 300) String text, @NotBlank String language,
                                   String voice, @DecimalMin("0.7") @DecimalMax("1.2") Double speed) {}
    public record SynthesisResponse(String audioUrl, int durationMs, boolean cached, double playbackRate, Instant expiresAt) {}
    /** status: COMPLETED(인식됨) / NO_SPEECH(말소리가 없음) */
    public record TranscriptionResponse(String id, String status, String text) {}
}
