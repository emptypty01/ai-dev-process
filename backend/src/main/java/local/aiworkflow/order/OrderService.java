package local.aiworkflow.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import local.aiworkflow.product.Product;
import local.aiworkflow.product.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999.99");
    private final OrderRepository repository;
    private final ProductRepository products;

    public OrderService(OrderRepository repository, ProductRepository products) {
        this.repository = repository;
        this.products = products;
    }

    public List<OrderSummary> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(long id) {
        OrderSummary order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return OrderDetail.from(order, repository.findLines(id));
    }

    @Transactional
    public OrderDetail create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = normalizeItems(request);
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = new BigDecimal("0.00");
        for (var entry : quantities.entrySet()) {
            Product product = products.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在，请刷新商品列表"));
            if (!product.enabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品“" + product.name() + "”已停用，请移除或刷新后重试");
            }
            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(entry.getValue()));
            total = total.add(lineTotal);
            if (total.compareTo(MAX_AMOUNT) > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单金额超过上限");
            lines.add(new OrderLine(product.id(), product.name(), product.type(), product.price(), entry.getValue(), lineTotal, "CATALOG"));
        }
        long id = repository.create("OD-" + UUID.randomUUID(), total, lines);
        return detail(id);
    }

    @Transactional
    public OrderSummary cancel(long id) {
        OrderSummary order = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if ("CANCELLED".equals(order.status())) return order;
        repository.cancel(id);
        return repository.findById(id).orElseThrow();
    }

    private Map<Long, Integer> normalizeItems(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请使用items选择至少一个商品并填写数量");
        }
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderLineRequest item : request.items()) {
            if (item == null || item.productId() == null || item.productId() < 1 || item.quantity() == null
                    || item.quantity() < 1 || item.quantity() > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效商品，数量须为1–1000的整数");
            }
            int combined = quantities.getOrDefault(item.productId(), 0) + item.quantity();
            if (combined > 1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "同一商品合计数量不能超过1000");
            quantities.put(item.productId(), combined);
        }
        return quantities;
    }
}
