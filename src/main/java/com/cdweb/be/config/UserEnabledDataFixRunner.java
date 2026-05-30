package com.cdweb.be.config;

import com.cdweb.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Sửa các bản ghi {@code users.enabled IS NULL} (dữ liệu cũ) để tránh NPE khi đăng nhập.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEnabledDataFixRunner implements ApplicationRunner {

  private final UserRepository userRepository;

  @Override
  public void run(ApplicationArguments args) {
    int updated = userRepository.fixNullEnabledFlags();
    if (updated > 0) {
      log.warn("Đã cập nhật enabled=true cho {} user có enabled NULL", updated);
    }
  }
}
