package com.stockgame.dao;

import com.stockgame.model.TradeRecord;
import com.stockgame.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TradeRecordDao {
    
    public List<TradeRecord> getByUserId(Long userId) throws SQLException {
        List<TradeRecord> records = new ArrayList<>();
        String sql = "SELECT tr.*, s.stock_code, s.stock_name FROM trade_records tr " +
                "JOIN stocks s ON tr.stock_id = s.id " +
                "WHERE tr.user_id = ? ORDER BY tr.trade_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        }
        return records;
    }
    
    public List<TradeRecord> getByUserAndStock(Long userId, Long stockId) throws SQLException {
        List<TradeRecord> records = new ArrayList<>();
        String sql = "SELECT tr.*, s.stock_code, s.stock_name FROM trade_records tr " +
                "JOIN stocks s ON tr.stock_id = s.id " +
                "WHERE tr.user_id = ? AND tr.stock_id = ? ORDER BY tr.trade_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        }
        return records;
    }
    
    public void save(TradeRecord record) throws SQLException {
        String sql = "INSERT INTO trade_records (user_id, stock_id, stock_code, stock_name, " +
                "trade_type, quantity, price, total_amount, game_session_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, record.getUserId());
            stmt.setLong(2, record.getStockId());
            stmt.setString(3, record.getStockCode());
            stmt.setString(4, record.getStockName());
            stmt.setString(5, record.getTradeType().name());
            stmt.setInt(6, record.getQuantity());
            stmt.setBigDecimal(7, record.getPrice());
            stmt.setBigDecimal(8, record.getTotalAmount());
            if (record.getGameSessionId() != null) {
                stmt.setLong(9, record.getGameSessionId());
            } else {
                stmt.setNull(9, Types.BIGINT);
            }
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                record.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    private TradeRecord mapResultSet(ResultSet rs) throws SQLException {
        TradeRecord record = new TradeRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getLong("user_id"));
        record.setStockId(rs.getLong("stock_id"));
        record.setStockCode(rs.getString("stock_code"));
        record.setStockName(rs.getString("stock_name"));
            record.setTradeType(com.stockgame.model.Order.OrderType.valueOf(rs.getString("trade_type")));
        record.setQuantity(rs.getInt("quantity"));
        record.setPrice(rs.getBigDecimal("price"));
        record.setTotalAmount(rs.getBigDecimal("total_amount"));
        
        Long gameSessionId = rs.getLong("game_session_id");
        if (!rs.wasNull()) {
            record.setGameSessionId(gameSessionId);
        }
        
        record.setTradeTime(rs.getTimestamp("trade_time").toLocalDateTime());
        return record;
    }
}
