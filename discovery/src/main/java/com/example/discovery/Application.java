package com.example.discovery;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication(scanBasePackages = "com.example")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
