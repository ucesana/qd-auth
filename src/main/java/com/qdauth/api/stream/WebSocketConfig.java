package com.qdauth.api.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final StreamRegistry registry;

  public WebSocketConfig(StreamRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    registry
        .addHandler(new IngestHandler(this.registry), "/api/livestreams/{id}/ingest")
        .setAllowedOrigins("*")
        .addInterceptors(new AuthHandshakeInterceptor())
        .addInterceptors(new StreamIdHandshakeInterceptor());

    registry
        .addHandler(new ConsumeHandler(this.registry), "/api/livestreams/{id}/consume")
        .setAllowedOrigins("*")
        .addInterceptors(new AuthHandshakeInterceptor())
        .addInterceptors(new StreamIdHandshakeInterceptor());
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

    container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
    container.setMaxTextMessageBufferSize(64 * 1024);

    return container;
  }
}
