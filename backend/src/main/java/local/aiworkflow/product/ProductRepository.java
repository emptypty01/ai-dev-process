package local.aiworkflow.product;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private static final RowMapper<Product> MAPPER = (rs, rowNum) -> new Product(rs.getLong("id"), rs.getString("type"),
            rs.getString("name"), rs.getBigDecimal("price"), rs.getBoolean("enabled"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll(Boolean enabled) {
        return enabled == null ? jdbc.query("SELECT * FROM demo_product ORDER BY id DESC", MAPPER)
                : jdbc.query("SELECT * FROM demo_product WHERE enabled = ? ORDER BY id DESC", MAPPER, enabled);
    }

    public Optional<Product> findById(long id) {
        return jdbc.query("SELECT * FROM demo_product WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Product> findByIdForUpdate(long id) {
        return jdbc.query("SELECT * FROM demo_product WHERE id = ? FOR UPDATE", MAPPER, id).stream().findFirst();
    }

    public Product create(String type, String name, BigDecimal price) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO demo_product (type, name, price) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, type);
            statement.setString(2, name);
            statement.setBigDecimal(3, price);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) throw new IllegalStateException("数据库未返回商品ID");
        return findById(id.longValue()).orElseThrow();
    }

    public void update(long id, String type, String name, BigDecimal price) {
        jdbc.update("UPDATE demo_product SET type = ?, name = ?, price = ? WHERE id = ?", type, name, price, id);
    }

    public void setEnabled(long id, boolean enabled) {
        jdbc.update("UPDATE demo_product SET enabled = ? WHERE id = ?", enabled, id);
    }
}
