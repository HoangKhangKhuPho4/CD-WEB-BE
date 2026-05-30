package com.cdweb.be.repository;

import com.cdweb.be.entity.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

  Optional<Permission> findByCode(String code);

  List<Permission> findByCodeIn(Collection<String> codes);
}
