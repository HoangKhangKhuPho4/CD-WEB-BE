package com.cdweb.be;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
  public static void main(String[] args) {
    System.out.println("HASH=" + new BCryptPasswordEncoder().encode("admin123"));
  }
}
