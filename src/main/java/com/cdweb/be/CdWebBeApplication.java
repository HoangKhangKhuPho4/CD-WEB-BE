package com.cdweb.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CdWebBeApplication {

  public static void main(String[] args) {
    SpringApplication.run(CdWebBeApplication.class, args);
  }
}
