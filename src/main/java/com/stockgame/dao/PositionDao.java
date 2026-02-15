package com.stockgame.dao;

import com.stockgame.model.Position;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PositionDao {
    
    public List<Position> getByUserId(Long userId) throws SQLException {
        List<Position> positions = new ArrayList<>();
        String sql = "SELECT p.*, s.stock_code, s.stock_name, s.current_price " +
                "FROM positions p " +
                "JOIN stocks s ON p.stock_id = s.id " +
                "WHERE p.user_id = ? AND p.quantity > 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                positions.add(mapResultSet(rs));
            }
        }
        return positions;
    }
    
    public Position getByUserAndStock(Long userId, Long stockId) throws SQLException {
        String sql = "SELECT p.*, s.stock_code, s.stock_name, s.current_price " +
                "FROM positions p " +
                "JOIN stocks s ON p.stock_id = s.id " +
                "WHERE p.user_id = ? AND p.stock_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }
    
    public void save(Position position) throws SQLException {
        String sql = "INSERT INTO positions (user_id, stock_id, stock_code, stock_name, quantity, frozen_quantity, " +
                "average_cost, current_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, position.getUserId());
            stmt.setLong(2, position.getStockId());
            stmt.setString(3, position.getStockCode());
            stmt.setString(4, position.getStockName());
            stmt.setInt(5, position.getQuantity());
            stmt.setInt(6, position.getFrozenQuantity() != null ? position.getFrozenQuantity() : 0);
            stmt.setBigDecimal(7, position.getAverageCost());
            stmt.setBigDecimal(8, position.getCurrentValue());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                position.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void update(Position position) throws SQLException {
        String sql = "UPDATE positions SET quantity = ?, frozen_quantity = ?, average_cost = ?, current_value = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, position.getQuantity());
            stmt.setInt(2, position.getFrozenQuantity() != null ? position.getFrozenQuantity() : 0);
            stmt.setBigDecimal(3, position.getAverageCost());
            stmt.setBigDecimal(4, position.getCurrentValue());
            stmt.setLong(5, position.getId());
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void updateCurrentValue(Long positionId, BigDecimal currentPrice) throws SQLException {
        String sql = "UPDATE positions SET current_value = quantity * ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, currentPrice);
            stmt.setLong(2, positionId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void updateAllCurrentValues() throws SQLException {
        String sql = "UPDATE positions SET current_value = quantity * s.current_price " +
                "FROM stocks s WHERE positions.stock_id = s.id";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            conn.commit();
        }
    }
    
    private Position mapResultSet(ResultSet rs) throws SQLException {
        Position position = new Position();
        position.setId(rs.getLong("id"));
        position.setUserId(rs.getLong("user_id"));
        position.setStockId(rs.getLong("stock_id"));
        position.setStockCode(rs.getString("stock_code"));
        position.setStockName(rs.getString("stock_name"));
        position.setQuantity(rs.getInt("quantity"));
        position.setFrozenQuantity(rs.getInt("frozen_quantity"));
        position.setAverageCost(rs.getBigDecimal("average_cost"));
        
        BigDecimal currentPrice = rs.getBigDecimal("current_price");
        if (currentPrice != null) {
            position.setCurrentValue(currentPrice.multiply(BigDecimal.valueOf(position.getQuantity())));
        }
        
        position.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        position.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return position;
    }
}
