package com.qdauth.repository;

import com.qdauth.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

  Optional<RefreshToken> findById(String id);

  @Modifying
  @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.familyId = :familyId")
  void revokeFamily(@Param("familyId") String familyId);

  @Modifying
  @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId")
  void revokeAllForUser(@Param("userId") String userId);
}
