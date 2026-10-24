package local.aiworkflow.order;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private static final RowMapper<OrderItem> MAPPER = (rs, rowNum) -> new OrderItem(
            rs.getLong("id"), rs.getString("order_no"), rs.getString("item_name"), rs.getInt("quantity"),
            rs.getBigDecimal("unit_price"), rs.getBigDecimal("total_amount"), rs.getString("status"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OrderItem> findAll() {
        return jdbc.query("SELECT * FROM demo_order ORDER BY id DESC LIMIT 100", MAPPER);
    }

    public Optional<OrderItem> findById(long id) {
        return jdbc.query("SELECT * FROM demo_order WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<OrderItem> findByIdForUpdate(long id) {
        return jdbc.query("SELECT * FROM demo_order WHERE id = ? FOR UPDATE", MAPPER, id).stream().findFirst();
    }

    public OrderItem create(String orderNo, String itemName, int quantity, BigDecimal unitPrice, BigDecimal totalAmount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO demo_order (order_no, item_name, quantity, unit_price, total_amount) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, orderNo);
            statement.setString(2, itemName);
            statement.setInt(3, quantity);
            statement.setBigDecimal(4, unitPrice);
            statement.setBigDecimal(5, totalAmount);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) {
            throw new IllegalStateException("数据库未返回订单 ID");
        }
        return findById(id.longValue()).orElseThrow();
    }

    public void cancel(long id) {
        jdbc.update("UPDATE demo_order SET status = 'CANCELLED' WHERE id = ? AND status = 'NEW'", id);
    }
}
