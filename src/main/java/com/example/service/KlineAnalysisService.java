package com.example.service;

import com.example.domain.Kline;
import com.example.domain.KlineAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class KlineAnalysisService {

    private final CurrencyMarketDataService marketDataService;

    @Autowired
    public KlineAnalysisService(CurrencyMarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * Perform technical analysis on kline data for a symbol
     */
    @Cacheable(value = "klineAnalysis", key = "#symbol")
    public KlineAnalysis analyzeKlines(String symbol) {
        List<Kline> klines = marketDataService.getKlines(symbol);
        if (klines == null || klines.isEmpty()) {
            log.error("No kline data available for {}", symbol);
            return null;
        }
        
        // Sort klines by time (newest last)
        klines.sort((a, b) -> a.getOpenTime().compareTo(b.getOpenTime()));
        
        KlineAnalysis analysis = new KlineAnalysis();
        analysis.setSymbol(symbol);
        analysis.setAnalysisTime(Instant.now());
        
        // Extract closing prices for analysis
        List<BigDecimal> closePrices = klines.stream()
                .map(Kline::getClose)
                .collect(Collectors.toList());
                
        // Extract volumes for analysis
        List<BigDecimal> volumes = klines.stream()
                .map(Kline::getVolume)
                .collect(Collectors.toList());
                
        // Calculate technical indicators
        calculateMovingAverages(analysis, closePrices);
        calculateRSI(analysis, closePrices);
        calculateMACD(analysis, closePrices);
        calculateBollingerBands(analysis, closePrices);
        calculateVolumeIndicators(analysis, closePrices, volumes);
        identifyPricePatterns(analysis, klines);
        findSupportResistanceLevels(analysis, klines);
        determineTrend(analysis, closePrices, klines);
        
        return analysis;
    }
    
    /**
     * Calculate SMA and EMA indicators
     */
    private void calculateMovingAverages(KlineAnalysis analysis, List<BigDecimal> closePrices) {
        int size = closePrices.size();
        
        // SMA20 calculation
        if (size >= 20) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = size - 20; i < size; i++) {
                sum = sum.add(closePrices.get(i));
            }
            BigDecimal sma20 = sum.divide(BigDecimal.valueOf(20), 8, RoundingMode.HALF_UP);
            analysis.setSma20(sma20);
            analysis.setBollingerMiddle(sma20); // Middle bollinger band is the SMA20
        }
        
        // EMA14 calculation
        if (size >= 14) {
            // Simple initial EMA calculation
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = size - 14; i < size; i++) {
                sum = sum.add(closePrices.get(i));
            }
            BigDecimal initialEMA = sum.divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
            
            // Multiplier: 2/(period + 1) = 2/15 = 0.1333
            BigDecimal multiplier = BigDecimal.valueOf(2.0 / (14 + 1));
            
            BigDecimal ema = initialEMA;
            for (int i = size - 14; i < size; i++) {
                // EMA = (Close - Previous EMA) * multiplier + Previous EMA
                ema = closePrices.get(i).subtract(ema)
                        .multiply(multiplier)
                        .add(ema);
            }
            analysis.setEma14(ema);
        }
    }
    
    /**
     * Calculate RSI (Relative Strength Index)
     */
    private void calculateRSI(KlineAnalysis analysis, List<BigDecimal> closePrices) {
        int size = closePrices.size();
        if (size < 15) { // Need at least 15 periods to calculate 14-period RSI
            return;
        }
        
        // Calculate price changes
        List<BigDecimal> changes = new ArrayList<>();
        for (int i = 1; i < size; i++) {
            changes.add(closePrices.get(i).subtract(closePrices.get(i - 1)));
        }
        
        // Separate gains (positive) and losses (negative)
        List<BigDecimal> gains = changes.stream()
                .map(change -> change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO)
                .collect(Collectors.toList());
                
        List<BigDecimal> losses = changes.stream()
                .map(change -> change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO)
                .collect(Collectors.toList());
        
        // Calculate initial averages for first 14 periods
        BigDecimal avgGain = gains.subList(0, 14).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
                
        BigDecimal avgLoss = losses.subList(0, 14).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
        
        // For remaining periods, use smoothed averages
        for (int i = 14; i < changes.size(); i++) {
            avgGain = (avgGain.multiply(BigDecimal.valueOf(13))
                    .add(gains.get(i)))
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
                    
            avgLoss = (avgLoss.multiply(BigDecimal.valueOf(13))
                    .add(losses.get(i)))
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
        }
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            analysis.setRsi14(BigDecimal.valueOf(100));
        } else {
            BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
            BigDecimal rsi = BigDecimal.valueOf(100)
                    .subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP));
            analysis.setRsi14(rsi);
        }
    }
    
    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    private void calculateMACD(KlineAnalysis analysis, List<BigDecimal> closePrices) {
        int size = closePrices.size();
        if (size < 26) { // Need at least 26 data points for MACD
            return;
        }
        
        // Calculate 12-period EMA
        BigDecimal ema12 = calculateEMA(closePrices, 12);
        
        // Calculate 26-period EMA
        BigDecimal ema26 = calculateEMA(closePrices, 26);
        
        // MACD Line = 12-period EMA - 26-period EMA
        BigDecimal macd = ema12.subtract(ema26);
        analysis.setMacd(macd);
        
        // Calculate Signal Line (9-period EMA of MACD Line)
        // For this, we would need historical MACD values, simplified approach:
        BigDecimal signal = macd.multiply(BigDecimal.valueOf(0.2)).add(macd.multiply(BigDecimal.valueOf(0.8)));
        analysis.setMacdSignal(signal);
        
        // MACD Histogram = MACD Line - Signal Line
        BigDecimal histogram = macd.subtract(signal);
        analysis.setMacdHistogram(histogram);
    }
    
    /**
     * Helper method to calculate EMA for any period
     */
    private BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        int size = prices.size();
        if (size < period) {
            return BigDecimal.ZERO;
        }
        
        // Calculate SMA as initial EMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = size - period; i < size; i++) {
            sum = sum.add(prices.get(i));
        }
        BigDecimal ema = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        
        // Multiplier: 2/(period + 1)
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        
        // Calculate EMA
        for (int i = size - period + 1; i < size; i++) {
            ema = prices.get(i).subtract(ema)
                    .multiply(multiplier)
                    .add(ema);
        }
        
        return ema;
    }
    
    /**
     * Calculate Bollinger Bands
     */
    private void calculateBollingerBands(KlineAnalysis analysis, List<BigDecimal> closePrices) {
        int size = closePrices.size();
        if (size < 20 || analysis.getSma20() == null) {
            return;
        }
        
        // Calculate standard deviation of last 20 prices
        BigDecimal sma20 = analysis.getSma20();
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        
        for (int i = size - 20; i < size; i++) {
            BigDecimal diff = closePrices.get(i).subtract(sma20);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(20), 8, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        
        // Calculate Bollinger Bands (SMA20 ± 2 * standard deviation)
        BigDecimal bollingerUpper = sma20.add(stdDev.multiply(BigDecimal.valueOf(2)));
        BigDecimal bollingerLower = sma20.subtract(stdDev.multiply(BigDecimal.valueOf(2)));
        
        analysis.setBollingerUpper(bollingerUpper);
        analysis.setBollingerLower(bollingerLower);
    }
    
    /**
     * Calculate volume-based indicators
     */
    private void calculateVolumeIndicators(KlineAnalysis analysis, List<BigDecimal> closePrices, List<BigDecimal> volumes) {
        int size = volumes.size();
        
        // Volume SMA5
        if (size >= 5) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = size - 5; i < size; i++) {
                sum = sum.add(volumes.get(i));
            }
            BigDecimal volumeSMA5 = sum.divide(BigDecimal.valueOf(5), 8, RoundingMode.HALF_UP);
            analysis.setVolumeSMA5(volumeSMA5);
        }
        
        // On Balance Volume (OBV)
        if (size >= 2) {
            BigDecimal obv = volumes.get(0);
            for (int i = 1; i < size; i++) {
                if (closePrices.get(i).compareTo(closePrices.get(i - 1)) > 0) {
                    // Price up, add volume
                    obv = obv.add(volumes.get(i));
                } else if (closePrices.get(i).compareTo(closePrices.get(i - 1)) < 0) {
                    // Price down, subtract volume
                    obv = obv.subtract(volumes.get(i));
                }
                // If price unchanged, OBV remains the same
            }
            analysis.setObv(obv);
        }
    }
    
    /**
     * Identify common price patterns
     */
    private void identifyPricePatterns(KlineAnalysis analysis, List<Kline> klines) {
        int size = klines.size();
        if (size < 3) {
            return;
        }
        
        // Get the most recent candles for pattern recognition
        Kline lastCandle = klines.get(size - 1);
        Kline prevCandle = klines.get(size - 2);
        
        // Doji pattern (open and close prices are almost equal)
        BigDecimal dojiThreshold = lastCandle.getHigh().subtract(lastCandle.getLow())
                .multiply(BigDecimal.valueOf(0.1));
        BigDecimal openCloseAbs = lastCandle.getOpen().subtract(lastCandle.getClose()).abs();
        analysis.setDoji(openCloseAbs.compareTo(dojiThreshold) <= 0);
        
        // Hammer pattern
        // Lower shadow should be at least twice the body
        boolean isHammer = false;
        if (lastCandle.getClose().compareTo(lastCandle.getOpen()) > 0) { // Bullish candle
            BigDecimal body = lastCandle.getClose().subtract(lastCandle.getOpen());
            BigDecimal lowerShadow = lastCandle.getOpen().subtract(lastCandle.getLow());
            BigDecimal upperShadow = lastCandle.getHigh().subtract(lastCandle.getClose());
            
            isHammer = lowerShadow.compareTo(body.multiply(BigDecimal.valueOf(2))) > 0 
                    && upperShadow.compareTo(body.multiply(BigDecimal.valueOf(0.5))) < 0;
        } else { // Bearish candle
            BigDecimal body = lastCandle.getOpen().subtract(lastCandle.getClose());
            BigDecimal lowerShadow = lastCandle.getClose().subtract(lastCandle.getLow());
            BigDecimal upperShadow = lastCandle.getHigh().subtract(lastCandle.getOpen());
            
            isHammer = lowerShadow.compareTo(body.multiply(BigDecimal.valueOf(2))) > 0 
                    && upperShadow.compareTo(body.multiply(BigDecimal.valueOf(0.5))) < 0;
        }
        analysis.setHammerPattern(isHammer);
        
        // Bullish/Bearish Engulfing
        boolean isBullishEngulfing = lastCandle.getClose().compareTo(lastCandle.getOpen()) > 0 // Current bullish
                && prevCandle.getClose().compareTo(prevCandle.getOpen()) < 0 // Previous bearish
                && lastCandle.getOpen().compareTo(prevCandle.getClose()) < 0 // Current open < prev close
                && lastCandle.getClose().compareTo(prevCandle.getOpen()) > 0; // Current close > prev open
                
        boolean isBearishEngulfing = lastCandle.getClose().compareTo(lastCandle.getOpen()) < 0 // Current bearish
                && prevCandle.getClose().compareTo(prevCandle.getOpen()) > 0 // Previous bullish
                && lastCandle.getOpen().compareTo(prevCandle.getClose()) > 0 // Current open > prev close
                && lastCandle.getClose().compareTo(prevCandle.getOpen()) < 0; // Current close < prev open
        
        analysis.setEngulfingPattern(isBullishEngulfing || isBearishEngulfing);
    }
    
    /**
     * Find potential support and resistance levels
     */
    private void findSupportResistanceLevels(KlineAnalysis analysis, List<Kline> klines) {
        // Simple pivot points based on recent highs and lows
        List<BigDecimal> highs = klines.stream()
                .map(Kline::getHigh)
                .collect(Collectors.toList());
                
        List<BigDecimal> lows = klines.stream()
                .map(Kline::getLow)
                .collect(Collectors.toList());
        
        // Find local maxima and minima
        List<BigDecimal> resistanceLevels = new ArrayList<>();
        List<BigDecimal> supportLevels = new ArrayList<>();
        
        for (int i = 5; i < klines.size() - 5; i++) {
            // Check if this candle's high is a local maximum
            if (isLocalMaximum(highs, i, 5)) {
                resistanceLevels.add(klines.get(i).getHigh());
            }
            
            // Check if this candle's low is a local minimum
            if (isLocalMinimum(lows, i, 5)) {
                supportLevels.add(klines.get(i).getLow());
            }
        }
        
        // Limit to 3 most significant levels
        resistanceLevels = mergeSimilarLevels(resistanceLevels);
        supportLevels = mergeSimilarLevels(supportLevels);
        
        analysis.setResistanceLevels(resistanceLevels);
        analysis.setSupportLevels(supportLevels);
    }
    
    /**
     * Check if a price is a local maximum
     */
    private boolean isLocalMaximum(List<BigDecimal> prices, int index, int window) {
        BigDecimal value = prices.get(index);
        for (int i = Math.max(0, index - window); i < Math.min(prices.size(), index + window + 1); i++) {
            if (i != index && prices.get(i).compareTo(value) > 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if a price is a local minimum
     */
    private boolean isLocalMinimum(List<BigDecimal> prices, int index, int window) {
        BigDecimal value = prices.get(index);
        for (int i = Math.max(0, index - window); i < Math.min(prices.size(), index + window + 1); i++) {
            if (i != index && prices.get(i).compareTo(value) < 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Merge price levels that are close to each other
     */
    private List<BigDecimal> mergeSimilarLevels(List<BigDecimal> levels) {
        if (levels.size() <= 3) {
            return levels;
        }
        
        // Sort the levels
        Collections.sort(levels);
        
        // Merge levels that are within 0.5% of each other
        List<BigDecimal> mergedLevels = new ArrayList<>();
        BigDecimal currentLevel = levels.get(0);
        BigDecimal sum = currentLevel;
        int count = 1;
        
        for (int i = 1; i < levels.size(); i++) {
            BigDecimal nextLevel = levels.get(i);
            BigDecimal percentDiff = nextLevel.subtract(currentLevel)
                    .divide(currentLevel, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).abs();
            
            if (percentDiff.compareTo(BigDecimal.valueOf(0.5)) <= 0) {
                // Merge with current level
                sum = sum.add(nextLevel);
                count++;
            } else {
                // Add average of current cluster and start a new one
                mergedLevels.add(sum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP));
                currentLevel = nextLevel;
                sum = currentLevel;
                count = 1;
            }
        }
        
        // Add the last cluster
        mergedLevels.add(sum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP));
        
        // Return at most 3 levels
        return mergedLevels.subList(0, Math.min(3, mergedLevels.size()));
    }
    
    /**
     * Determine overall market trend
     */
    private void determineTrend(KlineAnalysis analysis, List<BigDecimal> closePrices, List<Kline> klines) {
        int size = closePrices.size();
        if (size < 20 || analysis.getEma14() == null || analysis.getSma20() == null) {
            return;
        }
        
        // Check EMA14 vs SMA20 for trend direction
        boolean emaAboveSma = analysis.getEma14().compareTo(analysis.getSma20()) > 0;
        
        // Check last 10 candles for trend consistency
        int bullishCandles = 0;
        int bearishCandles = 0;
        
        for (int i = size - 10; i < size; i++) {
            if (klines.get(i).getClose().compareTo(klines.get(i).getOpen()) > 0) {
                bullishCandles++;
            } else if (klines.get(i).getClose().compareTo(klines.get(i).getOpen()) < 0) {
                bearishCandles++;
            }
        }
        
        // Calculate RSI trend
        boolean rsiOverbought = analysis.getRsi14() != null && analysis.getRsi14().compareTo(BigDecimal.valueOf(70)) > 0;
        boolean rsiOversold = analysis.getRsi14() != null && analysis.getRsi14().compareTo(BigDecimal.valueOf(30)) < 0;
        
        // Determine overall trend
        String trend;
        if (emaAboveSma && bullishCandles >= 6) {
            trend = rsiOverbought ? "BULLISH_OVERBOUGHT" : "BULLISH";
        } else if (!emaAboveSma && bearishCandles >= 6) {
            trend = rsiOversold ? "BEARISH_OVERSOLD" : "BEARISH";
        } else {
            trend = "NEUTRAL";
        }
        
        analysis.setOverallTrend(trend);
        
        // Calculate trend strength (0-100)
        int trendScore = 50; // Start neutral
        
        // Add/subtract based on EMA vs SMA
        trendScore += emaAboveSma ? 15 : -15;
        
        // Add/subtract based on candle ratio
        trendScore += (bullishCandles - bearishCandles) * 3;
        
        // Adjust for RSI
        if (rsiOverbought) {
            trendScore = Math.min(100, trendScore + 10);
        } else if (rsiOversold) {
            trendScore = Math.max(0, trendScore - 10);
        }
        
        // Adjust for MACD
        if (analysis.getMacd() != null && analysis.getMacdSignal() != null) {
            if (analysis.getMacd().compareTo(analysis.getMacdSignal()) > 0) {
                trendScore += 10;
            } else {
                trendScore -= 10;
            }
        }
        
        // Ensure score is between 0-100
        trendScore = Math.max(0, Math.min(100, trendScore));
        
        analysis.setTrendStrength(BigDecimal.valueOf(trendScore));
    }
} 