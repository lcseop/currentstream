package com.mjc813.currentstreambackend.models.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
    CRUD 기능을 자동으로 제공하는 JpaRepository를 상속한 인터페이스
 */
@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {
    Optional<UsersEntity> findByUid(String uid);

    boolean existsByTag(String tag);
}
