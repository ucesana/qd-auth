package com.qdauth.api.channel.controller;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.service.AccountService;
import com.qdauth.api.auth.security.AccessGuard;
import com.qdauth.api.auth.security.QdPrincipal;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.entity.ChannelSubscription;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.channel.service.ChannelSubscriptionService;
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
@RequestMapping("/api/channels")
public class ChannelController {

  private final ChannelService channelService;
  private final ChannelSubscriptionService channelSubscriptionService;
  private final AccountService accountService;
  private final AccessGuard accessGuard;

  public ChannelController(
      ChannelService channelService,
      ChannelSubscriptionService channelSubscriptionService,
      AccountService accountService,
      AccessGuard accessGuard) {
    this.channelService = channelService;
    this.channelSubscriptionService = channelSubscriptionService;
    this.accountService = accountService;
    this.accessGuard = accessGuard;
  }

  /** Create a channel for any account owned by the user. */
  @PostMapping
  public ResponseEntity<ChannelResponse> createChannel(
      @RequestBody CreateChannelRequest request, @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccount(request.accountId());
    accessGuard.requireAccountOwner(principal, account);
    Channel channel =
        channelService.createChannel(request.accountId(), request.name(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(ChannelResponse.from(channel));
  }

  /** Get any channel. */
  @GetMapping("/{id}")
  public ChannelResponse getChannel(@PathVariable String id) {
    return ChannelResponse.from(channelService.getChannel(id));
  }

  /** List the channels of any account. */
  @GetMapping
  public List<ChannelResponse> listChannels(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String accountId) {
    return channelService
        .listChannelsFilterBy(
            ChannelFilter.builder().withName(name).withAccountId(accountId).build())
        .stream()
        .map(ChannelResponse::from)
        .toList();
  }

  /** Account owner subscribes to any channel. */
  @PostMapping("/subscriptions")
  public ResponseEntity<ChannelSubscriptionResponse> subscribe(
      @RequestBody SubscribeRequest request, @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccountForUser(principal.userId());
    accessGuard.requireAccountOwner(principal, account);
    ChannelSubscription subscription =
        channelSubscriptionService.subscribe(request.channelId(), account.getId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ChannelSubscriptionResponse.from(subscription));
  }

  /** List all the channels your account has subscribed to. */
  @GetMapping("/subscriptions")
  public List<ChannelSubscriptionResponse> listSubscriptions(
      @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccountForUser(principal.userId());
    accessGuard.requireAccountOwner(principal, account);
    return channelSubscriptionService.listSubscriptionsByAccountId(account.getId()).stream()
        .map(ChannelSubscriptionResponse::from)
        .toList();
  }

  /** List the subscriptions to any channel. */
  @GetMapping("/{id}/subscriptions")
  public List<ChannelSubscriptionResponse> listChannelSubscribers(@PathVariable String id) {
    return channelSubscriptionService.listSubscriptionsByChannelId(id).stream()
        .map(ChannelSubscriptionResponse::from)
        .toList();
  }

  /** Ban subscription to channel owned by user. */
  @PostMapping("/{id}/subscriptions/ban")
  public ResponseEntity<Void> banAccount(
      @PathVariable String id,
      @RequestBody BanRequest request,
      @AuthenticationPrincipal QdPrincipal principal) {
    Channel channel = channelService.getChannel(id);
    accessGuard.requireChannelOwner(principal, channel);
    channelSubscriptionService.banAccount(id, request.accountId(), request.reason());
    return ResponseEntity.noContent().build();
  }

  public record CreateChannelRequest(String accountId, String name, String description) {}

  public record ChannelResponse(String id, String name, String description, String accountId) {
    public static ChannelResponse from(Channel channel) {
      return new ChannelResponse(
          channel.getId(),
          channel.getName(),
          channel.getDescription(),
          channel.getAccount().getId());
    }
  }

  public record SubscribeRequest(String channelId) {}

  public record BanRequest(String accountId, String reason) {}

  public record ChannelSubscriptionResponse(
      String id,
      String channelId,
      String accountId,
      boolean banned,
      String banReason,
      LocalDateTime bannedAt) {
    public static ChannelSubscriptionResponse from(ChannelSubscription subscription) {
      return new ChannelSubscriptionResponse(
          subscription.getId(),
          subscription.getChannel().getId(),
          subscription.getAccount().getId(),
          subscription.isBanned(),
          subscription.getBanReason(),
          subscription.getBannedAt());
    }
  }
}
