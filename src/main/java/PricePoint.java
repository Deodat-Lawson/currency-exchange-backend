import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import java.time.Instant;

@Measurement(name = "crypto_price")
public class PricePoint {
  @Column(tag = true)
  String symbol;           // e.g., "BTCUSD"

  @Column
  Double price;            // e.g., 45000.5

  @Column(timestamp = true)
  Instant time;            // timestamp for the price
}
