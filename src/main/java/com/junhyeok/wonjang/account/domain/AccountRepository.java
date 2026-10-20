package com.junhyeok.wonjang.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // code 유무 조회
    boolean existsByCode(String code);
}