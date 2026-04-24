package com.cdweb.be.repository;

import com.cdweb.be.entity.QrAuthToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QrAuthTokenRepository extends JpaRepository<QrAuthToken, Long> {

  Optional<QrAuthToken> findByTokenAndExpiryDateAfter(String token, LocalDateTime now);

  Optional<QrAuthToken> findByToken(String token);
}
