package com.example.controller;

import com.example.domain.CurrencyMarketData;
import com.example.service.CurrencyMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketdata")
public class CurrencyMarketDataController {

  private final CurrencyMarketDataService marketDataService;

  @Autowired
  public CurrencyMarketDataController(CurrencyMarketDataService marketDataService) {
    this.marketDataService = marketDataService;
  }

  // GET /api/marketdata/{symbol}
  // Retrieves aggregated market data for the specified symbol
  @GetMapping("/{symbol}")
  public ResponseEntity<CurrencyMarketData> getMarketData(@PathVariable String symbol) {
    CurrencyMarketData marketData = marketDataService.getMarketData(symbol);
    if (marketData == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(marketData);
  }
}
