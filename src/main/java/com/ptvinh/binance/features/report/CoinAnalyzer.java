package com.ptvinh.binance.features.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptvinh.binance.domain.CoinPair;
import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import com.ptvinh.binance.features.crawling.candlestick.CandlestickRepository;
import com.ptvinh.binance.features.report.model.CoinReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoinAnalyzer {

    private final CandlestickRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public CoinAnalyzer(CandlestickRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void reportCoins() throws JsonProcessingException {
        final List<CoinReport> coinReports = Arrays.stream(CoinPair.values()).map(this::runReport).collect(Collectors.toList());
        System.out.println("##################### Report #####################");

        final String format = "%-5s %-10s %-10s %-10s %-10s %-10s\n";
        coinReports.forEach(coinReport -> System.out.format(format, "symbol:", coinReport.getSymbol().toString(), "currentPrice:", coinReport.getCurrentPrice(), "percentDown:", coinReport.getPercentDown()));
        System.out.println("##################### Done: #####################");
    }

    private CoinReport runReport(CoinPair symbol)  {
        final Optional<CandlestickDataEntity> optional = repository.findOneByMaxCloseTime(symbol.toString());

        if (optional.isEmpty()) {
            log.info("Skipping report: " + symbol);
            return null;
        }

        final CandlestickDataEntity current = optional.get();

        final List<CandlestickDataEntity> candlestickDataEntities =
                repository.findInTimeFrameBySymbol(symbol.toString(), new Timestamp(current.getCloseTime().minus(10, ChronoUnit.HOURS).toEpochMilli()));
        final double currentPrice = current.getClosePrice().doubleValue();
        final double highestPriceIn = candlestickDataEntities.stream().max(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
        final double lowestPriceIn = candlestickDataEntities.stream().min(Comparator.comparing(CandlestickDataEntity::getClosePrice)).get().getClosePrice().doubleValue();
        final double average = 1 - current.getClosePrice().doubleValue() / highestPriceIn;
        final CoinReport report = CoinReport.builder()
                .symbol(symbol)
                .currentPrice(currentPrice)
                .highestPriceIn(highestPriceIn)
                .lowestPriceIn(lowestPriceIn)
                .averageIn(candlestickDataEntities.stream().mapToDouble(entry -> entry.getClosePrice().doubleValue()).average().orElse(0d))
                .percentDown(average * 100 * -1)
                .build();

//        log.info(String.format("%s: report: %s", symbol, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report)));
        return report;
    }
}
