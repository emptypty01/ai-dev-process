package local.aiworkflow.product;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductRequestTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ProductService service;

    @Test
    void acceptsMaximumLengthsAfterTrimmingForCreateAndUpdate() throws Exception {
        String type = "类".repeat(40);
        String name = "名".repeat(80);
        Product product = new Product(1L, type, name, new BigDecimal("1.00"), true, Instant.EPOCH, Instant.EPOCH);
        when(service.create(any())).thenReturn(product);
        when(service.update(eq(1L), any())).thenReturn(product);
        String body = "{\"type\":\" " + type + " \",\"name\":\" " + name + " \",\"price\":\"1.00\"}";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(name));
        mvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value(type));
    }

    @Test
    void rejectsInvalidProductAndMissingEnabledState() throws Exception {
        for (String body : List.of("{}", "{\"type\":\"办公\",\"name\":\"键盘\",\"price\":1.001}",
                "{\"type\":\"办公\",\"name\":\"键盘\",\"price\":0}")) {
            mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        }
        mvc.perform(put("/api/products/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
