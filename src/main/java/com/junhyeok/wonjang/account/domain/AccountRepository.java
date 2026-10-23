package com.junhyeok.wonjang.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // code 유무 조회
    boolean existsByCode(String code);

    // code 조회
    Optional<Account> findByCode(String code);
}