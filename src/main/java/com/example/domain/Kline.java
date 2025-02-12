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
@Measurement(name = "klines")
public class Kline {

  @Column(tag = true)
  private String symbol;

  @Column(timestamp = true)
  private Instant openTime;

  @Column
  private Instant closeTime;

  @Column
  private BigDecimal open;

  @Column
  private BigDecimal high;

  @Column
  private BigDecimal low;

  @Column
  private BigDecimal close;

  @Column
  private BigDecimal volume;
}
