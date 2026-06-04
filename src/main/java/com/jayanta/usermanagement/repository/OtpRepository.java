package com.jayanta.usermanagement.repository;

import com.jayanta.usermanagement.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmailAndCode(String email, String code);

    @Query("DELETE FROM Otp o WHERE o.email = :email")
    void deleteByEmail(@Param("email") String email);

    Optional<Otp> findFirstByEmailOrderByCreatedAtDesc(String email);
}
