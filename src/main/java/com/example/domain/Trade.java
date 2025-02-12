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
@Measurement(name = "trades")
public class Trade {

  @Column(tag = true)
  private String tradeId;

  @Column(tag = true)
  private String symbol;

  @Column
  private BigDecimal price;

  @Column
  private BigDecimal quantity;

  @Column
  private String side;  // e.g., "buy" or "sell"

  @Column(timestamp = true)
  private Instant timestamp;
}
