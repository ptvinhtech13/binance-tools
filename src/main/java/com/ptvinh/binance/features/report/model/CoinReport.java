package com.ptvinh.binance.features.report.model;

import com.ptvinh.binance.domain.CoinPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CoinReport {
    private CoinPair symbol;
    private double lowestPriceIn;
    private double highestPriceIn;
    private double currentPrice;
    private double averageIn;
    private Double percentDown;

//    private List<CoinTimeFrameReport> timeFrameReport;
}
