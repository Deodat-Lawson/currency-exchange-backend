package com.example.service;

import com.influxdb.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

  private final InfluxDBClient influxDBClient;

  @Autowired
  public MarketDataService(InfluxDBClient influxDBClient) {
    this.influxDBClient = influxDBClient;
  }

  // Service methods that use influxDBClient...
}
