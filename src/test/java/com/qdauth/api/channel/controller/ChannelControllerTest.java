package com.qdauth.api.channel.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qdauth.BaseControllerTest;
import com.qdauth.TestUserAccount;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.channel.service.ChannelSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ChannelControllerTest extends BaseControllerTest {

  @Autowired ChannelService channelService;

  @Autowired ChannelSubscriptionService channelSubscriptionService;

  @Test
  void createChannel_returnsCreatedChannel() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("owner@example.com");

    mockMvc
        .perform(
            post("/api/channels")
                .cookie(user.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "accountId":"%s",
                      "name":"General",
                      "description":"General discussion"
                    }
                    """
                        .formatted(user.account().getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").value(user.account().getId()))
        .andExpect(jsonPath("$.name").value("General"))
        .andExpect(jsonPath("$.description").value("General discussion"))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void createChannel_returns403ForAnotherUsersAccount() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount other = createUserAndAccountAndLogin("other@example.com");

    mockMvc
        .perform(
            post("/api/channels")
                .cookie(other.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "accountId":"%s",
                      "name":"Hacked",
                      "description":"Should fail"
                    }
                    """
                        .formatted(owner.account().getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void getChannel_returnsChannel() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(user.account().getId(), "General", "General discussion");

    mockMvc
        .perform(get("/api/channels/{id}", channel.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(channel.getId()))
        .andExpect(jsonPath("$.accountId").value(user.account().getId()))
        .andExpect(jsonPath("$.name").value("General"))
        .andExpect(jsonPath("$.description").value("General discussion"));
  }

  @Test
  void getChannel_returns404ForUnknownChannel() throws Exception {
    mockMvc.perform(get("/api/channels/does-not-exist")).andExpect(status().isNotFound());
  }

  @Test
  void listChannels_returnsAllChannels() throws Exception {
    TestUserAccount user1 = createUserAndAccountAndLogin("user1@example.com");
    TestUserAccount user2 = createUserAndAccountAndLogin("user2@example.com");

    channelService.createChannel(user1.account().getId(), "One", "Desc1");
    channelService.createChannel(user2.account().getId(), "Two", "Desc2");

    mockMvc
        .perform(get("/api/channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void listChannelsByAccount_returnsOnlyThatAccountsChannels() throws Exception {
    TestUserAccount user1 = createUserAndAccountAndLogin("user1@example.com");
    TestUserAccount user2 = createUserAndAccountAndLogin("user2@example.com");

    Channel channel = channelService.createChannel(user1.account().getId(), "General", "Desc");

    channelService.createChannel(user2.account().getId(), "Other", "Desc");

    mockMvc
        .perform(
            get("/api/channels").param("accountId", user1.account().getId()).cookie(user1.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(channel.getId()))
        .andExpect(jsonPath("$[0].accountId").value(user1.account().getId()));
  }

  @Test
  void listChannelsByFilter_returnsChannelsLikeNameCaseInsensitive() throws Exception {
    TestUserAccount user1 = createUserAndAccountAndLogin("user1@example.com");

    channelService.createChannel(user1.account().getId(), "General", "General topics");
    channelService.createChannel(user1.account().getId(), "Random", "Random stuff");
    channelService.createChannel(user1.account().getId(), "Gencon", "Boardgames convention!");
    channelService.createChannel(user1.account().getId(), "Oxygen", "Chemistry");
    channelService.createChannel(
        user1.account().getId(), "The Next Generation", "The Best Generation");

    mockMvc
        .perform(get("/api/channels").param("name", "gen").cookie(user1.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].name").value(containsStringIgnoringCase("gen")))
        .andExpect(jsonPath("$[1].name").value(containsStringIgnoringCase("gen")))
        .andExpect(jsonPath("$[2].name").value(containsStringIgnoringCase("gen")))
        .andExpect(jsonPath("$[3].name").value(containsStringIgnoringCase("gen")));
  }

  @Test
  void subscribe_createsSubscription() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount subscriber = createUserAndAccountAndLogin("subscriber@example.com");

    Channel channel = channelService.createChannel(owner.account().getId(), "General", "Desc");

    mockMvc
        .perform(
            post("/api/channels/subscriptions")
                .cookie(subscriber.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "channelId":"%s"
                    }
                    """
                        .formatted(channel.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.channelId").value(channel.getId()))
        .andExpect(jsonPath("$.accountId").value(subscriber.account().getId()))
        .andExpect(jsonPath("$.banned").value(false))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void listSubscriptions_returnsAccountsSubscriptions() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount subscriber = createUserAndAccountAndLogin("subscriber@example.com");

    Channel channel = channelService.createChannel(owner.account().getId(), "General", "Desc");

    channelSubscriptionService.subscribe(channel.getId(), subscriber.account().getId());

    mockMvc
        .perform(get("/api/channels/subscriptions").cookie(subscriber.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].channelId").value(channel.getId()))
        .andExpect(jsonPath("$[0].accountId").value(subscriber.account().getId()));
  }

  @Test
  void listChannelSubscribers_returnsSubscribers() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount subscriber = createUserAndAccountAndLogin("subscriber@example.com");

    Channel channel = channelService.createChannel(owner.account().getId(), "General", "Desc");

    channelSubscriptionService.subscribe(channel.getId(), subscriber.account().getId());

    mockMvc
        .perform(
            get("/api/channels/{id}/subscriptions", channel.getId()).cookie(subscriber.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].channelId").value(channel.getId()))
        .andExpect(jsonPath("$[0].accountId").value(subscriber.account().getId()));
  }

  @Test
  void banAccount_marksSubscriptionAsBanned() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount subscriber = createUserAndAccountAndLogin("subscriber@example.com");

    Channel channel = channelService.createChannel(owner.account().getId(), "General", "Desc");

    channelSubscriptionService.subscribe(channel.getId(), subscriber.account().getId());

    mockMvc
        .perform(
            post("/api/channels/{id}/subscriptions/ban", channel.getId())
                .cookie(owner.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "accountId":"%s",
                      "reason":"Spam"
                    }
                    """
                        .formatted(subscriber.account().getId())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/channels/{id}/subscriptions", channel.getId()).cookie(owner.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].banned").value(true))
        .andExpect(jsonPath("$[0].banReason").value("Spam"));
  }

  @Test
  void banAccount_returns403ForNonOwner() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount subscriber = createUserAndAccountAndLogin("subscriber@example.com");

    Channel channel = channelService.createChannel(owner.account().getId(), "General", "Desc");

    channelSubscriptionService.subscribe(channel.getId(), subscriber.account().getId());

    mockMvc
        .perform(
            post("/api/channels/{id}/subscriptions/ban", channel.getId())
                .cookie(subscriber.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "accountId":"%s",
                      "reason":"Spam"
                    }
                    """
                        .formatted(subscriber.account().getId())))
        .andExpect(status().isForbidden());
  }
}
