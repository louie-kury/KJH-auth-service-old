package com.louiekury.auth.account.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginCredentialJpaRepository extends JpaRepository<LoginCredentialJpaEntity, Long> {

    Optional<LoginCredentialJpaEntity> findByAccountId(Long accountId);

    Optional<LoginCredentialJpaEntity> findByLoginId(String loginId);
}
