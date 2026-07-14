package com.qdauth.api.channel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.entity.ChannelSubscription;
import com.qdauth.api.channel.repository.ChannelRepository;
import com.qdauth.api.channel.repository.ChannelSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ChannelSubscriptionServiceTest {

  @Mock private ChannelSubscriptionRepository channelSubscriptionRepository;
  @Mock private ChannelRepository channelRepository;
  @Mock private AccountRepository accountRepository;

  private ChannelSubscriptionService channelSubscriptionService;

  @BeforeEach
  void setUp() {
    channelSubscriptionService =
        new ChannelSubscriptionService(
            channelSubscriptionRepository, channelRepository, accountRepository);
  }

  @Test
  void subscribe_successfullyCreatesSubscription() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    Channel channel = new Channel();
    Account account = new Account();

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(channelSubscriptionRepository.existsByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(false);
    when(channelSubscriptionRepository.save(any(ChannelSubscription.class)))
        .thenAnswer(i -> i.getArgument(0));

    ChannelSubscription result = channelSubscriptionService.subscribe(channelId, accountId);

    assertThat(result).isNotNull();
    assertThat(result.getChannel()).isSameAs(channel);
    assertThat(result.getAccount()).isSameAs(account);

    verify(channelRepository).findById(channelId);
    verify(accountRepository).findById(accountId);
    verify(channelSubscriptionRepository).existsByChannelIdAndAccountId(channelId, accountId);
    verify(channelSubscriptionRepository).save(any(ChannelSubscription.class));
  }

  @Test
  void subscribe_throwsWhenChannelDoesNotExist() {
    final String channelId = "missingChannel";
    final String accountId = "accountId";

    when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> channelSubscriptionService.subscribe(channelId, accountId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Channel not found: " + channelId);

    verify(channelRepository).findById(channelId);
  }

  @Test
  void subscribe_throwsWhenAccountDoesNotExist() {
    final String channelId = "channelId";
    final String accountId = "missingAccount";

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(new Channel()));
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> channelSubscriptionService.subscribe(channelId, accountId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Account not found: " + accountId);

    verify(channelRepository).findById(channelId);
    verify(accountRepository).findById(accountId);
  }

  @Test
  void subscribe_throwsWhenAlreadySubscribed() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(new Channel()));
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account()));
    when(channelSubscriptionRepository.existsByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(true);

    assertThatThrownBy(() -> channelSubscriptionService.subscribe(channelId, accountId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already subscribed");

    verify(channelSubscriptionRepository).existsByChannelIdAndAccountId(channelId, accountId);
  }

  @Test
  void subscribe_throwsWhenDatabaseConstraintFails() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(new Channel()));
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account()));
    when(channelSubscriptionRepository.existsByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(false);

    when(channelSubscriptionRepository.save(any(ChannelSubscription.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(() -> channelSubscriptionService.subscribe(channelId, accountId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already subscribed");
  }

  @Test
  void getSubscription_returnsSubscriptionWhenPresent() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    ChannelSubscription subscription = new ChannelSubscription();

    when(channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(Optional.of(subscription));

    Optional<ChannelSubscription> result =
        channelSubscriptionService.getSubscription(channelId, accountId);

    assertThat(result).contains(subscription);

    verify(channelSubscriptionRepository).findByChannelIdAndAccountId(channelId, accountId);
  }

  @Test
  void getSubscription_returnsEmptyWhenNotSubscribed() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    when(channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(Optional.empty());

    Optional<ChannelSubscription> result =
        channelSubscriptionService.getSubscription(channelId, accountId);

    assertThat(result).isEmpty();

    verify(channelSubscriptionRepository).findByChannelIdAndAccountId(channelId, accountId);
  }

  @Test
  void banAccount_successfullyBansAccount() {
    final String channelId = "channelId";
    final String accountId = "accountId";
    final String reason = "Spam";

    when(channelSubscriptionRepository.banAccountFromChannel(channelId, accountId, reason))
        .thenReturn(1);

    channelSubscriptionService.banAccount(channelId, accountId, reason);

    verify(channelSubscriptionRepository).banAccountFromChannel(channelId, accountId, reason);
  }

  @Test
  void banAccount_throwsWhenReasonIsNull() {
    assertThatThrownBy(() -> channelSubscriptionService.banAccount("channelId", "accountId", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("A ban reason is required");
  }

  @Test
  void banAccount_throwsWhenReasonIsBlank() {
    assertThatThrownBy(() -> channelSubscriptionService.banAccount("channelId", "accountId", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("A ban reason is required");
  }

  @Test
  void banAccount_throwsWhenNoActiveSubscriptionExists() {
    final String channelId = "channelId";
    final String accountId = "accountId";

    when(channelSubscriptionRepository.banAccountFromChannel(channelId, accountId, "Spam"))
        .thenReturn(0);

    assertThatThrownBy(() -> channelSubscriptionService.banAccount(channelId, accountId, "Spam"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No active subscription found");

    verify(channelSubscriptionRepository).banAccountFromChannel(channelId, accountId, "Spam");
  }

  @Test
  void listSubscriptions_successfullyReturnsSubscriptions() {
    final String accountId = "accountId";

    final ChannelSubscription sub1 = new ChannelSubscription();
    final ChannelSubscription sub2 = new ChannelSubscription();

    when(channelSubscriptionRepository.findByAccountId(accountId)).thenReturn(List.of(sub1, sub2));

    List<ChannelSubscription> results =
        channelSubscriptionService.listSubscriptionsByAccountId(accountId);

    assertThat(results).hasSize(2).containsExactly(sub1, sub2);

    verify(channelSubscriptionRepository).findByAccountId(accountId);
  }
}
