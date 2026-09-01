package com.example.email;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.*;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
@Component
public class OrderEmailListener {
  private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);
  private final List<Map<String, Object>> emails = new CopyOnWriteArrayList<>();
  @JmsListener(destination = "order.created")
  public void sendEmail(Map<String,Object> event) {
    emails.add(Map.of("orderId", event.get("orderId"), "customerEmail", event.get("customerEmail"), "product", event.get("product"), "quantity", event.get("quantity")));
    if (emails.size() > 20) emails.remove(0);
    log.info("\n===== SIMULATED EMAIL =====\nTo: {}\nSubject: Order {} received\nBody: Thanks for ordering {} x {}.\n===========================", event.get("customerEmail"), event.get("orderId"), event.get("quantity"), event.get("product"));
  }
  public List<Map<String, Object>> findAll() { return List.copyOf(emails); }
}
