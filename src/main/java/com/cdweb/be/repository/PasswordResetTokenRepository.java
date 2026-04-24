package com.cdweb.be.repository;

import com.cdweb.be.entity.PasswordResetToken;
import com.cdweb.be.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByToken(String token);

  Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

  void deleteByUser(User user);
}
