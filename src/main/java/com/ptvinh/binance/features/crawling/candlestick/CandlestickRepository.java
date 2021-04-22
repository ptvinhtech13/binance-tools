package com.ptvinh.binance.features.crawling.candlestick;

import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandlestickRepository extends JpaRepository<CandlestickDataEntity, Long> {
}
