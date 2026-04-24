package com.cdweb.be.repository;

import com.cdweb.be.entity.AttributeValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {
  List<AttributeValue> findByAttribute_Id(Integer attributeId);
}
