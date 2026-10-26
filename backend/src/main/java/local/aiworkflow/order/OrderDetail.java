package local.aiworkflow.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetail(Long id, String orderNo, int productCount, long totalQuantity,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount, String status,
                          Instant createdAt, List<OrderLine> items) {
    public static OrderDetail from(OrderSummary order, List<OrderLine> items) {
        return new OrderDetail(order.id(), order.orderNo(), order.productCount(), order.totalQuantity(), order.totalAmount(),
                order.status(), order.createdAt(), List.copyOf(items));
    }
}
