package com.cdweb.be.repository;

import com.cdweb.be.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsernameOrEmail(String username, String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  // Coalesce NULL (legacy) as active
  @Query("SELECT u FROM User u WHERE u.enabled = true OR u.enabled IS NULL")
  Page<User> findAllActive(Pageable pageable);

  @Modifying
  @Transactional
  @Query("UPDATE User u SET u.enabled = true WHERE u.enabled IS NULL")
  int fixNullEnabledFlags();

  // Đã sửa: status -> enabled, đổi tham số từ Integer sang Boolean
  @Query("SELECT u FROM User u WHERE u.enabled = :enabled")
  Page<User> findByStatus(@Param("enabled") Boolean enabled, Pageable pageable);

  // Đã sửa: u.name -> u.fullName
  @Query(
          "SELECT u FROM User u WHERE "
                  + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

  @Query(
      "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'CUSTOMER' "
          + "AND (u.enabled = true OR u.enabled IS NULL)")
  Page<User> findCustomers(Pageable pageable);

  @Query(
      "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'CUSTOMER' "
          + "AND (u.enabled = true OR u.enabled IS NULL) AND ("
          + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<User> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

  @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = 'CUSTOMER'")
  long countCustomerAccounts();
}