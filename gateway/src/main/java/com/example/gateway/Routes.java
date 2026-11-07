package com.example.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.*;

@Configuration
public class Routes {
  @Bean
  RouteLocator hotelBookingRoutes(RouteLocatorBuilder b) {
    return b.routes()
        .route(
            "internal-denied",
            r ->
                r.path("/api/rooms/*/release", "/api/rooms/*/confirm-availability")
                    .filters(f -> f.setStatus(404))
                    .uri("no://op"))
        .route("hotel", r -> r.path("/api/hotels/**", "/api/rooms/**").uri("lb://hotel"))
        .route(
            "booking",
            r ->
                r.path("/booking/**", "/bookings/**", "/user/**", "/api/bookings/**")
                    .filters(f -> f.rewritePath("/api/bookings(?<rest>/?.*)", "/booking${rest}"))
                    .uri("lb://booking"))
        .build();
  }

  @Bean
  GlobalFilter trace() {
    return (exchange, chain) -> {
      String id = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
      if (id == null || !id.matches("[a-zA-Z0-9-]{1,80}")) id = UUID.randomUUID().toString();
      exchange.getResponse().getHeaders().set("X-Request-Id", id);
      return chain.filter(
          exchange
              .mutate()
              .request(
                  exchange
                      .getRequest()
                      .mutate()
                      .headers(h -> h.remove("X-Internal-Token"))
                      .header("X-Request-Id", id)
                      .build())
              .build());
    };
  }
}
