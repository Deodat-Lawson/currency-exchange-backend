package com.example.service;

import com.example.domain.*;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BinanceDataService {

    private final WebClient webClient;
    private final InfluxDBClient influxDBClient;
    private final WriteApi writeApi;

    @Value("${data.symbols}")
    private String symbolsConfig;

    @Autowired
    public BinanceDataService(WebClient.Builder webClientBuilder,
                              InfluxDBClient influxDBClient,
                              @Value("${binance.api.base-url}") String apiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(apiBaseUrl).build();
        this.influxDBClient = influxDBClient;
        this.writeApi = influxDBClient.makeWriteApi();
    }

    /**
     * Get the configured symbols list
     */
    public String getSymbolsConfig() {
        return symbolsConfig;
    }

    /**
     * Scheduled task to fetch and store market data
     */
    @Scheduled(fixedRateString = "${data.collection.interval.seconds:60}000")
    public void fetchAndStoreMarketData() {
        log.info("Fetching market data from Binance...");
        List<String> symbols = Arrays.asList(symbolsConfig.split(","));
        
        symbols.forEach(symbol -> {
            fetchCurrentPrice(symbol)
                .doOnNext(price -> writeApi.writeMeasurement(WritePrecision.MS, price))
                .subscribe(
                    price -> log.info("Stored current price for {}: {}", symbol, price.getPrice()),
                    error -> log.error("Error fetching current price for {}: {}", symbol, error.getMessage())
                );
            
            fetchKlines(symbol)
                .doOnNext(kline -> writeApi.writeMeasurement(WritePrecision.MS, kline))
                .subscribe(
                    kline -> log.info("Stored kline for {}", symbol),
                    error -> log.error("Error fetching klines for {}: {}", symbol, error.getMessage())
                );
            
            fetchTickerStatistics(symbol)
                .doOnNext(stats -> writeApi.writeMeasurement(WritePrecision.MS, stats))
                .subscribe(
                    stats -> log.info("Stored ticker statistics for {}", symbol),
                    error -> log.error("Error fetching ticker statistics for {}: {}", symbol, error.getMessage())
                );
        });
    }

    /**
     * Fetch current price from Binance API
     */
    public Mono<CurrentPrice> fetchCurrentPrice(String symbol) {
        return webClient.get()
                .uri("/api/v3/ticker/price?symbol={symbol}", symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    CurrentPrice price = new CurrentPrice();
                    price.setSymbol(symbol);
                    price.setPrice(new BigDecimal(response.get("price").toString()));
                    price.setTimestamp(Instant.now());
                    return price;
                });
    }

    /**
     * Fetch kline (candlestick) data from Binance API
     */
    public Flux<Kline> fetchKlines(String symbol) {
        return webClient.get()
                .uri("/api/v3/klines?symbol={symbol}&interval=1m&limit=60", symbol)
                .retrieve()
                .bodyToFlux(List.class)
                .map(klineData -> {
                    Kline kline = new Kline();
                    kline.setSymbol(symbol);
                    kline.setOpenTime(Instant.ofEpochMilli(Long.parseLong(klineData.get(0).toString())));
                    kline.setCloseTime(Instant.ofEpochMilli(Long.parseLong(klineData.get(6).toString())));
                    kline.setOpen(new BigDecimal(klineData.get(1).toString()));
                    kline.setHigh(new BigDecimal(klineData.get(2).toString()));
                    kline.setLow(new BigDecimal(klineData.get(3).toString()));
                    kline.setClose(new BigDecimal(klineData.get(4).toString()));
                    kline.setVolume(new BigDecimal(klineData.get(5).toString()));
                    return kline;
                });
    }

    /**
     * Fetch 24hr ticker statistics from Binance API
     */
    public Mono<TickerStatistics> fetchTickerStatistics(String symbol) {
        return webClient.get()
                .uri("/api/v3/ticker/24hr?symbol={symbol}", symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    TickerStatistics stats = new TickerStatistics();
                    stats.setSymbol(symbol);
                    stats.setOpenPrice(new BigDecimal(response.get("openPrice").toString()));
                    stats.setHighPrice(new BigDecimal(response.get("highPrice").toString()));
                    stats.setLowPrice(new BigDecimal(response.get("lowPrice").toString()));
                    stats.setLastPrice(new BigDecimal(response.get("lastPrice").toString()));
                    stats.setVolume(new BigDecimal(response.get("volume").toString()));
                    stats.setPriceChange(new BigDecimal(response.get("priceChange").toString()));
                    stats.setPriceChangePercent(new BigDecimal(response.get("priceChangePercent").toString()));
                    stats.setTimestamp(Instant.now());
                    return stats;
                });
    }

    /**
     * Fetch trades from Binance API
     */
    public Flux<Trade> fetchRecentTrades(String symbol) {
        return webClient.get()
                .uri("/api/v3/trades?symbol={symbol}&limit=50", symbol)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(tradeData -> {
                    Trade trade = new Trade();
                    trade.setTradeId(tradeData.get("id").toString());
                    trade.setSymbol(symbol);
                    trade.setPrice(new BigDecimal(tradeData.get("price").toString()));
                    trade.setQuantity(new BigDecimal(tradeData.get("qty").toString()));
                    trade.setSide(Boolean.parseBoolean(tradeData.get("isBuyerMaker").toString()) ? "sell" : "buy");
                    trade.setTimestamp(Instant.ofEpochMilli(Long.parseLong(tradeData.get("time").toString())));
                    return trade;
                });
    }
} 