package local.aiworkflow.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(Long id, String orderNo, int productCount, long totalQuantity,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount, String status, Instant createdAt) {
}
