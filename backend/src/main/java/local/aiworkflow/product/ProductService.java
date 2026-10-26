package local.aiworkflow.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> list(Boolean enabled) {
        return repository.findAll(enabled);
    }

    @Transactional
    public Product create(SaveProductRequest request) {
        SaveProductRequest normalized = normalize(request);
        return repository.create(normalized.type(), normalized.name(), normalized.price());
    }

    @Transactional
    public Product update(long id, SaveProductRequest request) {
        SaveProductRequest normalized = normalize(request);
        requireProduct(id);
        repository.update(id, normalized.type(), normalized.name(), normalized.price());
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public Product setEnabled(long id, boolean enabled) {
        requireProduct(id);
        repository.setEnabled(id, enabled);
        return repository.findById(id).orElseThrow();
    }

    private void requireProduct(long id) {
        repository.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    private SaveProductRequest normalize(SaveProductRequest request) {
        String type = request.type() == null ? "" : request.type();
        String name = request.name() == null ? "" : request.name();
        BigDecimal price = request.price();
        if (type.isEmpty() || type.length() > 40 || name.isEmpty() || name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品类型为1–40字，名称为1–80字");
        }
        if (price == null || price.scale() > 2 || price.compareTo(new BigDecimal("0.01")) < 0
                || price.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "价格为0.01–99999999.99元，最多两位小数");
        }
        return new SaveProductRequest(type, name, price.setScale(2, RoundingMode.UNNECESSARY));
    }
}
