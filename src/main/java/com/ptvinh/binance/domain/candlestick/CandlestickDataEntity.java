package com.ptvinh.binance.domain.candlestick;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "candlestick_data")
@EntityListeners(AuditingEntityListener.class)
public class CandlestickDataEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "candlestickDataSeqGen")
  @SequenceGenerator(name = "candlestickDataSeqGen", sequenceName = "candlestickDataSeq", initialValue = 1, allocationSize = 1000)
  private Long id;

  @Column(name = "open_time")
  private Instant openTime;

  @Column(name = "open_price")
  private BigDecimal openPrice;

  @Column(name = "high_price")
  private BigDecimal highPrice;

  @Column(name = "low_price")
  private BigDecimal lowPrice;

  @Column(name = "close_price")
  private BigDecimal closePrice;

  @Column(name = "volume")
  private BigDecimal volume;

  @Column(name = "close_time")
  private Instant closeTime;

  @Column(name = "base_asset_volume")
  private BigDecimal baseAssetVolume;

  @Column(name = "quote_asset_volume")
  private BigDecimal quoteAssetVolume;
}
