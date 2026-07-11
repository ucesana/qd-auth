package com.qdauth.api.stream.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qdauth.BaseControllerTest;
import com.qdauth.TestUserAccount;
import com.qdauth.api.channel.model.Channel;
import com.qdauth.api.channel.model.ChannelSubscription;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.channel.service.ChannelSubscriptionService;
import com.qdauth.api.stream.model.LiveStream;
import com.qdauth.api.stream.service.LiveStreamChatService;
import com.qdauth.api.stream.service.LiveStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class LiveStreamControllerTest extends BaseControllerTest {

  @Autowired ChannelService channelService;
  @Autowired LiveStreamService liveStreamService;
  @Autowired LiveStreamChatService liveStreamChatService;
  @Autowired ChannelSubscriptionService channelSubscriptionService;

  @Test
  void createStream_returnsCreatedStream() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    mockMvc
        .perform(
            post("/api/livestreams")
                .cookie(owner.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "channelId":"%s",
                      "name":"Weekly Stream",
                      "description":"Live coding"
                    }
                    """
                        .formatted(channel.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.channelId").value(channel.getId()))
        .andExpect(jsonPath("$.name").value("Weekly Stream"))
        .andExpect(jsonPath("$.description").value("Live coding"));
  }

  @Test
  void createStream_returns403ForNonOwner() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount other = createUserAndAccountAndLogin("other@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    mockMvc
        .perform(
            post("/api/livestreams")
                .cookie(other.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "channelId":"%s",
                      "name":"Hack",
                      "description":"Should fail"
                    }
                    """
                        .formatted(channel.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void getStream_returnsStream() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    mockMvc
        .perform(get("/api/livestreams/{id}", stream.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(stream.getId()))
        .andExpect(jsonPath("$.channelId").value(channel.getId()))
        .andExpect(jsonPath("$.name").value("Weekly Stream"))
        .andExpect(jsonPath("$.description").value("Live coding"));
  }

  @Test
  void getStream_returns404ForUnknownStream() throws Exception {
    mockMvc.perform(get("/api/livestreams/does-not-exist")).andExpect(status().isNotFound());
  }

  @Test
  void listStreams_returnsStreamsForChannel() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    liveStreamService.createStream(channel.getId(), "One", "Desc");
    liveStreamService.createStream(channel.getId(), "Two", "Desc");

    mockMvc
        .perform(get("/api/livestreams").param("channelId", channel.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void startStream_marksStreamStarted() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    mockMvc
        .perform(post("/api/livestreams/{id}/start", stream.getId()).cookie(owner.cookie()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/livestreams/{id}", stream.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startedAt").isNotEmpty());
  }

  @Test
  void startStream_returns403ForNonOwner() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount other = createUserAndAccountAndLogin("other@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    mockMvc
        .perform(post("/api/livestreams/{id}/start", stream.getId()).cookie(other.cookie()))
        .andExpect(status().isForbidden());
  }

  @Test
  void stopStream_marksStreamStopped() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    liveStreamService.startStream(stream.getId());

    mockMvc
        .perform(post("/api/livestreams/{id}/stop", stream.getId()).cookie(owner.cookie()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/livestreams/{id}", stream.getId()).cookie(owner.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stoppedAt").isNotEmpty());
  }

  @Test
  void postChat_returnsCreatedChat() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount viewer = createUserAndAccountAndLogin("viewer@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    ChannelSubscription subscription =
        channelSubscriptionService.subscribe(channel.getId(), viewer.account().getId());

    mockMvc
        .perform(
            post("/api/livestreams/chats")
                .cookie(viewer.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "streamId":"%s",
                      "accountId":"%s",
                      "message":"Hello world"
                    }
                    """
                        .formatted(stream.getId(), viewer.account().getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.streamId").value(stream.getId()))
        .andExpect(jsonPath("$.channelSubscriptionId").value(subscription.getId()))
        .andExpect(jsonPath("$.message").value("Hello world"));
  }

  @Test
  void listChats_returnsChatsForStream() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount viewer = createUserAndAccountAndLogin("viewer@example.com");

    Channel channel =
        channelService.createChannel(owner.account().getId(), "General", "General discussion");

    LiveStream stream =
        liveStreamService.createStream(channel.getId(), "Weekly Stream", "Live coding");

    channelSubscriptionService.subscribe(channel.getId(), viewer.account().getId());

    liveStreamChatService.postMessage(stream.getId(), viewer.account().getId(), "Hello world");

    mockMvc
        .perform(get("/api/livestreams/chats").param("streamId", stream.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].streamId").value(stream.getId()))
        .andExpect(jsonPath("$[0].message").value("Hello world"));
  }
}
