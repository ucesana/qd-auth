package com.qdauth.api.channel.service;

import com.qdauth.api.channel.controller.ChannelFilter;
import com.qdauth.api.channel.entity.Channel;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ChannelSpecifications {

  private ChannelSpecifications() {}

  public static Specification<Channel> withFilter(ChannelFilter filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.hasText(filter.getAccountId())) {
        predicates.add(cb.equal(root.get("account").get("id"), filter.getAccountId()));
      }

      if (StringUtils.hasText(filter.getName())) {
        predicates.add(
            cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
