package com.cdweb.be.repository;

import com.cdweb.be.entity.Image;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {

  List<Image> findByProductId(Integer productId);

  void deleteByProductId(Integer productId);
}
