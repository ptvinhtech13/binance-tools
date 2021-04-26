package com.ptvinh.binance.features.report;

import com.ptvinh.binance.config.DynamicEnvConfig;
import com.ptvinh.binance.domain.CoinPropertiesConfig;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoinAnalyzer {

  private static final int CLEANUP_REPORT_MAX = 100;
  private final AtomicInteger counter = new AtomicInteger(CLEANUP_REPORT_MAX);
  private final CandlestickRepository candlestickRepository;
  private final CoinReportRepository coinReportRepository;

  private final DecimalFormat newFormat = new DecimalFormat("#.######");

  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DynamicEnvConfig dynamicEnvConfig;

  @Autowired
  public CoinAnalyzer(CandlestickRepository candlestickRepository,
      CoinReportRepository coinReportRepository,
      DynamicEnvConfig dynamicEnvConfig) {
    this.candlestickRepository = candlestickRepository;
    this.dynamicEnvConfig = dynamicEnvConfig;
    this.coinReportRepository = coinReportRepository;
  }

  @Scheduled(fixedDelay = 5000)
  public void reportCoins() throws IOException {
    final Map<String, CoinPropertiesConfig.CoinProperties> coinPros = dynamicEnvConfig.getCoinPropertiesConfig().getSymbols().stream().collect(Collectors.toMap(el -> el.getSymbol(), Function.identity()));
    final List<CoinReportEntity> coinReportEntities = dynamicEnvConfig.getCoinPropertiesConfig().getSymbols().stream()
        .filter(CoinPropertiesConfig.CoinProperties::isDisabledReport)
        .map(this::runReport)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (!coinReportEntities.isEmpty()) {
      System.out.println(String.format("##################### Report: %s #####################", LocalDateTime.now().format(timeFormatter)));
      final String format = "%-5s %-10s "
          + "%-8s %-20s "
          + "%-8s %-10s "
          + "%-8s %-10s "
          + "%-8s %-10s "
          + "%-8s %-10s "
          + "%-8s %-10s "
          + "%-8s %-10s\n";
      coinReportEntities.forEach(
          coinReportEntity -> {
            CoinPropertiesConfig.CoinProperties coinProperties = coinPros.get(coinReportEntity.getSymbol());
            System.out.format(
                format,
                decorateColor(coinReportEntity) + "symbol:",
                coinProperties.getSymbolDisplay(),
                "curPrice:",
                newFormat.format(coinReportEntity.getCurrentPrice()) + (Objects.isNull(coinProperties.getDownTarget()) ? "" : "-[" + newFormat.format(coinProperties.getDownTarget()) + "]"),
                "changes:",
                newFormat.format(coinReportEntity.getPercentDelta()),
                "lowP1h:",
                newFormat.format(coinReportEntity.getLowestPriceIn1h()),
                "highP1h:",
                newFormat.format(coinReportEntity.getHighestPriceIn1h()),
                "lowP10h:",
                newFormat.format(coinReportEntity.getLowestPriceIn()),
                "highP10h:",
                newFormat.format(coinReportEntity.getHighestPriceIn()),
                "perDown:" + ConsoleColors.RESET,
                newFormat.format(coinReportEntity.getPercentDown()));
          });
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

  private CoinReportEntity runReport(CoinPropertiesConfig.CoinProperties coinProperties) {
    final String symbol = coinProperties.getSymbol();
    final Optional<CandlestickDataEntity> optional = candlestickRepository.findOneByMaxCloseTime(symbol);

    if (optional.isEmpty()) {
      log.info("Skipping report: " + symbol);
      return null;
    }

    final CandlestickDataEntity current = optional.get();
    final List<CandlestickDataEntity> candlestickDataEntities1h = candlestickRepository.findInTimeFrameBySymbol(symbol, new Timestamp(current.getCloseTime().minus(1, ChronoUnit.HOURS).toEpochMilli()));
    final List<CandlestickDataEntity> candlestickDataEntities10h = candlestickRepository.findInTimeFrameBySymbol(symbol, new Timestamp(current.getCloseTime().minus(10, ChronoUnit.HOURS).toEpochMilli()));
    final double currentPrice = current.getClosePrice().doubleValue();
    final double highestPriceIn10h = candlestickDataEntities10h.stream().max(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double lowestPriceIn10h = candlestickDataEntities10h.stream().min(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double highestPriceIn1h = candlestickDataEntities1h.stream().max(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
    final double lowestPriceIn1h = candlestickDataEntities1h.stream().min(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();

    final double average = 1 - current.getClosePrice().doubleValue() / highestPriceIn10h;
    final double currentPerDown = average * 100 * -1;

    final Optional<CoinReportEntity> latestReport = coinReportRepository.findByLatestCoinReportBySymbol(symbol);

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
      //      System.out.println(latestReport.get().toString());
      //      System.out.println(reportEntity.toString());
      return null;
    }

    return reportEntity;

  }

  private boolean compareReport(CoinReportEntity latest, CoinReportEntity current) {
    return (0 == Double.compare(latest.getCurrentPrice(), current.getCurrentPrice()));
  }
}
