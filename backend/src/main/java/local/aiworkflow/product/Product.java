package local.aiworkflow.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record Product(Long id, String type, String name, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal price,
                      boolean enabled, Instant createdAt, Instant updatedAt) {
}
