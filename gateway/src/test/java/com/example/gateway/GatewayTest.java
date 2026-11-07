package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "eureka.client.enabled=false")
class GatewayTest {
  @LocalServerPort int port;

  @Test
  void internalRouteIsClosed() {
    WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build()
        .post()
        .uri("/api/rooms/1/release")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .exists("X-Request-Id");
  }
}
