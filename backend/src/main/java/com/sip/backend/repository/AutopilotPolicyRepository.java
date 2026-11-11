package com.sip.backend.repository;

import com.sip.backend.entity.AutopilotPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutopilotPolicyRepository extends JpaRepository<AutopilotPolicy, String> {
}