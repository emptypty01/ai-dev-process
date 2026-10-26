package local.aiworkflow.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void createsTrimmedProductWithTwoDecimalPrice() {
        Product product = new Product(1L, "办公设备", "键盘", new BigDecimal("9.00"), true, Instant.EPOCH, Instant.EPOCH);
        when(repository.create("办公设备", "键盘", new BigDecimal("9.00"))).thenReturn(product);
        assertThat(service.create(new SaveProductRequest(" 办公设备 ", " 键盘 ", new BigDecimal("9"))).enabled()).isTrue();
    }

    @Test
    void rejectsBlankFieldsAndInvalidPrices() {
        for (SaveProductRequest request : new SaveProductRequest[] {
                new SaveProductRequest(" ", "键盘", new BigDecimal("1.00")),
                new SaveProductRequest("办公", " ", new BigDecimal("1.00")),
                new SaveProductRequest("办公", "键盘", new BigDecimal("0")),
                new SaveProductRequest("办公", "键盘", new BigDecimal("1.001")),
                new SaveProductRequest("办公", "键盘", new BigDecimal("100000000"))}) {
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void changesEnabledStateWithoutRemovingProduct() {
        Product disabled = new Product(1L, "办公", "键盘", new BigDecimal("9.00"), false, Instant.EPOCH, Instant.EPOCH);
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(disabled));
        when(repository.findById(1)).thenReturn(Optional.of(disabled));
        assertThat(service.setEnabled(1, false).enabled()).isFalse();
        verify(repository).setEnabled(1, false);
    }
}
