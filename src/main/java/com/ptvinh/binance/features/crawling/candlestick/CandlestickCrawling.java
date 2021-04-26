package com.ptvinh.binance.features.crawling.candlestick;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ptvinh.binance.config.DynamicEnvConfig;
import com.ptvinh.binance.domain.CoinPropertiesConfig;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.client.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class CandlestickCrawling implements SchedulingConfigurer {

  @Value("${crawing.config.init-crawling-date:2021-04-01T11:15:00.000Z}")
  private String initCrawlingDate;

  private final DynamicEnvConfig dynamicEnvConfig;

  private final Map<String, Lock> lockMap;

  private final CandlestickRepository candlestickRepository;
  private final BinanceClient binanceClient;

  @Autowired
  public CandlestickCrawling(BinanceClient client,
      CandlestickRepository candlestickRepository,
      DynamicEnvConfig dynamicEnvConfig) {
    this.candlestickRepository = candlestickRepository;
    this.binanceClient = client;
    this.dynamicEnvConfig = dynamicEnvConfig;
    this.lockMap = dynamicEnvConfig.getCoinPropertiesConfig().getSymbols().stream()
        .collect(Collectors.toMap(CoinPropertiesConfig.CoinProperties::getSymbol, symbol -> new ReentrantLock()));
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
    dynamicEnvConfig.getCoinPropertiesConfig().getSymbols().stream().filter(CoinPropertiesConfig.CoinProperties::isSyncUp)
        .map(CoinPropertiesConfig.CoinProperties::getSymbol).forEach(
            symbol -> {
              final Runnable runnable = () -> {
                lockMap.get(symbol).lock();
                fetchRawData(symbol);
                lockMap.get(symbol).unlock();
              };

              final Trigger trigger = triggerContext -> {
                CronTrigger crontrigger = new CronTrigger("*/2 * * * * *");
                return crontrigger.nextExecutionTime(triggerContext);
              };
              scheduledTaskRegistrar.addTriggerTask(runnable, trigger);
            });
  }

  private void fetchRawData(String symbol) {
    final Instant latest = candlestickRepository.findOneByMaxCloseTime(symbol)
        .map(CandlestickDataEntity::getCloseTime)
        .orElse(Instant.parse(initCrawlingDate));

    final Instant now = Instant.now();
    // log.info(String.format("%s: FetchRawData: start from %s to %s", symbol, latest, now));
    //    log.info(String.format("%s : FetchRawData: %s - %s", symbol, latest.toEpochMilli(), latest.toString()));

    if (latest.isBefore(now)) {
      final List<CandlestickDataEntity> entities = binanceClient.getCandlesickData(symbol, latest.toEpochMilli()).stream()
          .map(node -> {
            final Iterator<JsonNode> iterator = node.elements();
            final List<Object> result = new ArrayList<>();
            while (iterator.hasNext()) {
              result.add(iterator.next());
            }
            return CandlestickDataEntity.builder()
                .openTime(Instant.ofEpochMilli(((LongNode) result.get(0)).longValue()))
                .openPrice(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(1)).asText())))
                .highPrice(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(2)).asText())))
                .lowPrice(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(3)).asText())))
                .closePrice(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(4)).asText())))
                .volume(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(5)).asText())))
                .closeTime(Instant.ofEpochMilli(((LongNode) result.get(6)).longValue()))
                .quoteAssetVolume(BigDecimal.valueOf(Double.parseDouble(((TextNode) result.get(7)).asText())))
                .symbol(symbol)
                .build();
          })
          .collect(Collectors.toList());
      candlestickRepository.saveAll(entities);
    } else {
      //      log.info(String.format("%s: FetchRawData is latest", symbol));
    }

  }
}
