package com.java.persistence.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.java.domain.HomeUserRole;
import com.java.persistence.entity.HomeUserEntity;
import com.java.persistence.entity.HomeUserId;

public interface HomeUserRepository extends JpaRepository<HomeUserEntity, HomeUserId> {

    List<HomeUserEntity> findByUserId(Long userId);

    @Query("""
        select hu
        from HomeUserEntity hu
        join fetch hu.user u
        where hu.home.id = :homeId
        order by u.username asc
    """)
    List<HomeUserEntity> findAllByHomeId(@Param("homeId") Long homeId);

    @Query("""
        select hu
        from HomeUserEntity hu
        join fetch hu.user u
        where hu.home.id = :homeId
          and hu.user.id = :userId
    """)
    Optional<HomeUserEntity> findByHomeIdAndUserId(
            @Param("homeId") Long homeId,
            @Param("userId") Long userId
    );

    @Query("""
        select hu.home.id
        from HomeUserEntity hu
        where hu.user.id = :userId
          and hu.primary = true
    """)
    Optional<Long> findPrimaryHomeIdByUserId(@Param("userId") Long userId);

    @Query("""
        select hu.roleInHome
        from HomeUserEntity hu
        where hu.user.id = :userId
          and hu.primary = true
    """)
    Optional<HomeUserRole> findPrimaryHomeUserRoleIdByUserId(@Param("userId") Long userId);

    boolean existsByHomeIdAndUserId(Long homeId, Long userId);

    @Modifying
    @Query("""
        update HomeUserEntity hu
        set hu.primary = false
        where hu.user.id = :userId
    """)
    int clearPrimaryByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
        update HomeUserEntity hu
        set hu.primary = false
        where hu.home.id = :homeId
    """)
    int clearPrimaryByHomeId(@Param("homeId") Long homeId);
}