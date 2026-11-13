package com.line1.booktalk.domain.speech;

import com.line1.booktalk.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI가 만든 읽어 주기(TTS) 음성.
 * TTS는 하루 한도가 서버 전체 공용이라, 한 번 만든 문장은 DB에 저장해 두고 다시 만들지 않는다.
 * (메모리에만 두면 Render 무료 서버가 잠들 때마다 사라진다)
 */
@Getter
@Entity
@Table(name = "tts_audios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TtsAudio extends BaseEntity {

    /** 문장의 sha256 */
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 300)
    private String text;

    /** wav 파일 (PostgreSQL에서는 bytea) */
    @Column(nullable = false, length = 10_000_000)
    private byte[] audio;

    @Column(nullable = false)
    private int durationMs;

    static TtsAudio create(String id, String text, byte[] audio, int durationMs) {
        TtsAudio t = new TtsAudio();
        t.id = id;
        t.text = text;
        t.audio = audio;
        t.durationMs = durationMs;
        return t;
    }
}
