package local.aiworkflow.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SaveProductRequest(
        @NotBlank(message = "商品类型不能为空") @Size(max = 40, message = "商品类型最多40字") String type,
        @NotBlank(message = "商品名称不能为空") @Size(max = 80, message = "商品名称最多80字") String name,
        @NotNull(message = "商品价格不能为空") @DecimalMin(value = "0.01", message = "商品价格至少0.01元")
        @DecimalMax(value = "99999999.99", message = "商品价格超过上限")
        @Digits(integer = 8, fraction = 2, message = "商品价格最多两位小数") BigDecimal price) {
    public SaveProductRequest {
        type = type == null ? null : type.strip();
        name = name == null ? null : name.strip();
    }
}
