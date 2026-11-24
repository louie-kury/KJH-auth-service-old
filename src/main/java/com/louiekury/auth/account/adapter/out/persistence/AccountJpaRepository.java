package com.louiekury.auth.account.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    Optional<AccountJpaEntity> findByUuid(String uuid);

    Optional<AccountJpaEntity> findByEmail(String email);
}
