package com.porterlike.services.auth.repository;

import com.porterlike.services.auth.model.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
