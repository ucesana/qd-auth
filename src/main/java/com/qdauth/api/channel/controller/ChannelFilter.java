package com.qdauth.api.channel.controller;

public class ChannelFilter {
  private String name;
  private String accountId;

  private ChannelFilter() {}

  public static ChannelFilterBuilder builder() {
    return new ChannelFilterBuilder();
  }

  public String getName() {
    return name;
  }

  public String getAccountId() {
    return accountId;
  }

  public static final class ChannelFilterBuilder {
    private String name;
    private String accountId;

    private ChannelFilterBuilder() {}

    public ChannelFilterBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ChannelFilterBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public ChannelFilter build() {
      ChannelFilter channelFilter = new ChannelFilter();
      channelFilter.name = this.name;
      channelFilter.accountId = this.accountId;
      return channelFilter;
    }
  }
}
