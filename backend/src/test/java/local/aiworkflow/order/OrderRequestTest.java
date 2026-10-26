package local.aiworkflow.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    void rejectsInvalidItemsAndClientPricesBeforeBusinessInvocation() throws Exception {
        for (String body : List.of("{}", "{\"items\":[]}", "{\"items\":[null]}",
                "{\"items\":[{\"productId\":1,\"quantity\":1.5}]}",
                "{\"items\":[{\"productId\":1,\"quantity\":0}]}",
                "{\"items\":[{\"productId\":1,\"quantity\":1,\"unitPrice\":0.01}]}",
                "{\"itemName\":\"键盘\",\"quantity\":1,\"unitPrice\":10.00}")) {
            mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        }
        verifyNoInteractions(service);
    }

    @Test
    void acceptsProductItemsAndSerializesMoneyAsDecimalStrings() throws Exception {
        when(service.create(any())).thenReturn(new OrderDetail(1L, "OD-request", 1, 2, new BigDecimal("598.00"),
                "NEW", Instant.EPOCH, List.of()));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value("598.00"))
                .andExpect(jsonPath("$.totalQuantity").value(2));
    }
}
