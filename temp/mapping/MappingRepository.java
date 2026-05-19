package com.mjc813.currentstreambackend.models.mapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MappingRepository extends JpaRepository<MappingEntity, Long> {
}
