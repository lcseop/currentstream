package com.currentstreambackend.currentstreambackend.models.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
    void deleteByTeamId(Long teamId);

    List<GoalEntity> findByTeamId(Long teamId);

    List<GoalEntity> findByTeamIdAndUserId(Long teamId, Long userId);
}
