package com.ptvinh.binance.features.crawling.candlestick;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ptvinh.binance.domain.CoinPair;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.client.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CandlestickCrawling {

  @Value("${crawing.config.init-crawling-date:2021-04-10T00:00:00.000Z}")
  private String initCrawlingDate;

  private final CandlestickRepository candlestickRepository;
  private final BinanceClient binanceClient;
  private final Lock bnbUsdtLock = new ReentrantLock();
  private final Lock dogeUsdtLock = new ReentrantLock();
  private final Lock adaUsdtLock = new ReentrantLock();
  private final Lock tkoUsdtLock = new ReentrantLock();
  private final Lock tomoUsdtLock = new ReentrantLock();
  private final Lock cakeUsdtLock = new ReentrantLock();
  private final Lock linaUsdtLock = new ReentrantLock();
  private final Lock trxUsdtLock = new ReentrantLock();
  private final Lock winUsdtLock = new ReentrantLock();
  private final Lock bttUsdtLock = new ReentrantLock();
  private final Lock btcUsdtLock = new ReentrantLock();

  @Autowired
  public CandlestickCrawling(BinanceClient client, CandlestickRepository candlestickRepository) {
    this.candlestickRepository = candlestickRepository;
    this.binanceClient = client;
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchBnbUsdt() throws InterruptedException {
    final CoinPair symbol = CoinPair.BNBUSDT;
    bnbUsdtLock.lock();
    fetchRawData(symbol);
    bnbUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchADAUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.ADAUSDT;
    adaUsdtLock.lock();
    fetchRawData(symbol);
    adaUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchTKOUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.TKOUSDT;
    tkoUsdtLock.lock();
    fetchRawData(symbol);
    tkoUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchDogeUsdt() throws InterruptedException {
    final CoinPair symbol = CoinPair.DOGEUSDT;
    dogeUsdtLock.lock();
    fetchRawData(symbol);
    dogeUsdtLock.unlock();
  }

  //  @Async
  //  @Scheduled(fixedDelay = 2000)
  //  public void fetchTomoUsdt() throws InterruptedException {
  //    final CoinPair symbol = CoinPair.TOMOUSDT;
  //    tomoUsdtLock.lock();
  //    fetchRawData(symbol);
  //    tomoUsdtLock.unlock();
  //  }

  //  @Async
  //  @Scheduled(fixedDelay = 2000)
  //  public void fetchCAKEUSDT() throws InterruptedException {
  //    final CoinPair symbol = CoinPair.CAKEUSDT;
  //    cakeUsdtLock.lock();
  //    fetchRawData(symbol);
  //    cakeUsdtLock.unlock();
  //  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchBTCUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.BTCUSDT;
    btcUsdtLock.lock();
    fetchRawData(symbol);
    btcUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchWinUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.WINUSDT;
    winUsdtLock.lock();
    fetchRawData(symbol);
    winUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchBTTUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.BTTUSDT;
    bttUsdtLock.lock();
    fetchRawData(symbol);
    bttUsdtLock.unlock();
  }

  @Async
  @Scheduled(fixedDelay = 2000)
  public void fetchTRXUSDT() throws InterruptedException {
    final CoinPair symbol = CoinPair.TRXUSDT;
    trxUsdtLock.lock();
    fetchRawData(symbol);
    trxUsdtLock.unlock();
  }

  //  @Async
  //  @Scheduled(fixedDelay = 2000)
  //  public void fetchLINAUSDT() throws InterruptedException {
  //    final CoinPair symbol = CoinPair.LINAUSDT;
  //    linaUsdtLock.lock();
  //    fetchRawData(symbol);
  //    linaUsdtLock.unlock();
  //  }

  private void fetchRawData(CoinPair symbol) throws InterruptedException {
    final Instant latest = candlestickRepository.findOneByMaxCloseTime(symbol.toString())
        .map(CandlestickDataEntity::getCloseTime)
        .orElse(Instant.parse(initCrawlingDate));

    final Instant now = Instant.now();
    // log.info(String.format("%s: FetchRawData: start from %s to %s", symbol, latest, now));

    if (latest.isBefore(now)) {
      //            log.info(String.format("%s: Fetching Candlestick: from %s to %s", symbol, latest, now));
      final List<CandlestickDataEntity> entities = binanceClient.getCandlesickData(symbol.toString(), latest.toEpochMilli()).stream()
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
      //            log.info(String.format("%s: FetchRawData is latest", symbol));
    }

  }
}
