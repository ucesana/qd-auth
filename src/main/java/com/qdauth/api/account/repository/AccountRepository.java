package com.qdauth.api.account.repository;

import com.qdauth.api.account.model.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, String> {

  Optional<Account> getByUserId(String userId);

  List<Account> findByUserId(String userId);

  @Modifying
  @Query("UPDATE Account a SET a.name = :name WHERE a.id = :id")
  void updateName(@Param("id") String id, @Param("name") String name);
}
