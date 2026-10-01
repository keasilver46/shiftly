package com.shiftly.domain.user.repository;

import com.shiftly.domain.user.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {}
