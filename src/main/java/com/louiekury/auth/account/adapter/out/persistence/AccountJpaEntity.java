package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.global.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String uuid;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "terms_agreed", nullable = false)
    private boolean termsAgreed;

    public AccountJpaEntity(
            String uuid,
            String status,
            String name,
            String phoneNumber,
            String gender,
            LocalDate birthDate,
            String address,
            String email,
            boolean termsAgreed
    ) {
        this.uuid = uuid;
        this.status = status;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.email = email;
        this.termsAgreed = termsAgreed;
    }
}
