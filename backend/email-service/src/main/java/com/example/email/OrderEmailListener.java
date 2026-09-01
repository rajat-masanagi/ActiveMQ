package com.example.email;
import java.util.Map;
import org.slf4j.*;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
@Component
public class OrderEmailListener {
  private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);
  @JmsListener(destination = "order.created")
  public void sendEmail(Map<String,Object> event) {
    log.info("\n===== SIMULATED EMAIL =====\nTo: {}\nSubject: Order {} received\nBody: Thanks for ordering {} x {}.\n===========================", event.get("customerEmail"), event.get("orderId"), event.get("quantity"), event.get("product"));
  }
}
