package com.cdweb.be.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomCorsFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    final HttpServletResponse response = (HttpServletResponse) res;
    final HttpServletRequest request = (HttpServletRequest) req;

    // 🌍 CORS Headers
    String origin = request.getHeader("Origin");

    // 🎯 Whitelist allowed origins
    if (origin != null && isAllowedOrigin(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
    }

    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader(
        "Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control");

    // 🔍 Handle preflight requests
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    chain.doFilter(req, res);
  }

  private boolean isAllowedOrigin(String origin) {
    // 🎯 Define your allowed origins
    if (origin == null) return false;

    // Allow any localhost with any port during development
    if (origin.startsWith("http://localhost:")
        || origin.startsWith("http://127.0.0.1:")
        || origin.startsWith("http://192.168.")
        || origin.startsWith("http://10.")) {
      return true;
    }

    return origin.equals("http://localhost:3000")
        || origin.equals("http://localhost:4200")
        || origin.equals("http://localhost:8081")
        || origin.endsWith(".ngrok-free.app")
        || origin.startsWith("https://your-production-domain.com");
  }
}
