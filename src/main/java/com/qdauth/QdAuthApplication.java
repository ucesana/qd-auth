package com.qdauth;

import com.qdauth.properties.CorsProperties;
import com.qdauth.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
public class QdAuthApplication {

  public static void main(String[] args) {
    SpringApplication.run(QdAuthApplication.class, args);
  }
}
