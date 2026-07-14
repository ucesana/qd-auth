package com.qdauth.api.auth.repository;

import com.qdauth.api.auth.entity.Session;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, String> {

  List<Session> findByUserId(String userId);

  @Modifying
  @Query("DELETE FROM Session s WHERE s.familyId = :familyId")
  void deleteByFamilyId(@Param("familyId") String familyId);

  List<Session> findByUserIdAndDeviceId(String userId, String deviceId);

  @Modifying
  @Query(
      """
    DELETE FROM Session s WHERE NOT EXISTS (
        SELECT 1 FROM RefreshToken t
        WHERE t.familyId = s.familyId
        AND t.revoked = false
        AND t.consumed = false
        AND t.expiresAt > :now
    )
    """)
  void deleteOrphaned(@Param("now") Instant now);
}
