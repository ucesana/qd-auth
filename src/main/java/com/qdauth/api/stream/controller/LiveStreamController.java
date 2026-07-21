package com.qdauth.api.stream.controller;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.service.AccountService;
import com.qdauth.api.auth.security.AccessGuard;
import com.qdauth.api.auth.security.QdPrincipal;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.stream.StreamRegistry;
import com.qdauth.api.stream.entity.LiveStream;
import com.qdauth.api.stream.entity.LiveStreamChat;
import com.qdauth.api.stream.service.LiveStreamChatService;
import com.qdauth.api.stream.service.LiveStreamService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/livestreams")
public class LiveStreamController {

  private static final Logger log = LoggerFactory.getLogger(LiveStreamController.class);

  private final LiveStreamService liveStreamService;
  private final LiveStreamChatService liveStreamChatService;
  private final ChannelService channelService;
  private final AccountService accountService;
  private final AccessGuard accessGuard;
  private final StreamRegistry streamRegistry;

  public LiveStreamController(
      LiveStreamService liveStreamService,
      LiveStreamChatService liveStreamChatService,
      ChannelService channelService,
      AccountService accountService,
      AccessGuard accessGuard,
      StreamRegistry streamRegistry) {
    this.liveStreamService = liveStreamService;
    this.liveStreamChatService = liveStreamChatService;
    this.channelService = channelService;
    this.accountService = accountService;
    this.accessGuard = accessGuard;
    this.streamRegistry = streamRegistry;
  }

  /** Channel owner creates a stream. */
  @PostMapping
  public ResponseEntity<LiveStreamResponse> createStream(
      @RequestBody CreateLiveStreamRequest request,
      @AuthenticationPrincipal QdPrincipal principal) {
    Channel channel = channelService.getChannel(request.channelId());
    accessGuard.requireChannelOwner(principal, channel);
    LiveStream stream =
        liveStreamService.createStream(request.channelId(), request.name(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(LiveStreamResponse.from(stream));
  }

  /** Any user gets a stream. */
  @GetMapping("/{id}")
  public LiveStreamResponse getStream(@PathVariable String id) {
    return LiveStreamResponse.from(liveStreamService.getStream(id));
  }

  /** Any user lists streams of any channel. */
  @GetMapping
  public List<LiveStreamResponse> listStreamsForChannel(@RequestParam String channelId) {
    return liveStreamService.listStreamsForChannel(channelId).stream()
        .map(LiveStreamResponse::from)
        .toList();
  }

  /** Any user lists streams of any channel. */
  @GetMapping("/browse")
  public List<LiveStreamResponse> browseStreams() {
    return liveStreamService.browseStreams().stream().map(LiveStreamResponse::from).toList();
  }

  /** Channel owner starts a stream. */
  @PostMapping("/{id}/start")
  public ResponseEntity<Void> startStream(
      @PathVariable String id, @AuthenticationPrincipal QdPrincipal principal) {
    LiveStream stream = liveStreamService.getStream(id);
    accessGuard.requireChannelOwner(principal, stream.getChannel());
    liveStreamService.startStream(id);
    streamRegistry.getOrCreate(id);
    log.info("Start stream {}", id);
    return ResponseEntity.noContent().build();
  }

  /** Channel owner stops a stream. */
  @PostMapping("/{id}/stop")
  public ResponseEntity<Void> stopStream(
      @PathVariable String id, @AuthenticationPrincipal QdPrincipal principal) {
    LiveStream stream = liveStreamService.getStream(id);
    accessGuard.requireChannelOwner(principal, stream.getChannel());
    liveStreamService.stopStream(id);
    streamRegistry.remove(id);
    log.info("Stopped stream {}", id);
    return ResponseEntity.noContent().build();
  }

  /** Update live stream thumbnail */
  @PutMapping(
      value = "/{id}/thumbnail",
      consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
  public ResponseEntity<Void> updateThumbnail(
      @PathVariable String id,
      @RequestBody byte[] image,
      @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
      @AuthenticationPrincipal QdPrincipal principal) {

    LiveStream stream = liveStreamService.getStream(id);

    accessGuard.requireChannelOwner(principal, stream.getChannel());

    liveStreamService.updateThumbnail(id, image, contentType);

    return ResponseEntity.noContent().build();
  }

  /** Get the live stream thumbnail */
  @GetMapping("/{id}/thumbnail")
  public ResponseEntity<byte[]> getThumbnail(@PathVariable String id) {

    LiveStream stream = liveStreamService.getStream(id);

    if (stream.getThumbnail() == null) {
      return ResponseEntity.notFound().build();
    }

    MediaType contentType = MediaType.parseMediaType(stream.getThumbnailContentType());

    return ResponseEntity.ok()
        .contentType(contentType)
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)))
        .body(stream.getThumbnail());
  }

  /**
   * Any user posts a chat to a stream. If the user's subscription has been banned from the channel
   * then `IllegalStateException` exception is thrown.
   */
  @PostMapping("/chats")
  public ResponseEntity<LiveStreamChatResponse> postChat(
      @RequestBody PostChatRequest request, @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccountForUser(principal);
    accessGuard.requireAccountOwner(principal, account);
    LiveStreamChat chat =
        liveStreamChatService.postMessage(
            request.streamId(), request.accountId(), request.message());
    return ResponseEntity.status(HttpStatus.CREATED).body(LiveStreamChatResponse.from(chat));
  }

  /**
   * Open to any authenticated user, treated the same as viewing a channel or stream. If chat
   * history should instead be restricted to accounts subscribed to the stream's channel, this
   * method would need to additionally fetch the caller's own ChannelSubscription for that channel
   * and reject the request if none exists or if it is banned; that stricter reading was not
   * explicit in the requirements, so it has not been applied here.
   */
  @GetMapping("/chats")
  public List<LiveStreamChatResponse> listChats(@RequestParam String streamId) {
    return liveStreamChatService.listMessagesForStream(streamId).stream()
        .map(LiveStreamChatResponse::from)
        .toList();
  }

  public record CreateLiveStreamRequest(String channelId, String name, String description) {}

  /** Only the owning channel's id is included, not a nested channel representation. */
  public record LiveStreamResponse(
      String id,
      String name,
      String description,
      String thumbnailContentType,
      String channelId,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime stoppedAt) {
    public static LiveStreamResponse from(LiveStream stream) {
      return new LiveStreamResponse(
          stream.getId(),
          stream.getName(),
          stream.getDescription(),
          stream.getThumbnailContentType(),
          stream.getChannel().getId(),
          stream.getCreatedAt(),
          stream.getStartedAt(),
          stream.getStoppedAt());
    }
  }

  public record PostChatRequest(String streamId, String accountId, String message) {}

  /**
   * channelSubscriptionId is exposed rather than accountId directly, since the chat message is
   * associated with a ChannelSubscription, not an Account, at the persistence layer.
   */
  public record LiveStreamChatResponse(
      String id, String streamId, String channelSubscriptionId, String message) {
    public static LiveStreamChatResponse from(LiveStreamChat chat) {
      return new LiveStreamChatResponse(
          chat.getId(),
          chat.getStream().getId(),
          chat.getChannelSubscription().getId(),
          chat.getMessage());
    }
  }
}
