package com.line1.booktalk.domain.speech;

import com.line1.booktalk.domain.ai.LearningAiClient;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.global.auth.LoginUser;
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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 음성 합성(TTS)과 음성 인식(STT).
 * app.ai.features=remote면 AI 서버(Gemini)를 쓰고, fake면 무음 파일 / 안내 문구를 돌려준다.
 * 읽어 주기 음성은 TtsService가 DB에 저장해 두고 다시 쓴다.
 */
@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {
    private static final Set<String> AUDIO_TYPES = Set.of("audio/wav", "audio/mp3", "audio/mpeg", "audio/aiff",
            "audio/aac", "audio/ogg", "audio/flac");

    private final ChildService childService;
    private final LearningAiClient aiClient;
    private final TtsService ttsService;

    /** 한국어 문장을 읽어 준다. audioUrl로 GET 하면 wav 파일이 나온다 */
    @PostMapping("/synthesis")
    ApiResponse<SynthesisResponse> synthesis(@LoginUser String userId, @Valid @RequestBody SynthesisRequest r) {
        String text = r.text().trim();
        if (text.matches("(?i).*(kill|suicide|자살|죽여).*")) throw new BusinessException(ErrorCode.TEXT_NOT_ALLOWED);
        double speed = r.speed() == null ? 1.0 : r.speed();
        // 한 번 만든 문장은 DB에서 꺼내 쓴다 (하루 한도를 아끼고, 서버가 잠들어도 남아 있다)
        TtsService.Result result = ttsService.getOrCreate(text, speed);
        // 같은 음성 파일을 쓰고, 속도는 앱에서 audio.playbackRate = speed 로 맞춘다
        return ApiResponse.of(new SynthesisResponse("/v1/speech/audio/" + result.audio().getId() + ".wav",
                result.audio().getDurationMs(), result.cached(), speed, Instant.now().plus(Duration.ofDays(30))));
    }

    /** 만든 음성 파일. <audio src>에서 바로 쓰도록 로그인 없이 열려 있다 */
    @GetMapping("/audio/{key}.wav")
    ResponseEntity<byte[]> audio(@PathVariable String key) {
        TtsAudio audio = ttsService.find(key).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7))).body(audio.getAudio());
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

    public record SynthesisRequest(@NotBlank @Size(max = 300) String text, @NotBlank String language,
                                   String voice, @DecimalMin("0.7") @DecimalMax("1.2") Double speed) {}
    public record SynthesisResponse(String audioUrl, int durationMs, boolean cached, double playbackRate, Instant expiresAt) {}
    /** status: COMPLETED(인식됨) / NO_SPEECH(말소리가 없음) */
    public record TranscriptionResponse(String id, String status, String text) {}
}
