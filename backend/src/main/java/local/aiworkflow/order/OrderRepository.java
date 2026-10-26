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
    private static final String SUMMARY_SQL = """
            SELECT o.*, (SELECT COUNT(*) FROM demo_order_line l WHERE l.order_id = o.id) AS product_count,
                (SELECT COALESCE(SUM(l.quantity), 0) FROM demo_order_line l WHERE l.order_id = o.id) AS total_quantity
            FROM demo_order o
            """;
    private static final RowMapper<OrderSummary> MAPPER = (rs, rowNum) -> new OrderSummary(rs.getLong("id"),
            rs.getString("order_no"), rs.getInt("product_count"), rs.getLong("total_quantity"),
            rs.getBigDecimal("total_amount"), rs.getString("status"), rs.getTimestamp("created_at").toInstant());
    private static final RowMapper<OrderLine> LINE_MAPPER = (rs, rowNum) -> new OrderLine((Long) rs.getObject("product_id"),
            rs.getString("product_name"), rs.getString("product_type"), rs.getBigDecimal("unit_price"),
            rs.getInt("quantity"), rs.getBigDecimal("line_total"), rs.getString("source"));
    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OrderSummary> findAll() {
        return jdbc.query(SUMMARY_SQL + " ORDER BY o.id DESC LIMIT 100", MAPPER);
    }

    public Optional<OrderSummary> findById(long id) {
        return jdbc.query(SUMMARY_SQL + " WHERE o.id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<OrderSummary> findByIdForUpdate(long id) {
        return jdbc.query(SUMMARY_SQL + " WHERE o.id = ? FOR UPDATE", MAPPER, id).stream().findFirst();
    }

    public List<OrderLine> findLines(long id) {
        return jdbc.query("SELECT * FROM demo_order_line WHERE order_id = ? ORDER BY id", LINE_MAPPER, id);
    }

    public long create(String orderNo, BigDecimal totalAmount, List<OrderLine> lines) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO demo_order (order_no, total_amount) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, orderNo);
            statement.setBigDecimal(2, totalAmount);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) throw new IllegalStateException("数据库未返回订单ID");
        jdbc.batchUpdate("""
                INSERT INTO demo_order_line (order_id, product_id, product_name, product_type, unit_price, quantity, line_total, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, lines, lines.size(), (statement, line) -> {
                    statement.setLong(1, id.longValue());
                    statement.setLong(2, line.productId());
                    statement.setString(3, line.productName());
                    statement.setString(4, line.productType());
                    statement.setBigDecimal(5, line.unitPrice());
                    statement.setInt(6, line.quantity());
                    statement.setBigDecimal(7, line.lineTotal());
                    statement.setString(8, line.source());
                });
        return id.longValue();
    }

    public void cancel(long id) {
        jdbc.update("UPDATE demo_order SET status = 'CANCELLED' WHERE id = ? AND status = 'NEW'", id);
    }
}
