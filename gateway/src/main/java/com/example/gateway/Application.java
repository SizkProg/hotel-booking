package com.example.gateway;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication(scanBasePackages = "com.example")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
