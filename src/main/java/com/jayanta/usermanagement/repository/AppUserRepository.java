package com.jayanta.usermanagement.repository;

import com.jayanta.usermanagement.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.isInternal = true")
    List<AppUser> findAllInternalUsers();

    @Query("SELECT u FROM AppUser u WHERE u.isInternal = false")
    List<AppUser> findAllExternalUsers();

}
