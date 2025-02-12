package com.example.domain;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "order_book")
public class OrderBook {

  @Column(tag = true)
  private String symbol;

  // These lists could be stored as separate points in a more advanced design.
  // Here, we assume the order book snapshot is serialized as JSON or processed differently.
  // For demonstration, we leave them as plain lists.
  private List<Order> bids; // Buy orders
  private List<Order> asks; // Sell orders

  @Column(timestamp = true)
  private Instant timestamp;
}
