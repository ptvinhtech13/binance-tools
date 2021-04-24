package com.ptvinh.binance.features.report.model;

import com.ptvinh.binance.domain.CoinPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "coin_report")
@EntityListeners(AuditingEntityListener.class)
public class CoinReportEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coinReportSeqGen")
  @SequenceGenerator(name = "coinReportSeqGen", sequenceName = "coinReportSeq", initialValue = 1, allocationSize = 1000)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "symbol")
  private CoinPair symbol;

  @Column(name = "lowest_price")
  private double lowestPriceIn;

  @Column(name = "highest_price")
  private double highestPriceIn;

  @Column(name = "current_price")
  private double currentPrice;

  @Column(name = "percent_down")
  private double percentDown;

  @Column(name = "percent_delta")
  private double percentDelta;

  @Column(name = "lowest_price_in_1h")
  private double lowestPriceIn1h;

  @Column(name = "highest_price_in_1h")
  private double highestPriceIn1h;

  @CreatedDate
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Transient
  private boolean isGoingUp;
}
