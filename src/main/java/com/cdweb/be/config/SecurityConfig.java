package com.cdweb.be.config;

import com.cdweb.be.util.JwtAuthenticationEntryPoint;
import com.cdweb.be.util.JwtAuthenticationFilter;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${cors.allowed.origins:http://localhost:3000,http://localhost:5173}")
  private String[] allowedOrigins;

  public SecurityConfig(
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    // Public endpoints
                    .requestMatchers(
                        "/img/**",
                        "/api/auth/**",
                        "/api/public/**",
                        "/api/qr/generate",
                        "/api/qr/status/**",
                        "/api/shipping/**", // GHN Proxy — địa chỉ & phí vận chuyển (PUBLIC)
                        "/api/ghn/provinces",
                        "/api/ghn/districts",
                        "/api/ghn/wards",
                        "/api/ghn/calculate-fee",
                        "/api/ghn/checkout",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs",
                        "/swagger-ui.html",
                        "/favicon.ico",
                        "/actuator/**" // optional
                        )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/product-types")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/producers/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/summary")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/recent")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/settings/general")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/cms/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/reviews/*/helpful")
                    .permitAll()

                    // Payment Gateway callbacks (public — gateway gọi trực tiếp, không có JWT)
                    .requestMatchers("/api/payment/vnpay/ipn")
                    .permitAll()
                    .requestMatchers("/api/payment/vnpay/return")
                    .permitAll()
                    .requestMatchers("/api/payment/momo/ipn")
                    .permitAll()
                    .requestMatchers("/api/payment/momo/return")
                    .permitAll()
                    .requestMatchers("/api/payment/zalopay/callback")
                    .permitAll()
                    .requestMatchers("/api/payment/zalopay/return")
                    .permitAll()

                    // Admin & Manage endpoints
                    .requestMatchers(HttpMethod.POST, "/api/products")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers(HttpMethod.POST, "/api/categories")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers(HttpMethod.PUT, "/api/categories/**")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers(HttpMethod.DELETE, "/api/categories/**")
                    .hasAuthority("PRODUCT_MANAGE")
                    .requestMatchers("/api/admin/**")
                    .authenticated()

                    // User endpoints
                    .requestMatchers("/api/cart/**")
                    .authenticated()
                    .requestMatchers("/api/orders/**")
                    .authenticated()
                    .requestMatchers("/api/payment/create")
                    .authenticated()
                    .requestMatchers("/api/payment/status/**")
                    .authenticated()
                    .requestMatchers("/api/payment/history/**")
                    .authenticated()
                    .requestMatchers("/api/users/profile/**")
                    .authenticated()

                    // All other requests need authentication
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 🎯 Specific allowed origins from properties (SECURE)
    // For development we allow any localhost port pattern so we don't need
    // to add each dev port manually (e.g., 5173, 5174...). In production
    // keep explicit origins only.
    configuration.setAllowedOriginPatterns(
        Arrays.asList(
            "http://localhost:*", "http://127.0.0.1:*", "http://10.*:*", "http://192.168.*:*"));

    // 📡 Allowed HTTP methods
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // 📝 Allowed headers
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));

    // 📤 Exposed headers (frontend có thể đọc)
    configuration.setExposedHeaders(
        Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));

    // 🔐 Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // ⏱️ Preflight request cache duration
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
