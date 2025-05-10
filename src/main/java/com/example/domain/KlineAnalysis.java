package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KlineAnalysis {
    private String symbol;
    private Instant analysisTime;
    
    // Technical indicators
    private BigDecimal sma20;  // 20-period Simple Moving Average
    private BigDecimal ema14;  // 14-period Exponential Moving Average
    private BigDecimal rsi14;  // 14-period Relative Strength Index
    private BigDecimal macd;   // Moving Average Convergence Divergence
    private BigDecimal macdSignal; // MACD signal line
    private BigDecimal macdHistogram; // MACD histogram
    private BigDecimal bollingerUpper; // Bollinger Band upper
    private BigDecimal bollingerMiddle; // Bollinger Band middle (SMA20)
    private BigDecimal bollingerLower; // Bollinger Band lower
    
    // Volume analysis
    private BigDecimal volumeSMA5; // 5-period volume SMA
    private BigDecimal obv; // On Balance Volume
    
    // Price patterns (true/false indicators)
    private boolean hammerPattern;
    private boolean engulfingPattern;
    private boolean doji;
    
    // Support/Resistance levels
    private List<BigDecimal> supportLevels;
    private List<BigDecimal> resistanceLevels;
    
    // Market trend indicators
    private String overallTrend; // "BULLISH", "BEARISH", or "NEUTRAL"
    private BigDecimal trendStrength; // 0-100 scale
} 