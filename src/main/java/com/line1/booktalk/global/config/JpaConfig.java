package com.line1.booktalk.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** createdAt, updatedAt을 자동으로 채우기 위한 설정 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
