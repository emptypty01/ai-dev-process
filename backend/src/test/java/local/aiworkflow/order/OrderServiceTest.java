package local.aiworkflow.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import local.aiworkflow.product.Product;
import local.aiworkflow.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final OrderService service = new OrderService(repository, products);

    @Test
    void createsConfiguredSnapshotsWithExactTotal() {
        when(products.findByIdForUpdate(1)).thenReturn(Optional.of(product(1, "机械键盘", "299.00", true)));
        when(products.findByIdForUpdate(2)).thenReturn(Optional.of(product(2, "无线鼠标", "89.00", true)));
        when(repository.create(any(), any(), anyList())).thenReturn(1L);
        when(repository.findById(1)).thenReturn(Optional.of(order("NEW")));
        when(repository.findLines(1)).thenReturn(List.of());
        service.create(new CreateOrderRequest(List.of(new OrderLineRequest(2L, 1), new OrderLineRequest(1L, 2))));
        verify(repository).create(any(), eq(new BigDecimal("687.00")), eq(List.of(
                new OrderLine(1L, "机械键盘", "办公设备", new BigDecimal("299.00"), 2, new BigDecimal("598.00"), "CATALOG"),
                new OrderLine(2L, "无线鼠标", "办公设备", new BigDecimal("89.00"), 1, new BigDecimal("89.00"), "CATALOG"))));
    }

    @Test
    void mergesDuplicateProductsBeforePricing() {
        when(products.findByIdForUpdate(1)).thenReturn(Optional.of(product(1, "键盘", "0.10", true)));
        when(repository.create(any(), any(), anyList())).thenReturn(1L);
        when(repository.findById(1)).thenReturn(Optional.of(order("NEW")));
        when(repository.findLines(1)).thenReturn(List.of());
        service.create(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 1), new OrderLineRequest(1L, 2))));
        verify(repository).create(any(), eq(new BigDecimal("0.30")), eq(List.of(
                new OrderLine(1L, "键盘", "办公设备", new BigDecimal("0.10"), 3, new BigDecimal("0.30"), "CATALOG"))));
    }

    @Test
    void rejectsUnavailableProductsWithoutCreatingPartialOrder() {
        when(products.findByIdForUpdate(1)).thenReturn(Optional.of(product(1, "键盘", "1.00", true)));
        when(products.findByIdForUpdate(2)).thenReturn(Optional.of(product(2, "鼠标", "1.00", false)));
        assertBadRequest(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 1), new OrderLineRequest(2L, 1))));
        when(products.findByIdForUpdate(2)).thenReturn(Optional.empty());
        assertBadRequest(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 1), new OrderLineRequest(2L, 1))));
        verify(repository, never()).create(any(), any(), anyList());
    }

    @Test
    void rejectsEmptyAndInvalidOrCombinedQuantities() {
        assertBadRequest(new CreateOrderRequest(List.of()));
        assertBadRequest(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 0))));
        assertBadRequest(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 1001))));
        assertBadRequest(new CreateOrderRequest(List.of(new OrderLineRequest(1L, 1000), new OrderLineRequest(1L, 1))));
        verify(repository, never()).create(any(), any(), anyList());
    }

    @Test
    void cancelsNewOrder() {
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(order("NEW")));
        when(repository.findById(1)).thenReturn(Optional.of(order("CANCELLED")));
        assertThat(service.cancel(1).status()).isEqualTo("CANCELLED");
        verify(repository).cancel(1);
    }

    @Test
    void repeatedCancellationReturnsExistingResult() {
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(order("CANCELLED")));
        assertThat(service.cancel(1).status()).isEqualTo("CANCELLED");
        verify(repository, never()).cancel(1);
    }

    @Test
    void missingOrderReturnsNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(99)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void assertBadRequest(CreateOrderRequest request) {
        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Product product(long id, String name, String price, boolean enabled) {
        return new Product(id, "办公设备", name, new BigDecimal(price), enabled, Instant.EPOCH, Instant.EPOCH);
    }

    private OrderSummary order(String status) {
        return new OrderSummary(1L, "OD-demo", 2, 3, new BigDecimal("687.00"), status, Instant.EPOCH);
    }
}
