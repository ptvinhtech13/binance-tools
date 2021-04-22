package com.ptvinh.binance.features.crawling.candlestick;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ptvinh.binance.domain.CoinPair;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.client.BinanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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


  @PostConstruct
  private void crawling() {
    List<CandlestickDataEntity> entities =
        binanceClient.getCandlesickData(CoinPair.DOGEUSDT.toString()).stream()
            .map(node -> {
              final Iterator<JsonNode> iterator = node.elements();
              final List<Object> result = new ArrayList<>();
              while (iterator.hasNext()) {
                result.add(iterator.next());
              }
              return CandlestickDataEntity.builder()
                  .openTime(Instant.ofEpochMilli(((LongNode) result.get(0)).longValue()))
                  .openPrice(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(1)).asText())))
                  .highPrice(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(2)).asText())))
                  .lowPrice(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(3)).asText())))
                  .closePrice(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(4)).asText())))
                  .volume(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(5)).asText())))
                  .closeTime(Instant.ofEpochMilli(((LongNode) result.get(6)).longValue()))
                  .quoteAssetVolume(BigDecimal.valueOf(Double.valueOf(((TextNode) result.get(7)).asText())))
                  .build();
            })

            .collect(Collectors.toList());

    candlestickRepository.saveAll(entities);
  }
}
