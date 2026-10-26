package local.aiworkflow.order;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "order-demo");
    }

    @GetMapping("/orders")
    public List<OrderSummary> list() {
        return service.list();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetail create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderSummary cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    @GetMapping("/orders/{id}")
    public OrderDetail detail(@PathVariable long id) {
        return service.detail(id);
    }
}
