package com.java.persistence.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.java.domain.AuthProvider;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "user_auth_providers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_auth_provider_user_provider", columnNames = {"user_id", "provider"}),
        @UniqueConstraint(name = "uk_user_auth_provider_provider_user_id", columnNames = {"provider", "provider_user_id"})
    }
)
@Getter
@Setter
public class UserAuthProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_auth_provider_user"))
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "provider", nullable = false, length = 32)
    private AuthProvider provider;

    @Column(name = "provider_user_id", length = 191)
    private String providerUserId;

    @Column(name = "provider_email", length = 191)
    private String providerEmail;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}