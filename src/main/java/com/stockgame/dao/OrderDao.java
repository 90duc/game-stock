package com.stockgame.dao;

import com.stockgame.model.Order;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {
    
    public List<Order> getPendingOrdersByStock(Long stockId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE stock_id = ? AND status = 'PENDING' ORDER BY created_at";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSet(rs));
            }
        }
        return orders;
    }
    
    // 通过会话ID获取待处理订单
    public List<Order> getPendingOrdersBySession(Long gameSessionId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE game_session_id = ? AND status = 'PENDING' ORDER BY created_at";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, gameSessionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSet(rs));
            }
        }
        return orders;
    }
    
    public List<Order> getByUserId(Long userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSet(rs));
            }
        }
        return orders;
    }
    
    public List<Order> getByUserAndStock(Long userId, Long stockId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? AND stock_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSet(rs));
            }
        }
        return orders;
    }
    
    public Order getById(Long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }
    
    public void save(Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, stock_id, order_type, price_type, quantity, " +
                "price, status, filled_quantity, game_session_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, order.getUserId());
            stmt.setLong(2, order.getStockId());
            stmt.setString(3, order.getOrderType().name());
            stmt.setString(4, order.getPriceType().name());
            stmt.setInt(5, order.getQuantity());
            stmt.setBigDecimal(6, order.getPrice());
            stmt.setString(7, order.getStatus().name());
            stmt.setInt(8, order.getFilledQuantity());
            if (order.getGameSessionId() != null) {
                stmt.setLong(9, order.getGameSessionId());
            } else {
                stmt.setNull(9, Types.BIGINT);
            }
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                order.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void update(Order order) throws SQLException {
        String sql = "UPDATE orders SET status = ?, filled_quantity = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.getStatus().name());
            stmt.setInt(2, order.getFilledQuantity());
            stmt.setLong(3, order.getId());
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void cancelOrder(Long orderId) throws SQLException {
        String sql = "UPDATE orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void cancelAllPendingOrders(Long gameSessionId) throws SQLException {
        String sql = "UPDATE orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE game_session_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, gameSessionId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public BigDecimal getFrozenBalanceByUser(Long userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(price * (quantity - filled_quantity)), 0) as frozen " +
                "FROM orders WHERE user_id = ? AND order_type = 'BUY' AND status = 'PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("frozen");
            }
        }
        return BigDecimal.ZERO;
    }
    
    // 获取用户指定股票的待卖出冻结数量
    public int getFrozenSellQuantity(Long userId, Long stockId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity - filled_quantity), 0) as frozen " +
                "FROM orders WHERE user_id = ? AND stock_id = ? AND order_type = 'SELL' AND status = 'PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("frozen");
            }
        }
        return 0;
    }
    
    private Order mapResultSet(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setUserId(rs.getLong("user_id"));
        order.setStockId(rs.getLong("stock_id"));
        order.setOrderType(Order.OrderType.valueOf(rs.getString("order_type")));
        order.setPriceType(Order.OrderPriceType.valueOf(rs.getString("price_type")));
        order.setQuantity(rs.getInt("quantity"));
        order.setPrice(rs.getBigDecimal("price"));
        order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
        order.setFilledQuantity(rs.getInt("filled_quantity"));
        
        Long gameSessionId = rs.getLong("game_session_id");
        if (!rs.wasNull()) {
            order.setGameSessionId(gameSessionId);
        }
        
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return order;
    }
}
