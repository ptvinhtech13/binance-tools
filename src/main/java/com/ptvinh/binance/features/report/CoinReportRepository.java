package com.ptvinh.binance.features.report;

import com.ptvinh.binance.features.report.model.CoinReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoinReportRepository extends JpaRepository<CoinReportEntity, Long> {

  @Query(value = "select *\n" +
      "from coin_report\n" +
      "where symbol = :symbol\n" +
      "  and created_at = \n" +
      "      (select max(created_at) \n" +
      "       from coin_report" +
      "       where symbol = :symbol)", nativeQuery = true)
  Optional<CoinReportEntity> findByLatestCoinReportBySymbol(@Param("symbol") String symbol);

  //  List<CoinReportEntity> findAllLatestReportIn(List<String> symbols);
}
