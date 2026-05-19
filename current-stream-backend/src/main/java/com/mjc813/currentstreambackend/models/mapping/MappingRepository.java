package com.mjc813.currentstreambackend.models.mapping;

import com.mjc813.currentstreambackend.models.teams.TeamsDto;
import com.mjc813.currentstreambackend.models.teams.TeamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingRepository extends JpaRepository<MappingEntity, Long> {
    List<MappingEntity> findByUserId(Long userId);

    List<MappingEntity> findByTeamId(Long teamId);

    void deleteByUserIdAndTeamId(Long id, Long teamId);

    void deleteByTeamId(Long teamId);
}
