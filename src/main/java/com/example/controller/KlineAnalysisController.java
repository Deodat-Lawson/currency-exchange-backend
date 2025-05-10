package com.example.controller;

import com.example.domain.KlineAnalysis;
import com.example.service.CurrencyMarketDataService;
import com.example.service.KlineAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class KlineAnalysisController {

    private final KlineAnalysisService analysisService;
    private final CurrencyMarketDataService marketDataService;

    @Autowired
    public KlineAnalysisController(KlineAnalysisService analysisService,
                                  CurrencyMarketDataService marketDataService) {
        this.analysisService = analysisService;
        this.marketDataService = marketDataService;
    }

    /**
     * Performs technical analysis on kline data for a specific symbol
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<KlineAnalysis> getAnalysis(@PathVariable String symbol) {
        KlineAnalysis analysis = analysisService.analyzeKlines(symbol);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analysis);
    }

    /**
     * Performs technical analysis on all available symbols
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, KlineAnalysis>> getAllAnalysis() {
        List<String> symbols = marketDataService.getAllSymbols();
        Map<String, KlineAnalysis> allAnalyses = new HashMap<>();
        
        for (String symbol : symbols) {
            KlineAnalysis analysis = analysisService.analyzeKlines(symbol);
            if (analysis != null) {
                allAnalyses.put(symbol, analysis);
            }
        }
        
        if (allAnalyses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(allAnalyses);
    }
    
    /**
     * Get market analysis summary - returns only symbols with strong trends
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Map<String, Object>>> getAnalysisSummary() {
        List<String> symbols = marketDataService.getAllSymbols();
        Map<String, Map<String, Object>> summary = new HashMap<>();
        
        for (String symbol : symbols) {
            KlineAnalysis analysis = analysisService.analyzeKlines(symbol);
            if (analysis != null) {
                // Only include symbols with strong trends (strength > 65)
                if (analysis.getTrendStrength() != null && 
                        analysis.getTrendStrength().intValue() > 65) {
                    
                    Map<String, Object> symbolSummary = new HashMap<>();
                    symbolSummary.put("trend", analysis.getOverallTrend());
                    symbolSummary.put("strength", analysis.getTrendStrength());
                    symbolSummary.put("rsi", analysis.getRsi14());
                    symbolSummary.put("patterns", getActivePatterns(analysis));
                    
                    summary.put(symbol, symbolSummary);
                }
            }
        }
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Helper method to get active chart patterns
     */
    private List<String> getActivePatterns(KlineAnalysis analysis) {
        List<String> patterns = new java.util.ArrayList<>();
        
        if (analysis.isHammerPattern()) {
            patterns.add("HAMMER");
        }
        
        if (analysis.isEngulfingPattern()) {
            patterns.add("ENGULFING");
        }
        
        if (analysis.isDoji()) {
            patterns.add("DOJI");
        }
        
        return patterns;
    }
} 