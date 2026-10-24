package local.aiworkflow.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository);

    @Test
    void createsOrderWithExactDecimalAmountAndTrimmedName() {
        when(repository.create(any(), eq("机械键盘"), eq(3), eq(new BigDecimal("19.90")), eq(new BigDecimal("59.70"))))
                .thenReturn(order("NEW"));
        assertThat(service.create(new CreateOrderRequest("  机械键盘  ", 3, new BigDecimal("19.90"))).status()).isEqualTo("NEW");
    }

    @Test
    void rejectsBlankItemName() {
        assertBadRequest(new CreateOrderRequest("  ", 1, new BigDecimal("1.00")));
    }

    @Test
    void rejectsOutOfRangeQuantity() {
        assertBadRequest(new CreateOrderRequest("键盘", 0, new BigDecimal("1.00")));
        assertBadRequest(new CreateOrderRequest("键盘", 1001, new BigDecimal("1.00")));
    }

    @Test
    void rejectsInvalidPriceAndExcessivePrecision() {
        assertBadRequest(new CreateOrderRequest("键盘", 1, new BigDecimal("0.00")));
        assertBadRequest(new CreateOrderRequest("键盘", 1, new BigDecimal("1.001")));
        assertBadRequest(new CreateOrderRequest("键盘", 1, new BigDecimal("100000000")));
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
        when(repository.findByIdForUpdate(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(99)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void assertBadRequest(CreateOrderRequest request) {
        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private OrderItem order(String status) {
        return new OrderItem(1L, "OD-demo", "机械键盘", 3, new BigDecimal("19.90"), new BigDecimal("59.70"), status, Instant.EPOCH);
    }
}
