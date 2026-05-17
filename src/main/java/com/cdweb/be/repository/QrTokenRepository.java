package com.cdweb.be.repository;

import com.cdweb.be.entity.QrToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, Long> {

    Optional<QrToken> findBySessionId(String sessionId);

    Optional<QrToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE QrToken q SET q.status = 'EXPIRED' WHERE q.expiresAt < :now AND q.status = 'PENDING'")
    int expireOldTokens(LocalDateTime now);
}