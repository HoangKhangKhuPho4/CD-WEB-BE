package com.cdweb.be.repository;

import com.cdweb.be.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

  @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
  Optional<Cart> findByUserId(@Param("userId") Integer userId);
}
