package com.example.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class AuthTest {
  @Autowired MockMvc mvc;

  @Test
  void registerReturnsJwt() throws Exception {
    mvc.perform(
            post("/user/register")
                .contentType("application/json")
                .content("{\"username\":\"test-user\",\"password\":\"Password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  void wrongPasswordRejected() throws Exception {
    mvc.perform(
            post("/user/auth")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"Wrong1234\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousBookingRejected() throws Exception {
    mvc.perform(get("/bookings")).andExpect(status().isUnauthorized());
  }
}
