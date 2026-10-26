package local.aiworkflow.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(@NotNull(message = "请选择商品") @Positive(message = "商品ID不正确") Long productId,
                               @NotNull(message = "请填写数量") @Min(value = 1, message = "数量至少为1")
                               @Max(value = 1000, message = "单个商品数量最多1000") Integer quantity) {
}
