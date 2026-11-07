package com.example.booking;

import static org.springframework.http.HttpStatus.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
  private final AccountRepository accounts;
  private final JwtEncoder encoder;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

  public AuthController(AccountRepository a, JwtEncoder e) {
    accounts = a;
    encoder = e;
  }

  public record Credentials(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record AdminInput(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Size(min = 8, max = 72) String password,
      @Pattern(regexp = "USER|ADMIN") @NotNull String role) {}

  @PostMapping("/user/register")
  public Map<String, String> register(@Valid @RequestBody Credentials in) {
    return token(create(in.username(), in.password(), "USER"));
  }

  @PostMapping("/user/auth")
  public Map<String, String> auth(@Valid @RequestBody Credentials in) {
    var a =
        accounts
            .findByUsername(in.username())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    if (!passwords.matches(in.password(), a.password))
      throw new ResponseStatusException(UNAUTHORIZED);
    return token(a);
  }

  private Account create(String name, String password, String role) {
    var a = new Account();
    a.username = name;
    a.password = passwords.encode(password);
    a.role = role;
    return accounts.saveAndFlush(a);
  }

  private Map<String, String> token(Account a) {
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("hotel-booking")
            .subject(a.id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("roles", List.of(a.role))
            .build();
    return Map.of(
        "token",
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue());
  }

  @GetMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Account> users() {
    return accounts.findAll();
  }

  @PostMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  public Account add(@Valid @RequestBody AdminInput in) {
    return create(in.username(), in.password(), in.role());
  }

  @PatchMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  public Account update(@RequestParam Long id, @Valid @RequestBody AdminInput in) {
    var a = accounts.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    a.username = in.username();
    a.password = passwords.encode(in.password());
    a.role = in.role();
    return accounts.save(a);
  }

  @DeleteMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@RequestParam Long id) {
    accounts.deleteById(id);
  }

  @Bean
  org.springframework.boot.CommandLineRunner seed(
      @org.springframework.beans.factory.annotation.Value("${demo.admin-password:Admin12345!}")
          String password) {
    return args -> create("admin", password, "ADMIN");
  }
}
