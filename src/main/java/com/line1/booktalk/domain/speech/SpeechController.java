package com.line1.booktalk.domain.speech;

import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.common.HashUtils;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {
    private final ChildService childService;

    @PostMapping("/synthesis")
    ApiResponse<SynthesisResponse> synthesis(@LoginUser String userId, @Valid @RequestBody SynthesisRequest r) {
        String text = r.text().trim();
        if (text.matches("(?i).*(kill|suicide|자살|죽여).*")) throw new BusinessException(ErrorCode.TEXT_NOT_ALLOWED);
        double speed = r.speed() == null ? 1.0 : r.speed();
        String key = HashUtils.sha256(r.language() + "|" + (r.voice() == null ? "child-friendly-neutral" : r.voice()) + "|" + speed + "|" + text);
        return ApiResponse.of(new SynthesisResponse("/v1/media/tts/" + key + ".mp3", Math.max(400, text.length() * 120), true, Instant.now().plusSeconds(30L * 86400)));
    }

    @PostMapping("/transcriptions")
    ApiResponse<TranscriptionResponse> transcription(@LoginUser String userId, @Valid @RequestBody TranscriptionRequest r) {
        childService.getOwnedChild(userId, r.childId());
        return ApiResponse.of(new TranscriptionResponse("job_stt_" + java.util.UUID.randomUUID().toString().replace("-", ""), "QUEUED"));
    }

    public record SynthesisRequest(@NotBlank @Size(max = 300) String text, @NotBlank String language,
                                   String voice, @DecimalMin("0.7") @DecimalMax("1.2") Double speed) {}
    public record SynthesisResponse(String audioUrl, int durationMs, boolean cached, Instant expiresAt) {}
    public record TranscriptionRequest(@NotBlank String uploadId, @NotBlank String language, @NotBlank String childId) {}
    public record TranscriptionResponse(String jobId, String status) {}
}
