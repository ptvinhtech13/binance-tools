package com.ptvinh.binance.features.crawling.candlestick;

import com.ptvinh.binance.domain.candlestick.CandlestickDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandlestickRepository extends JpaRepository<CandlestickDataEntity, Long> {
  @Query(value = "select *\n" +
      "from candlestick_data\n" +
      "where symbol = :symbol\n" +
      "  and close_time = \n" +
      "      (select max(close_time) \n" +
      "       from candlestick_data" +
      "       where symbol = :symbol)", nativeQuery = true)
  Optional<CandlestickDataEntity> findOneByMaxCloseTime(@Param("symbol") String symbol);

  @Query(value = "select *\n" +
      "from candlestick_data\n" +
      "where symbol = :symbol\n" +
      "  and close_time >= :endTime", nativeQuery = true)
  List<CandlestickDataEntity> findInTimeFrameBySymbol(@Param("symbol") String symbol, @Param("endTime") Timestamp endTime);
}
