package local.aiworkflow.product;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull(message = "请指定商品启用状态") Boolean enabled) {
}
