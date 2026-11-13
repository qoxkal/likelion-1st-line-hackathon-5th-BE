package com.line1.booktalk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

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

    @Test
    void demoBooksExposeVocabularyAndActiveConversation() throws Exception {
        String loginJson = mvc.perform(post("/auth/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"demo-integration-device","locale":"ko"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String access = jsonString(loginJson, "accessToken");

        String childJson = mvc.perform(post("/children")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"시연아이","age":5,"relationship":"GUARDIAN","explanationLanguage":"vi"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String childId = jsonString(childJson, "id");

        String booksJson = mvc.perform(get("/books")
                        .param("source", "DEMO")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[?(@.title == '토끼의 당근 밭')].expressions.length()").value(5))
                .andExpect(jsonPath("$.data[?(@.title == '토끼의 당근 밭')].expressions[*].text")
                        .value(containsInAnyOrder("두근두근", "퐁당", "엉엉", "반짝반짝", "눈이 휘둥그레지다")))
                .andExpect(jsonPath("$.data[?(@.title == '곰곰이의 비 오는 날')].expressions.length()").value(4))
                .andExpect(jsonPath("$.data[?(@.title == '곰곰이의 비 오는 날')].expressions[*].text")
                        .value(containsInAnyOrder("첨벙첨벙", "퐁당", "엉금엉금", "꾸벅꾸벅")))
                .andExpect(jsonPath("$.data[?(@.title == '토토의 밤 소풍')].expressions.length()").value(5))
                .andExpect(jsonPath("$.data[?(@.title == '토토의 밤 소풍')].expressions[*].text")
                        .value(containsInAnyOrder("두근두근", "깡충깡충", "살금살금", "데굴데굴", "반짝반짝")))
                .andExpect(jsonPath("$.data[?(@.title == '해와 달이 된 오누이')].expressions.length()").value(6))
                .andExpect(jsonPath("$.data[?(@.title == '해와 달이 된 오누이')].expressions[*].text")
                        .value(containsInAnyOrder("구불구불", "꿀꺽", "살금살금", "엉엉", "아등바등", "영차")))
                .andReturn().getResponse().getContentAsString();

        String vocabularyId = firstExpressionId(booksJson, "토끼의 당근 밭");

        mvc.perform(get("/vocabulary/{id}", vocabularyId)
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(vocabularyId));

        mvc.perform(post("/children/{childId}/sentence-practices", childId)
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vocabularyId":"%s","sentence":"마음이 두근두근해요.","inputMethod":"KEYBOARD"}
                                """.formatted(vocabularyId)))
                .andExpect(status().isCreated());

        String sessionJson = mvc.perform(post("/children/{childId}/conversation-sessions", childId)
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vocabularyId":"%s","scene":"당근 밭","difficulty":"LEVEL_1"}
                                """.formatted(vocabularyId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = jsonString(sessionJson, "id");

        mvc.perform(get("/children/{childId}/conversation-sessions", childId)
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(sessionId))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        String replacementJson = mvc.perform(post("/children/{childId}/conversation-sessions", childId)
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vocabularyId":"%s","scene":"밤 소풍","difficulty":"LEVEL_1"}
                                """.formatted(vocabularyId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String replacementId = jsonString(replacementJson, "id");

        mvc.perform(get("/conversation-sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mvc.perform(get("/children/{childId}/conversation-sessions", childId)
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(replacementId));
    }

    private String firstExpressionId(String json, String title) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"title\\\"\\s*:\\s*\\\"" + java.util.regex.Pattern.quote(title)
                        + "\\\".*?\\\"expressions\\\"\\s*:\\s*\\[\\s*\\{\\s*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
                        java.util.regex.Pattern.DOTALL)
                .matcher(json);
        if (!matcher.find()) throw new AssertionError("Expression not found for book: " + title);
        return matcher.group(1);
    }

    private String jsonString(String json, String field) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("JSON field not found: " + field);
        return matcher.group(1);
    }
}
