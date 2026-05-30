package com.cdweb.be.repository;

import com.cdweb.be.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigurationRepository
    extends JpaRepository<SystemConfiguration, Long> {}
