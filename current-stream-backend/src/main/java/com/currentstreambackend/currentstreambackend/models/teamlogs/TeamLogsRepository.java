package com.currentstreambackend.currentstreambackend.models.teamlogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamLogsRepository extends JpaRepository<TeamLogsEntity, Long> {

    List<TeamLogsEntity> findTop10ByTeamIdOrderByCreatedAtDesc(Long teamId);

}