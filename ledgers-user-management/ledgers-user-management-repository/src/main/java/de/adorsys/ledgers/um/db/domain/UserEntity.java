/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.um.db.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "login", name = UserEntity.USER_LOGIN_UNIQUE),
        @UniqueConstraint(columnNames = "email", name = UserEntity.USER_EMAIL_UNIQUE)
})
public class UserEntity {

    public static final String USER_LOGIN_UNIQUE = "user_login_unique";
    public static final String USER_EMAIL_UNIQUE = "user_email_unique";

    @Id
    @Column(name = "user_id")
    private String id;

    @Column(nullable = false)
    private String login;

    @Column(nullable = false)
    private String email;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<ScaUserDataEntity> scaUserData = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<AccountAccess> accountAccesses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Collection<UserRole> userRoles = new ArrayList<>();

    private String branch;

    @Column(name = "block")
    private boolean blocked;

    @Column(name = "system_block")
    private boolean systemBlocked;

    @Column
    @CreationTimestamp
    private LocalDateTime created;

    public boolean isEnabled() {
        return !isBlocked() && !isSystemBlocked();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserEntity)) {
            return false;
        }
        UserEntity that = (UserEntity) o;
        return Objects.equals(getId(), that.getId()) &&
                       Objects.equals(getLogin(), that.getLogin()) &&
                       Objects.equals(getEmail(), that.getEmail()) &&
                       Objects.equals(getScaUserData(), that.getScaUserData()) &&
                       Objects.equals(getAccountAccesses(), that.getAccountAccesses()) &&
                       Objects.equals(getUserRoles(), that.getUserRoles()) &&
                       Objects.equals(getBranch(), that.getBranch()) &&
                       Objects.equals(isBlocked(), that.isBlocked()) &&
                       Objects.equals(isSystemBlocked(), that.isSystemBlocked());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getLogin(), getEmail(), getScaUserData(), getAccountAccesses(), getUserRoles(), getBranch(), isBlocked(), isSystemBlocked());
    }
}