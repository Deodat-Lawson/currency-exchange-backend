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
@Measurement(name = "crypto_price")
public class CurrentPrice {

  @Column(tag = true)
  private String symbol;

  @Column
  private BigDecimal price;

  @Column(timestamp = true)
  private Instant timestamp;
}
