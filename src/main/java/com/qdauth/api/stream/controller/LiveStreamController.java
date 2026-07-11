package com.qdauth.api.stream.controller;

import com.qdauth.api.account.model.Account;
import com.qdauth.api.account.service.AccountService;
import com.qdauth.api.auth.security.AccessGuard;
import com.qdauth.api.auth.security.QdPrincipal;
import com.qdauth.api.channel.model.Channel;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.stream.model.LiveStream;
import com.qdauth.api.stream.model.LiveStreamChat;
import com.qdauth.api.stream.service.LiveStreamChatService;
import com.qdauth.api.stream.service.LiveStreamService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livestreams")
public class LiveStreamController {

  private final LiveStreamService liveStreamService;
  private final LiveStreamChatService liveStreamChatService;
  private final ChannelService channelService;
  private final AccountService accountService;
  private final AccessGuard accessGuard;

  public LiveStreamController(
      LiveStreamService liveStreamService,
      LiveStreamChatService liveStreamChatService,
      ChannelService channelService,
      AccountService accountService,
      AccessGuard accessGuard) {
    this.liveStreamService = liveStreamService;
    this.liveStreamChatService = liveStreamChatService;
    this.channelService = channelService;
    this.accountService = accountService;
    this.accessGuard = accessGuard;
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
  public List<LiveStreamResponse> listStreams(@RequestParam String channelId) {
    return liveStreamService.listStreamsForChannel(channelId).stream()
        .map(LiveStreamResponse::from)
        .toList();
  }

  /** Channel owner starts a stream. */
  @PostMapping("/{id}/start")
  public ResponseEntity<Void> startStream(
      @PathVariable String id, @AuthenticationPrincipal QdPrincipal principal) {
    LiveStream stream = liveStreamService.getStream(id);
    accessGuard.requireChannelOwner(principal, stream.getChannel());
    liveStreamService.startStream(id);
    return ResponseEntity.noContent().build();
  }

  /** Channel owner stops a stream. */
  @PostMapping("/{id}/stop")
  public ResponseEntity<Void> stopStream(
      @PathVariable String id, @AuthenticationPrincipal QdPrincipal principal) {
    LiveStream stream = liveStreamService.getStream(id);
    accessGuard.requireChannelOwner(principal, stream.getChannel());
    liveStreamService.stopStream(id);
    return ResponseEntity.noContent().build();
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
      String channelId,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime stoppedAt) {
    public static LiveStreamResponse from(LiveStream stream) {
      return new LiveStreamResponse(
          stream.getId(),
          stream.getName(),
          stream.getDescription(),
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
