package com.example.domain;

import java.util.List;

public class CurrencyMarketData {
  private String symbol;
  private CurrentPrice currentPrice;
  private List<Trade> recentTrades;
  private List<Kline> klines;
  private TickerStatistics tickerStatistics;
  private SymbolInformation symbolInformation;

  // No-argument constructor (if needed)
  public CurrencyMarketData() {
  }

  // All-arguments constructor
  public CurrencyMarketData(String symbol, CurrentPrice currentPrice,
                            List<Trade> recentTrades, List<Kline> klines,
                            TickerStatistics tickerStatistics, SymbolInformation symbolInformation) {
    this.symbol = symbol;
    this.currentPrice = currentPrice;
    this.recentTrades = recentTrades;
    this.klines = klines;
    this.tickerStatistics = tickerStatistics;
    this.symbolInformation = symbolInformation;
  }

  // Getters and setters (or use Lombok @Data to generate them)
}
