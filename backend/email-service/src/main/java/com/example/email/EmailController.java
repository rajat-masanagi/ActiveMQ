package com.example.email;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @CrossOrigin(origins = "http://localhost:5173")
public class EmailController {
  private final OrderEmailListener listener;
  public EmailController(OrderEmailListener listener) { this.listener = listener; }
  @GetMapping("/emails") public List<Map<String, Object>> emails() { return listener.findAll(); }
}
