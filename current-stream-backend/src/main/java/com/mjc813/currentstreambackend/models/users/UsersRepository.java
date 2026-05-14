package com.mjc813.currentstreambackend.models.users;

import org.springframework.data.jpa.repository.JpaRepository;

/*
    CRUD 기능을 자동으로 제공하는 JpaRepository를 상속한 인터페이스
 */
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {
}
