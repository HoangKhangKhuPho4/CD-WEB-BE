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
public interface AddressRepository extends JpaRepository<UserAddress, Integer> {

  List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Integer userId);

  Optional<UserAddress> findByIdAndUserId(Integer id, Integer userId);

  boolean existsByIdAndUserId(Integer id, Integer userId);

  @Modifying
  @Query(
      "UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.id <> :excludeId")
  void unsetDefaultForUser(@Param("userId") Integer userId, @Param("excludeId") Integer excludeId);

  @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.user.id = :userId")
  long countByUserId(@Param("userId") Integer userId);
}
