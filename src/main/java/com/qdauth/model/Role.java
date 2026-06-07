// src/main/java/com/qdauth/model/Role.java
package com.qdauth.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

  @Id
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @Column(nullable = false, unique = true)
  private String name;

  @PrePersist
  protected void onCreate() {
    if (id == null) id = UUID.randomUUID().toString();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
