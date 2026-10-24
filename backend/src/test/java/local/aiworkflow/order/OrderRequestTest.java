package local.aiworkflow.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderRequestTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderService service;

    @Test
    void rejectsFractionalQuantityBeforeBusinessInvocation() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"键盘\",\"quantity\":1.5,\"unitPrice\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsIntegerQuantity() throws Exception {
        OrderItem order = new OrderItem(1L, "OD-request", "键盘", 1, new BigDecimal("10.00"), new BigDecimal("10.00"),
                "NEW", Instant.EPOCH);
        when(service.create(any())).thenReturn(order);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"键盘\",\"quantity\":1,\"unitPrice\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(1));
    }
}
