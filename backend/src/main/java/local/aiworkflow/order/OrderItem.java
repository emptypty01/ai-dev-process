package local.aiworkflow.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItem(Long id, String orderNo, String itemName, int quantity, BigDecimal unitPrice,
                        BigDecimal totalAmount, String status, Instant createdAt) {
}
