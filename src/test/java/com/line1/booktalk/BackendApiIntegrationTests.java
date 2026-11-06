package com.line1.booktalk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApiIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void guestRefreshAndChildProfileFlow() throws Exception {
        String loginJson = mvc.perform(post("/auth/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"integration-device-1","locale":"ko"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn().getResponse().getContentAsString();

        String access = jsonString(loginJson, "accessToken");
        String refresh = jsonString(loginJson, "refreshToken");

        mvc.perform(post("/children")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"민준","age":4,"relationship":"MOTHER","explanationLanguage":"vi"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("민준"));

        mvc.perform(get("/children").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String refreshedJson = mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(jsonString(refreshedJson, "refreshToken")).isNotEqualTo(refresh);

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));
    }

    private String jsonString(String json, String field) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("JSON field not found: " + field);
        return matcher.group(1);
    }
}
