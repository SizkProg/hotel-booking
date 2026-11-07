package com.example.booking;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class HotelClientTest {
  private Booking booking() {
    var b = new Booking();
    b.id = UUID.randomUUID();
    b.roomId = 1L;
    b.startDate = LocalDate.now().plusDays(1);
    b.endDate = b.startDate.plusDays(2);
    return b;
  }

  @Test
  void retriesServerErrorsAndPreservesIdempotencyKey() throws Exception {
    var count = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var b = booking();
    server.createContext(
        "/",
        exchange -> {
          var body =
              new String(
                  exchange.getRequestBody().readAllBytes(),
                  java.nio.charset.StandardCharsets.UTF_8);
          int status =
              body.contains(b.id.toString())
                      && "internal"
                          .equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))
                  ? (count.incrementAndGet() < 3 ? 503 : 204)
                  : 400;
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });
    server.start();
    try {
      var client =
          new HotelClient(
              RestClient.builder(),
              "http://127.0.0.1:" + server.getAddress().getPort(),
              "internal");
      client.confirm(b);
      assertEquals(3, count.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void conflictIsNotRetried() throws Exception {
    var count = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          count.incrementAndGet();
          exchange.sendResponseHeaders(409, -1);
          exchange.close();
        });
    server.start();
    try {
      var client =
          new HotelClient(
              RestClient.builder(),
              "http://127.0.0.1:" + server.getAddress().getPort(),
              "internal");
      assertThrows(HttpClientErrorException.Conflict.class, () -> client.confirm(booking()));
      assertEquals(1, count.get());
    } finally {
      server.stop(0);
    }
  }
}
