package com.cdweb.be.repository;

import com.cdweb.be.entity.UserAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// Sửa Integer thành Long ở đây
public interface AddressRepository extends JpaRepository<UserAddress, Long> {

  List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

  // Sửa Integer thành Long
  Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

  // Sửa Integer thành Long
  boolean existsByIdAndUserId(Long id, Long userId);

  @Modifying
  @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.id <> :excludeId")
  void unsetDefaultForUser(@Param("userId") Long userId, @Param("excludeId") Long excludeId); // Sửa Integer thành Long

  @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.user.id = :userId")
  long countByUserId(@Param("userId") Long userId); // Sửa Integer thành Long
}