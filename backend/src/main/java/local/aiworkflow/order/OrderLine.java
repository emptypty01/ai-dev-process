package local.aiworkflow.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record OrderLine(Long productId, String productName, String productType,
                        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal unitPrice, int quantity,
                        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal lineTotal, String source) {
}
