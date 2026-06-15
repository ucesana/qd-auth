package com.qdauth.resource.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamState streamState;

    public WebSocketConfig(StreamState streamState) {
        this.streamState = streamState;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new IngestHandler(streamState), "/api/streams/live/ingest")
                .setAllowedOrigins("*");
        registry.addHandler(new ConsumeHandler(streamState), "/api/streams/live/consume")
                .setAllowedOrigins("*")
                .addInterceptors(new AuthHandshakeInterceptor());
    }

    /**
     * Raise the binary message size limit to accommodate large media chunks.
     * The default (8192 bytes) is far too small for video keyframes.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024); // 10 MB
        container.setMaxTextMessageBufferSize(64 * 1024);
        return container;
    }
}
