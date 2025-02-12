package com.example.domain;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "symbol_information")
public class SymbolInformation {

  @Column(tag = true)
  private String symbol;      // e.g., "BTCUSD"

  @Column
  private String baseAsset;   // e.g., "BTC"

  @Column
  private String quoteAsset;  // e.g., "USD"

  @Column
  private String status;      // e.g., "TRADING", "HALTED"

  @Column
  private String country;     // e.g., "US" (if applicable)
}
