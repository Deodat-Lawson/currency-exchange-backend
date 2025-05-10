package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport {
    private Instant reportTime;
    private int totalSymbols;
    private int bullishSymbols;
    private int bearishSymbols;
    private int neutralSymbols;
    
    // Top performing symbols by strength
    private List<SymbolTrend> topBullish;
    private List<SymbolTrend> topBearish;
    
    // Symbols with significant patterns
    private List<SymbolPattern> significantPatterns;
    
    // Market correlation data
    private Map<String, Map<String, BigDecimal>> correlationMatrix;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolTrend {
        private String symbol;
        private String trend;
        private BigDecimal strength;
        private BigDecimal rsi;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolPattern {
        private String symbol;
        private String pattern;
        private String trend;
        private BigDecimal currentPrice;
    }
} 