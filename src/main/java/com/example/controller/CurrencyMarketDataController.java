package com.example.controller;

import com.example.domain.CurrencyMarketData;
import com.example.domain.Kline;
import com.example.service.CurrencyMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  
  // GET /api/marketdata/klines/{symbol}
  // Retrieves kline (candlestick) data for the specified symbol
  @GetMapping("/klines/{symbol}")
  public ResponseEntity<List<Kline>> getKlines(@PathVariable String symbol) {
    List<Kline> klines = marketDataService.getKlines(symbol);
    if (klines == null || klines.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(klines);
  }
  
  // GET /api/marketdata/all
  // Retrieves market data for all available symbols
  @GetMapping("/all")
  public ResponseEntity<Map<String, CurrencyMarketData>> getAllMarketData() {
    List<String> symbols = marketDataService.getAllSymbols();
    Map<String, CurrencyMarketData> allData = new HashMap<>();
    
    symbols.forEach(symbol -> {
      CurrencyMarketData marketData = marketDataService.getMarketData(symbol);
      if (marketData != null) {
        allData.put(symbol, marketData);
      }
    });
    
    if (allData.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    
    return ResponseEntity.ok(allData);
  }
  
  // GET /api/marketdata/symbols
  // Retrieves all available currency symbols
  @GetMapping("/symbols")
  public ResponseEntity<List<String>> getAllSymbols() {
    List<String> symbols = marketDataService.getAllSymbols();
    return ResponseEntity.ok(symbols);
  }
}
