package local.aiworkflow.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public List<OrderItem> list() {
        return repository.findAll();
    }

    @Transactional
    public OrderItem create(CreateOrderRequest request) {
        String itemName = request.itemName() == null ? "" : request.itemName().strip();
        BigDecimal price = request.unitPrice();
        if (itemName.isEmpty() || itemName.length() > 80 || request.quantity() < 1 || request.quantity() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品名称为 1–80 字，数量为 1–1000");
        }
        if (price == null || price.compareTo(new BigDecimal("0.01")) < 0
                || price.compareTo(new BigDecimal("99999999.99")) > 0 || price.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单价为 0.01–99999999.99，最多两位小数");
        }
        BigDecimal unitPrice = price.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return repository.create("OD-" + UUID.randomUUID(), itemName, request.quantity(), unitPrice, total);
    }

    @Transactional
    public OrderItem cancel(long id) {
        OrderItem order = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if ("CANCELLED".equals(order.status())) {
            return order;
        }
        repository.cancel(id);
        return repository.findById(id).orElseThrow();
    }
}
