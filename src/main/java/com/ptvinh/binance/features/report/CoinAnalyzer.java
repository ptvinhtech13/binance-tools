package com.ptvinh.binance.features.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptvinh.binance.domain.CoinPair;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.crawling.candlestick.CandlestickRepository;
import com.ptvinh.binance.features.report.model.CoinReportEntity;
import com.ptvinh.binance.utils.ConsoleColors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.ptvinh.binance.domain.CoinPair.CAKEUSDT;
import static com.ptvinh.binance.domain.CoinPair.LINAUSDT;
import static com.ptvinh.binance.domain.CoinPair.TOMOUSDT;

@Slf4j
@Service
public class CoinAnalyzer {

  private static final int CLEANUP_REPORT_MAX = 100;
  private final AtomicInteger counter = new AtomicInteger(CLEANUP_REPORT_MAX);
  private final CandlestickRepository candlestickRepository;
  private final ObjectMapper objectMapper;
  private final CoinReportRepository coinReportRepository;

  private final DecimalFormat newFormat = new DecimalFormat("#.#####");

  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final List<CoinPair> skippedCoins = Arrays.asList(
      TOMOUSDT, CAKEUSDT, LINAUSDT);

  @Autowired
  public CoinAnalyzer(CandlestickRepository candlestickRepository, CoinReportRepository coinReportRepository, ObjectMapper objectMapper) {
    this.candlestickRepository = candlestickRepository;
    this.objectMapper = objectMapper;
    this.coinReportRepository = coinReportRepository;
  }

  @Scheduled(fixedDelay = 5000)
  public void reportCoins() throws IOException {

    final List<CoinReportEntity> coinReportEntities = Arrays.stream(CoinPair.values()).map(this::runReport).filter(el -> Objects.nonNull(el) && !skippedCoins.contains(el.getSymbol())).collect(Collectors.toList());
    if (!coinReportEntities.isEmpty()) {
      System.out.println(String.format("##################### Report: %s #####################", LocalDateTime.now().format(timeFormatter)));
      final String format = "%-5s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s "
          + "%-10s %-10s\n";
      coinReportEntities.forEach(
          coinReportEntity -> System.out.format(
              format,
              decorateColor(coinReportEntity) + "symbol:",
              coinReportEntity.getSymbol().toString(),
              "curPrice:",
              newFormat.format(coinReportEntity.getCurrentPrice()),
              "changes:",
              newFormat.format(coinReportEntity.getPercentDelta()),
              "lowPrIn1h:",
              newFormat.format(coinReportEntity.getLowestPriceIn1h()),
              "highPrIn1h:",
              newFormat.format(coinReportEntity.getHighestPriceIn1h()),
              "lowPrIn10h:",
              newFormat.format(coinReportEntity.getLowestPriceIn()),
              "highPrIn10h:",
              newFormat.format(coinReportEntity.getHighestPriceIn()),
              "perDown:" + ConsoleColors.RESET,
              newFormat.format(coinReportEntity.getPercentDown())));
      System.out.println("##################### Done: #####################");
      coinReportRepository.saveAll(coinReportEntities);
    }
    if (counter.decrementAndGet() == 0) {
      Runtime.getRuntime().exec("clear");
      counter.set(CLEANUP_REPORT_MAX);
    }
  }

  private String decorateColor(CoinReportEntity coinReportEntity) {
    if (coinReportEntity.isGoingUp()) {
      return ConsoleColors.BLUE;
    }
    if (coinReportEntity.getPercentDown() < 0) {
      return ConsoleColors.RED;
    }
    if (coinReportEntity.getPercentDown() == 0) {
      return ConsoleColors.GREEN;
    }
    return "";
  }

  private CoinReportEntity runReport(CoinPair symbol) {
    final Optional<CandlestickDataEntity> optional = candlestickRepository.findOneByMaxCloseTime(symbol.toString());

    if (optional.isEmpty()) {
      log.info("Skipping report: " + symbol);
      return null;
    }

    final CandlestickDataEntity current = optional.get();
    final List<CandlestickDataEntity> candlestickDataEntities1h = candlestickRepository.findInTimeFrameBySymbol(symbol.toString(), new Timestamp(current.getCloseTime().minus(1, ChronoUnit.HOURS).toEpochMilli()));
    final List<CandlestickDataEntity> candlestickDataEntities10h = candlestickRepository.findInTimeFrameBySymbol(symbol.toString(), new Timestamp(current.getCloseTime().minus(10, ChronoUnit.HOURS).toEpochMilli()));
    final double currentPrice = current.getClosePrice().doubleValue();
    final double highestPriceIn10h = candlestickDataEntities10h.stream().max(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double lowestPriceIn10h = candlestickDataEntities10h.stream().min(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double highestPriceIn1h = candlestickDataEntities1h.stream().max(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double lowestPriceIn1h = candlestickDataEntities1h.stream().min(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();

    final double average = 1 - current.getClosePrice().doubleValue() / highestPriceIn10h;
    final double currentPerDown = average * 100 * -1;

    final Optional<CoinReportEntity> latestReport = coinReportRepository.findByLatestCoinReportBySymbol(symbol.toString());

    final CoinReportEntity reportEntity = CoinReportEntity.builder()
        .symbol(symbol)
        .currentPrice(currentPrice)
        .highestPriceIn(highestPriceIn10h)
        .lowestPriceIn(lowestPriceIn10h)
        .lowestPriceIn1h(lowestPriceIn1h)
        .highestPriceIn1h(highestPriceIn1h)
        .percentDown(currentPerDown)
        .percentDelta(currentPrice - latestReport.map(CoinReportEntity::getCurrentPrice).orElse(0d))
        .isGoingUp(currentPrice - latestReport.map(CoinReportEntity::getCurrentPrice).orElse(0d) >= 0)
        .build();

    if (latestReport.isPresent() && compareReport(latestReport.get(), reportEntity)) {
      return null;
    }

    return reportEntity;

  }

  private boolean compareReport(CoinReportEntity latest, CoinReportEntity current) {
    return (0 == Double.compare(latest.getCurrentPrice(), current.getCurrentPrice()));
  }
}
