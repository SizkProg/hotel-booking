package com.example.hotel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class HotelSecurityTest {
  @Autowired MockMvc mvc;

  @Test
  void anonymousIsUnauthorized() throws Exception {
    mvc.perform(get("/api/hotels")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  void userCannotCreateHotel() throws Exception {
    mvc.perform(
            post("/api/hotels")
                .contentType("application/json")
                .content("{\"name\":\"Hotel\",\"address\":\"Address\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanCreateHotel() throws Exception {
    mvc.perform(
            post("/api/hotels")
                .contentType("application/json")
                .content("{\"name\":\"Hotel\",\"address\":\"Address\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidHotelRejected() throws Exception {
    mvc.perform(
            post("/api/hotels")
                .contentType("application/json")
                .content("{\"name\":\"\",\"address\":\"Address\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void internalEndpointNeedsSecret() throws Exception {
    mvc.perform(
            post("/api/rooms/1/release")
                .header("X-Internal-Token", "wrong")
                .contentType("application/json")
                .content("{\"requestId\":\"11111111-1111-1111-1111-111111111111\"}"))
        .andExpect(status().isForbidden());
  }
}
