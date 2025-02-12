package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyMarketData {

  private String symbol; // e.g., "BTCUSD"
  private CurrentPrice currentPrice;
  private OrderBook orderBook;
  private List<Trade> recentTrades;
  private List<Kline> klines;
  private TickerStatistics tickerStatistics;
  private SymbolInformation symbolInformation;
}
