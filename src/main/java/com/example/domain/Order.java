package com.example.domain;

import com.influxdb.annotations.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  @Column
  private BigDecimal price;

  @Column
  private BigDecimal quantity;
}
