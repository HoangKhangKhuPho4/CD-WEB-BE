package com.cdweb.be.config;

import com.cdweb.be.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Đảm bảo bản ghi cấu hình singleton tồn tại khi khởi động. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationInitializer implements ApplicationRunner {

  private final SystemConfigService systemConfigService;

  @Override
  public void run(ApplicationArguments args) {
    systemConfigService.getOrCreate();
    log.debug("System configuration singleton ready (id=1)");
  }
}
