package com.account.repository;

import com.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserEmail(String userEmail);
    Optional<Account> findByAccno(String accno);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Account a SET a.pin = :newPin WHERE a.id = :id AND a.userEmail = :email")
    int updatePin(@Param("id") Long id, @Param("email") String email, @Param("newPin") String newPin);
}
