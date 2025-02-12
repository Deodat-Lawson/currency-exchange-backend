package com.example.service;

import com.example.domain.*;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyMarketDataService {

  private final InfluxDBClient influxDBClient;
  private final QueryApi queryApi;

  // The bucket name is configured in application.properties
  @Value("${influx.bucket}")
  private String bucket;

  @Autowired
  public CurrencyMarketDataService(InfluxDBClient influxDBClient) {
    this.influxDBClient = influxDBClient;
    this.queryApi = influxDBClient.getQueryApi();
  }

  // Query the latest current price from the "crypto_price" measurement
  public CurrentPrice getCurrentPrice(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -1m) " +
            "|> filter(fn: (r) => r._measurement == \"crypto_price\" and r.symbol == \"%s\") " +
            "|> last()",
        bucket, symbol
    );
    List<CurrentPrice> results = queryApi.query(flux, CurrentPrice.class);
    return (results != null && !results.isEmpty()) ? results.get(0) : null;
  }

  // Query the latest order book snapshot from the "order_book" measurement
  public OrderBook getOrderBook(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -1m) " +
            "|> filter(fn: (r) => r._measurement == \"order_book\" and r.symbol == \"%s\") " +
            "|> last()",
        bucket, symbol
    );
    List<OrderBook> results = queryApi.query(flux, OrderBook.class);
    return (results != null && !results.isEmpty()) ? results.get(0) : null;
  }

  // Query recent trades from the "trades" measurement (e.g., the last 5 minutes)
  public List<Trade> getRecentTrades(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -5m) " +
            "|> filter(fn: (r) => r._measurement == \"trades\" and r.symbol == \"%s\")",
        bucket, symbol
    );
    List<Trade> results = queryApi.query(flux, Trade.class);
    return results;
  }

  // Query candlestick (kline) data from the "klines" measurement (e.g., the last 1 hour)
  public List<Kline> getKlines(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r._measurement == \"klines\" and r.symbol == \"%s\")",
        bucket, symbol
    );
    List<Kline> results = queryApi.query(flux, Kline.class);
    return results;
  }

  // Query the latest 24-hour ticker statistics from the "ticker_statistics" measurement
  public TickerStatistics getTickerStatistics(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -1m) " +
            "|> filter(fn: (r) => r._measurement == \"ticker_statistics\" and r.symbol == \"%s\") " +
            "|> last()",
        bucket, symbol
    );
    List<TickerStatistics> results = queryApi.query(flux, TickerStatistics.class);
    return (results != null && !results.isEmpty()) ? results.get(0) : null;
  }

  // Query the latest symbol information from the "symbol_information" measurement
  public SymbolInformation getSymbolInformation(String symbol) {
    String flux = String.format(
        "from(bucket: \"%s\") " +
            "|> range(start: -1m) " +
            "|> filter(fn: (r) => r._measurement == \"symbol_information\" and r.symbol == \"%s\") " +
            "|> last()",
        bucket, symbol
    );
    List<SymbolInformation> results = queryApi.query(flux, SymbolInformation.class);
    return (results != null && !results.isEmpty()) ? results.get(0) : null;
  }

  // Aggregate the above data into a single CurrencyMarketData object
  public CurrencyMarketData getMarketData(String symbol) {
    CurrentPrice currentPrice = getCurrentPrice(symbol);
    OrderBook orderBook = getOrderBook(symbol);
    List<Trade> recentTrades = getRecentTrades(symbol);
    List<Kline> klines = getKlines(symbol);
    TickerStatistics tickerStatistics = getTickerStatistics(symbol);
    SymbolInformation symbolInformation = getSymbolInformation(symbol);

    return new CurrencyMarketData(
        symbol,
        currentPrice,
        orderBook,
        recentTrades,
        klines,
        tickerStatistics,
        symbolInformation
    );
  }
}
