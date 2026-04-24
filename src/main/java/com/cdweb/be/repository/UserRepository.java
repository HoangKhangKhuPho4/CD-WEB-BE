package com.cdweb.be.repository;

import com.cdweb.be.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsernameOrEmail(String username, String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.status = 1")
  Page<User> findAllActive(Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.status = :status")
  Page<User> findByStatus(@Param("status") Integer status, Pageable pageable);

  @Query(
      "SELECT u FROM User u WHERE "
          + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}
