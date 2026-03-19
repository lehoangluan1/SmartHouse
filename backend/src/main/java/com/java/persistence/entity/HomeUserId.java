package com.java.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HomeUserId implements Serializable {

    private Long home;
    private Long user;

    public HomeUserId(Long home, Long user) {
        this.home = home;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HomeUserId that)) return false;
        return Objects.equals(home, that.home) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(home, user);
    }
}