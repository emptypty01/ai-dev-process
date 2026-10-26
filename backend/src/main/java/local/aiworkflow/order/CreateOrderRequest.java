package local.aiworkflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(@NotEmpty(message = "请至少选择一个商品") List<@NotNull @Valid OrderLineRequest> items) {
}
