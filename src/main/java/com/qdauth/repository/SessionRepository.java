package com.qdauth.repository;

import com.qdauth.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, String> {

    List<Session> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") String familyId);
}
