package com.example.order;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
@Service
public class OrderService {
  private final List<Order> orders = new CopyOnWriteArrayList<>();
  private final JmsTemplate jmsTemplate;
  public OrderService(JmsTemplate jmsTemplate) { this.jmsTemplate = jmsTemplate; }
  public Order create(CreateOrderRequest request) {
    Order order = new Order(UUID.randomUUID(), request.customerEmail(), request.product(), request.quantity());
    jmsTemplate.convertAndSend("order.created", Map.of("orderId", order.id().toString(), "customerEmail", order.customerEmail(), "product", order.product(), "quantity", order.quantity()));
    orders.add(order); return order;
  }
  public List<Order> findAll() { return List.copyOf(orders); }
}
