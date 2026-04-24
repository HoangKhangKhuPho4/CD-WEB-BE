package com.cdweb.be.repository;

import com.cdweb.be.entity.Attribute;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Integer> {
  Optional<Attribute> findByName(String name);
}
