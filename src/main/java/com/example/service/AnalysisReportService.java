package com.example.service;

import com.example.domain.AnalysisReport;
import com.example.domain.CurrentPrice;
import com.example.domain.KlineAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisReportService {

    private final KlineAnalysisService analysisService;
    private final CurrencyMarketDataService marketDataService;
    
    private AnalysisReport lastReport;
    private Map<String, List<BigDecimal>> historicalPrices = new ConcurrentHashMap<>();

    @Autowired
    public AnalysisReportService(KlineAnalysisService analysisService,
                                CurrencyMarketDataService marketDataService) {
        this.analysisService = analysisService;
        this.marketDataService = marketDataService;
    }

    /**
     * Generate a comprehensive analysis report
     */
    @Cacheable(value = "analysisReport")
    public AnalysisReport generateReport() {
        if (lastReport != null && 
                Instant.now().minusSeconds(300).isBefore(lastReport.getReportTime())) {
            // Return cached report if less than 5 minutes old
            return lastReport;
        }
        
        List<String> symbols = marketDataService.getAllSymbols();
        if (symbols.isEmpty()) {
            log.warn("No symbols available for analysis");
            return new AnalysisReport();
        }
        
        // Collect analysis for all symbols
        Map<String, KlineAnalysis> allAnalyses = new HashMap<>();
        for (String symbol : symbols) {
            KlineAnalysis analysis = analysisService.analyzeKlines(symbol);
            if (analysis != null) {
                allAnalyses.put(symbol, analysis);
                updateHistoricalPrices(symbol);
            }
        }
        
        // Create report
        AnalysisReport report = new AnalysisReport();
        report.setReportTime(Instant.now());
        report.setTotalSymbols(allAnalyses.size());
        
        // Count trend types
        countTrendTypes(report, allAnalyses);
        
        // Find top performers
        findTopPerformers(report, allAnalyses);
        
        // Find significant patterns
        findSignificantPatterns(report, allAnalyses);
        
        // Calculate correlation matrix if we have enough data
        if (historicalPrices.size() >= 2) {
            report.setCorrelationMatrix(calculateCorrelationMatrix());
        }
        
        lastReport = report;
        return report;
    }
    
    /**
     * Update historical prices for correlation calculation
     */
    private void updateHistoricalPrices(String symbol) {
        CurrentPrice price = marketDataService.getCurrentPrice(symbol);
        if (price == null || price.getPrice() == null) {
            return;
        }
        
        List<BigDecimal> prices = historicalPrices.getOrDefault(symbol, new ArrayList<>());
        
        // Keep only last 100 prices for each symbol
        if (prices.size() >= 100) {
            prices.remove(0);
        }
        
        prices.add(price.getPrice());
        historicalPrices.put(symbol, prices);
    }
    
    /**
     * Count the number of symbols in each trend category
     */
    private void countTrendTypes(AnalysisReport report, Map<String, KlineAnalysis> allAnalyses) {
        int bullish = 0;
        int bearish = 0;
        int neutral = 0;
        
        for (KlineAnalysis analysis : allAnalyses.values()) {
            if (analysis.getOverallTrend() == null) {
                continue;
            }
            
            String trend = analysis.getOverallTrend();
            if (trend.contains("BULLISH")) {
                bullish++;
            } else if (trend.contains("BEARISH")) {
                bearish++;
            } else {
                neutral++;
            }
        }
        
        report.setBullishSymbols(bullish);
        report.setBearishSymbols(bearish);
        report.setNeutralSymbols(neutral);
    }
    
    /**
     * Find top bullish and bearish performers based on trend strength
     */
    private void findTopPerformers(AnalysisReport report, Map<String, KlineAnalysis> allAnalyses) {
        List<AnalysisReport.SymbolTrend> bullishTrends = new ArrayList<>();
        List<AnalysisReport.SymbolTrend> bearishTrends = new ArrayList<>();
        
        for (Map.Entry<String, KlineAnalysis> entry : allAnalyses.entrySet()) {
            String symbol = entry.getKey();
            KlineAnalysis analysis = entry.getValue();
            
            if (analysis.getOverallTrend() == null || analysis.getTrendStrength() == null) {
                continue;
            }
            
            AnalysisReport.SymbolTrend trend = new AnalysisReport.SymbolTrend(
                symbol,
                analysis.getOverallTrend(),
                analysis.getTrendStrength(),
                analysis.getRsi14()
            );
            
            if (analysis.getOverallTrend().contains("BULLISH")) {
                bullishTrends.add(trend);
            } else if (analysis.getOverallTrend().contains("BEARISH")) {
                bearishTrends.add(trend);
            }
        }
        
        // Sort by trend strength (descending)
        bullishTrends.sort((a, b) -> b.getStrength().compareTo(a.getStrength()));
        bearishTrends.sort((a, b) -> b.getStrength().compareTo(a.getStrength()));
        
        // Take top 5 or fewer
        report.setTopBullish(bullishTrends.stream().limit(5).collect(Collectors.toList()));
        report.setTopBearish(bearishTrends.stream().limit(5).collect(Collectors.toList()));
    }
    
    /**
     * Find symbols with significant chart patterns
     */
    private void findSignificantPatterns(AnalysisReport report, Map<String, KlineAnalysis> allAnalyses) {
        List<AnalysisReport.SymbolPattern> patterns = new ArrayList<>();
        
        for (Map.Entry<String, KlineAnalysis> entry : allAnalyses.entrySet()) {
            String symbol = entry.getKey();
            KlineAnalysis analysis = entry.getValue();
            
            if (analysis.getOverallTrend() == null) {
                continue;
            }
            
            // Check for significant patterns
            if (analysis.isHammerPattern()) {
                addPatternToReport(patterns, symbol, "HAMMER", analysis);
            }
            
            if (analysis.isEngulfingPattern()) {
                addPatternToReport(patterns, symbol, "ENGULFING", analysis);
            }
            
            if (analysis.isDoji() && 
                    (analysis.getOverallTrend().equals("BULLISH") || 
                     analysis.getOverallTrend().equals("BEARISH"))) {
                addPatternToReport(patterns, symbol, "DOJI", analysis);
            }
        }
        
        report.setSignificantPatterns(patterns);
    }
    
    /**
     * Helper method to add patterns to the report list
     */
    private void addPatternToReport(List<AnalysisReport.SymbolPattern> patterns, 
                                   String symbol, String patternName, 
                                   KlineAnalysis analysis) {
        CurrentPrice price = marketDataService.getCurrentPrice(symbol);
        if (price == null) {
            return;
        }
        
        patterns.add(new AnalysisReport.SymbolPattern(
            symbol,
            patternName,
            analysis.getOverallTrend(),
            price.getPrice()
        ));
    }
    
    /**
     * Calculate price correlation matrix between symbols
     */
    private Map<String, Map<String, BigDecimal>> calculateCorrelationMatrix() {
        // Only include symbols with enough price history (at least 30 points)
        List<String> validSymbols = historicalPrices.entrySet().stream()
                .filter(e -> e.getValue().size() >= 30)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        
        Map<String, Map<String, BigDecimal>> correlationMatrix = new HashMap<>();
        
        for (String symbolA : validSymbols) {
            Map<String, BigDecimal> correlations = new HashMap<>();
            correlationMatrix.put(symbolA, correlations);
            
            List<BigDecimal> pricesA = historicalPrices.get(symbolA);
            
            for (String symbolB : validSymbols) {
                // Skip self-correlation (always 1.0)
                if (symbolA.equals(symbolB)) {
                    correlations.put(symbolB, BigDecimal.ONE);
                    continue;
                }
                
                List<BigDecimal> pricesB = historicalPrices.get(symbolB);
                int minSize = Math.min(pricesA.size(), pricesB.size());
                
                // Calculate correlation using last minSize data points
                BigDecimal correlation = calculatePearsonCorrelation(
                        pricesA.subList(pricesA.size() - minSize, pricesA.size()),
                        pricesB.subList(pricesB.size() - minSize, pricesB.size())
                );
                
                correlations.put(symbolB, correlation);
            }
        }
        
        return correlationMatrix;
    }
    
    /**
     * Calculate Pearson correlation coefficient between two price lists
     */
    private BigDecimal calculatePearsonCorrelation(List<BigDecimal> pricesA, List<BigDecimal> pricesB) {
        int n = pricesA.size();
        if (n != pricesB.size() || n < 2) {
            return BigDecimal.ZERO;
        }
        
        // Calculate means
        BigDecimal sumA = pricesA.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumB = pricesB.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal meanA = sumA.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);
        BigDecimal meanB = sumB.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);
        
        // Calculate covariance and variances
        BigDecimal covariance = BigDecimal.ZERO;
        BigDecimal varianceA = BigDecimal.ZERO;
        BigDecimal varianceB = BigDecimal.ZERO;
        
        for (int i = 0; i < n; i++) {
            BigDecimal diffA = pricesA.get(i).subtract(meanA);
            BigDecimal diffB = pricesB.get(i).subtract(meanB);
            
            covariance = covariance.add(diffA.multiply(diffB));
            varianceA = varianceA.add(diffA.multiply(diffA));
            varianceB = varianceB.add(diffB.multiply(diffB));
        }
        
        // Calculate correlation
        if (varianceA.compareTo(BigDecimal.ZERO) == 0 || varianceB.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal correlation = covariance.divide(
                varianceA.sqrt(new java.math.MathContext(10))
                        .multiply(varianceB.sqrt(new java.math.MathContext(10))),
                10, RoundingMode.HALF_UP);
        
        return correlation;
    }
    
    /**
     * Generate and cache a new report every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void scheduleReportGeneration() {
        log.info("Generating scheduled analysis report");
        generateReport();
    }
} 