package com.line1.booktalk.domain.speech;

import com.line1.booktalk.domain.ai.LearningAiClient;
import com.line1.booktalk.global.common.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * 읽어 주기 음성을 DB에서 먼저 찾고, 없을 때만 AI 서버에 만들어 달라고 한다.
 * AI 서버 없이 테스트할 때(fake)는 무음 파일을 만든다.
 */
@Service
@RequiredArgsConstructor
public class TtsService {

    private final TtsAudioRepository repository;
    private final LearningAiClient aiClient;

    public record Result(TtsAudio audio, boolean cached) {}

    public static String keyOf(String text) {
        return HashUtils.sha256(text.trim());
    }

    public boolean isCached(String text) {
        return repository.existsById(keyOf(text));
    }

    public Optional<TtsAudio> find(String key) {
        return repository.findById(key);
    }

    /** 음성을 가져온다. 처음 보는 문장이면 AI로 만들어 저장한다 (한도 초과면 429 오류가 난다) */
    public Result getOrCreate(String text, double speed) {
        String trimmed = text.trim();
        String key = keyOf(trimmed);
        Optional<TtsAudio> saved = repository.findById(key);
        if (saved.isPresent()) return new Result(saved.get(), true);
        byte[] wav = aiClient.isRemote() ? aiClient.synthesize(trimmed, speed)
                : silentWav(Math.max(400, trimmed.length() * 120));
        try {
            return new Result(repository.save(TtsAudio.create(key, trimmed, wav, durationMs(wav))), false);
        } catch (DataIntegrityViolationException e) {
            // 미리 만들기와 사용자 요청이 같은 문장을 동시에 저장한 경우: 먼저 저장된 것을 쓴다
            return repository.findById(key).map(saved2 -> new Result(saved2, true)).orElseThrow(() -> e);
        }
    }

    /** wav 길이 계산 (AI 서버 음성: 24kHz, 16bit, 1채널) */
    static int durationMs(byte[] wav) {
        if (wav.length <= 44) return 0;
        int byteRate = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).getInt(28);
        return byteRate <= 0 ? 0 : (int) ((wav.length - 44) * 1000L / byteRate);
    }

    /** AI 서버 없이 테스트할 때 쓰는 무음 wav */
    static byte[] silentWav(int ms) {
        int rate = 24_000; int dataSize = rate * 2 * ms / 1000;
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + dataSize).put("WAVE".getBytes()).put("fmt ".getBytes())
                .putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
                .putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(dataSize);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(h.array()); out.writeBytes(new byte[dataSize]);
        return out.toByteArray();
    }
}
