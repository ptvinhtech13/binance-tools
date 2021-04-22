package com.ptvinh.binance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Configuration
@EnableJpaRepositories("com.ptvinh.binance")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableFeignClients
public class BinanceToolsApplication {

  public static void main(String[] args) {
    SpringApplication.run(BinanceToolsApplication.class, args);
  }

}
