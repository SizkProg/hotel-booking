package com.example.booking;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class HotelClient {
  private final RestClient client;
  private final String secret;

  public HotelClient(
      @LoadBalanced RestClient.Builder builder,
      @Value("${hotel.url}") String url,
      @Value("${security.internal.secret}") String s) {
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(1000);
    f.setReadTimeout(1500);
    client = builder.requestFactory(f).baseUrl(url).build();
    secret = s;
  }

  public record RoomView(Long id, long timesBooked) {}

  public List<RoomView> recommend(Booking b, String jwt) {
    return retry(
        () -> {
          var result =
              client
                  .get()
                  .uri("/api/rooms/recommend?startDate={s}&endDate={e}", b.startDate, b.endDate)
                  .header("Authorization", "Bearer " + jwt)
                  .retrieve()
                  .body(RoomView[].class);
          return result == null ? List.of() : Arrays.asList(result);
        });
  }

  public void confirm(Booking b) {
    call(b, "confirm-availability");
  }

  public void release(Booking b) {
    call(b, "release");
  }

  private void call(Booking b, String op) {
    retry(
        () -> {
          client
              .post()
              .uri("/api/rooms/{id}/" + op, b.roomId)
              .header("X-Internal-Token", secret)
              .header("X-Request-Id", b.id.toString())
              .body(Map.of("requestId", b.id, "startDate", b.startDate, "endDate", b.endDate))
              .retrieve()
              .toBodilessEntity();
          return true;
        });
  }

  private <T> T retry(java.util.function.Supplier<T> action) {
    RuntimeException last = null;
    for (int i = 0; i < 3; i++) {
      try {
        return action.get();
      } catch (HttpClientErrorException e) {
        throw e;
      } catch (RestClientException e) {
        last = e;
        if (i < 2)
          try {
            Thread.sleep(100L * (1L << i));
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
          }
      }
    }
    throw last;
  }

  @Configuration
  public static class Config {
    @Bean
    @LoadBalanced
    RestClient.Builder clientBuilder() {
      return RestClient.builder();
    }
  }
}
