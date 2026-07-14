package com.qdauth.api.channel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.channel.controller.ChannelFilter;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.repository.ChannelRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

  @Mock private ChannelRepository channelRepository;
  @Mock private AccountRepository accountRepository;

  private ChannelService channelService;

  @BeforeEach
  void setUp() {
    channelService = new ChannelService(channelRepository, accountRepository);
  }

  @Test
  void create_successfullyCreatesChannel() {
    final String accountId = "accountId";
    final String name = "General";
    final String description = "General discussion";

    Account account = mock(Account.class);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

    Channel channel = channelService.createChannel(accountId, name, description);

    assertThat(channel).isNotNull();
    assertThat(channel.getAccount()).isSameAs(account);
    assertThat(channel.getName()).isEqualTo(name);
    assertThat(channel.getDescription()).isEqualTo(description);

    verify(accountRepository).findById(accountId);
    verify(channelRepository).save(any(Channel.class));
  }

  @Test
  void create_successfullyCreatesChannelWithNullDescription() {
    final String accountId = "accountId";
    final String name = "General";

    Account account = mock(Account.class);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

    Channel channel = channelService.createChannel(accountId, name, null);

    assertThat(channel).isNotNull();
    assertThat(channel.getAccount()).isSameAs(account);
    assertThat(channel.getName()).isEqualTo(name);
    assertThat(channel.getDescription()).isEmpty();

    verify(accountRepository).findById(accountId);
    verify(channelRepository).save(any(Channel.class));
  }

  @Test
  void create_throwsEntityNotFoundException() {
    final String accountId = "missingAccount";

    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> channelService.createChannel(accountId, "General", "Description"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Account not found: " + accountId);

    verify(accountRepository).findById(accountId);
  }

  @Test
  void getChannel_successfullyReturnsChannel() {
    final String channelId = "channelId";

    Channel channel = new Channel();
    channel.setName("General");
    channel.setDescription("General discussion");

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

    Channel result = channelService.getChannel(channelId);

    assertThat(result).isSameAs(channel);
    assertThat(result.getName()).isEqualTo("General");
    assertThat(result.getDescription()).isEqualTo("General discussion");

    verify(channelRepository).findById(channelId);
  }

  @Test
  void getChannel_throwsEntityNotFoundException() {
    final String channelId = "missingChannel";

    when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> channelService.getChannel(channelId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Channel not found: " + channelId);

    verify(channelRepository).findById(channelId);
  }

  @Test
  void listChannelsForAccount_returnsChannels() {
    final String accountId = "accountId";

    Channel channel1 = new Channel();
    channel1.setName("General");

    Channel channel2 = new Channel();
    channel2.setName("Random");

    when(channelRepository.findByAccountId(accountId)).thenReturn(List.of(channel1, channel2));

    List<Channel> channels = channelService.listChannelsForAccount(accountId);

    assertThat(channels).hasSize(2).containsExactly(channel1, channel2);

    verify(channelRepository).findByAccountId(accountId);
  }

  @Test
  void listChannelsByFilter_returnsChannels() {
    Channel channel1 = new Channel();
    channel1.setName("General");

    ChannelFilter filter = ChannelFilter.builder().withName("Gen").build();

    when(channelRepository.findAll(ArgumentMatchers.<Specification<Channel>>any()))
        .thenReturn(List.of(channel1));

    List<Channel> channels = channelService.listChannelsFilterBy(filter);

    assertThat(channels).hasSize(1).containsExactly(channel1);

    verify(channelRepository).findAll(ArgumentMatchers.<Specification<Channel>>any());
  }
}
