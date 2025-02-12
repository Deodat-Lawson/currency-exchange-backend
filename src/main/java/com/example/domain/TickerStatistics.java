package com.example.domain;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "ticker_statistics")
public class TickerStatistics {

  @Column(tag = true)
  private String symbol;

  @Column
  private BigDecimal openPrice;

  @Column
  private BigDecimal highPrice;

  @Column
  private BigDecimal lowPrice;

  @Column
  private BigDecimal lastPrice;

  @Column
  private BigDecimal volume;

  @Column
  private BigDecimal priceChange;

  @Column
  private BigDecimal priceChangePercent;

  @Column(timestamp = true)
  private Instant timestamp;
}
