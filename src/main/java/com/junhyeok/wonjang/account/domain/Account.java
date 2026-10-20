package com.junhyeok.wonjang.account.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// accounts 엔티티
@Entity
@Table(name = "accounts")
public class Account {

    // id (auto-increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 계정 코드 — 유일해야 하므로 unique=true
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    // 없으면 0,1,2... 숫자로 저장됨 — 반드시 STRING으로 지정
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountType type;

    // 삭제 없이 비활성화로만 처리 (Append-only 원칙)
    private boolean active = true;

    // updatable=false: 한 번 INSERT되면 이후 UPDATE 불가
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // INSERT 직전에 JPA가 자동 호출 — 생성자에서 직접 세팅할 필요 없음
    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    // JPA가 내부적으로 객체를 만들 때 사용. 외부에서 직접 생성하면 안 되므로 protected
    protected Account() {}

    public Account(String code, String name, AccountType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    // 삭제 대신 비활성화 — 원장 데이터는 지우지 않는다
    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public AccountType getType() { return type; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}