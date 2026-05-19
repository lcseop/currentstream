package com.mjc813.currentstreambackend.models.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InviteRepository extends JpaRepository<InviteEntity, Long> {
    boolean existsByTeamIdAndUserIdAndStatus(Long teamId, Long id, int i);

    List<InviteEntity> findByUserIdAndStatus(Long id, int i);

    void deleteByTeamId(Long teamId);
}
