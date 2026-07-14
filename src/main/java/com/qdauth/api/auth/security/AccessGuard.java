package com.qdauth.api.auth.security;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.channel.entity.Channel;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

  public static final String ACCESS_DENIED = "Access denied.";

  public void requireAccountOwner(QdPrincipal principal, Account account) {
    if (!isAccountOwner(principal, account)) {
      throw new AccessDeniedException(ACCESS_DENIED);
    }
  }

  public void requireChannelOwner(QdPrincipal principal, Channel channel) {
    if (!isChannelOwner(principal, channel)) {
      throw new AccessDeniedException(ACCESS_DENIED);
    }
  }

  public boolean isAccountOwner(QdPrincipal principal, Account account) {
    return Objects.equals(principal.userId(), account.getUser().getId());
  }

  public boolean isChannelOwner(QdPrincipal principal, Channel channel) {
    return isAccountOwner(principal, channel.getAccount());
  }
}
