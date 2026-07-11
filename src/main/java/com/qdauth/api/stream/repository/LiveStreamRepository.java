package com.qdauth.api.stream.repository;

import com.qdauth.api.stream.model.LiveStream;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, String> {

  List<LiveStream> findByChannelId(String channelId);

  /**
   * Retrieves the stream for a given channel that has not yet been stopped, if any. A channel is
   * expected to have at most one such record at a time.
   */
  Optional<LiveStream> findByChannelIdAndStoppedAtIsNull(String channelId);

  /**
   * Marks the stream as started by setting started_at to the current timestamp. The WHERE clause
   * restricts the update to streams that have not already been started, making the operation
   * idempotent; the returned count is 0 if the stream was already started or does not exist.
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE LiveStream ls SET ls.startedAt = CURRENT_TIMESTAMP WHERE ls.id = :id AND ls.startedAt IS NULL")
  int startStream(@Param("id") String id);

  /**
   * Marks the stream as stopped by setting stopped_at to the current timestamp. The WHERE clause
   * restricts the update to streams that have already been started and not yet stopped.
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE LiveStream ls SET ls.stoppedAt = CURRENT_TIMESTAMP "
          + "WHERE ls.id = :id AND ls.startedAt IS NOT NULL AND ls.stoppedAt IS NULL")
  int stopStream(@Param("id") String id);
}
