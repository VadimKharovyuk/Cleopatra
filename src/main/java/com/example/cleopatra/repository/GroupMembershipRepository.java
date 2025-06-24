package com.example.cleopatra.repository;

import com.example.cleopatra.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
}
