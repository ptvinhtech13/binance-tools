package com.ptvinh.binance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ptvinh.binance.domain.CoinPropertiesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;

@Configuration
public class DynamicEnvConfig {

  private final String coinPath;
  private final ObjectMapper mapper;
  private CoinPropertiesConfig coinPropertiesConfig;

  @Autowired
  public DynamicEnvConfig(@Value("${crawing.config.init-crawling-date:./coin-config/coin-config.yaml}") String coinPath) throws IOException {
    this.mapper = new ObjectMapper(new YAMLFactory());
    this.mapper.findAndRegisterModules();
    this.coinPath = coinPath;
    fetchCoinPropertiesConfig();
  }

  public synchronized CoinPropertiesConfig getCoinPropertiesConfig() {
    if (coinPropertiesConfig != null) {
      return coinPropertiesConfig;
    }
    try {
      return fetchCoinPropertiesConfig();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getCause());
    }
  }

  @Scheduled(fixedDelay = 5000)
  public CoinPropertiesConfig fetchCoinPropertiesConfig() throws IOException {
    this.coinPropertiesConfig = mapper.readValue(new File(coinPath), CoinPropertiesConfig.class);
    return this.coinPropertiesConfig;
  }
}
