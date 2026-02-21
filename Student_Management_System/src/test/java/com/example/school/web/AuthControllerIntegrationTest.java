package com.example.school.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerIntegrationTest {

    private static final String TEACHER_USERNAME = "auth_test_teacher";
    private static final String STUDENT_USERNAME = "auth_test_student";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/auth/me unauthenticated returns 401")
    void me_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me as teacher returns role TEACHER")
    @WithUserDetails(value = TEACHER_USERNAME, userDetailsServiceBeanName = "schoolUserDetailsService")
    void me_asTeacher_returnsTeacherRole() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEACHER_USERNAME))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @DisplayName("GET /api/auth/me as student returns role STUDENT")
    @WithUserDetails(value = STUDENT_USERNAME, userDetailsServiceBeanName = "schoolUserDetailsService")
    void me_asStudent_returnsStudentRole() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(STUDENT_USERNAME))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
