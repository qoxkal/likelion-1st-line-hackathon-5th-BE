package com.line1.booktalk.domain.speech;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TtsAudioRepository extends JpaRepository<TtsAudio, String> {
}
