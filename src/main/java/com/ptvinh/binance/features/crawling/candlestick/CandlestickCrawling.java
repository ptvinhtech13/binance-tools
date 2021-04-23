package com.ptvinh.binance.features.crawling.candlestick;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ptvinh.binance.domain.CoinPair;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.client.BinanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CandlestickCrawling {

  private final CandlestickRepository candlestickRepository;

  private final BinanceClient binanceClient;

  @Autowired
  public CandlestickCrawling(BinanceClient client, CandlestickRepository candlestickRepository) {
    this.candlestickRepository = candlestickRepository;
    this.binanceClient = client;
  }


  @Async
  @PostConstruct
  private void crawling() {
    final CoinPair symbol = CoinPair.DOGEUSDT;

    Instant from = Instant.parse("2021-01-01T00:00:00.000Z");
    final Instant now = Instant.parse("2021-01-05T00:00:00.000Z");

    while (from.isBefore(now)) {
      System.out.println("Fetching Candlestick from " + from.toString());
      final List<CandlestickDataEntity> entities =
          binanceClient.getCandlesickData(symbol.toString(), from.toEpochMilli()).stream()
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

      from =
          entities.stream()
              .max(Comparator.comparing(CandlestickDataEntity::getOpenTime))
              .get()
              .getOpenTime();
    }


  }
}
