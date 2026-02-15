package com.stockgame.dao;

import com.stockgame.model.DayKLine;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DayKLineDao {
    
    public List<DayKLine> getByStockId(Long stockId, int limit) throws SQLException {
        List<DayKLine> lines = new ArrayList<>();
        String sql = "SELECT * FROM day_kline WHERE stock_id = ? ORDER BY trade_date DESC LIMIT ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lines.add(mapResultSet(rs));
            }
        }
        return lines;
    }
    
    public DayKLine getLastKLine(Long stockId) throws SQLException {
        String sql = "SELECT * FROM day_kline WHERE stock_id = ? ORDER BY trade_date DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }
    
    public void save(DayKLine line) throws SQLException {
        String sql = "INSERT INTO day_kline (stock_id, trade_date, open, high, low, close, volume) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, line.getStockId());
            stmt.setDate(2, Date.valueOf(line.getTradeDate()));
            stmt.setBigDecimal(3, line.getOpen());
            stmt.setBigDecimal(4, line.getHigh());
            stmt.setBigDecimal(5, line.getLow());
            stmt.setBigDecimal(6, line.getClose());
            stmt.setLong(7, line.getVolume());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                line.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void saveBatch(List<DayKLine> lines) throws SQLException {
        String sql = "INSERT INTO day_kline (stock_id, trade_date, open, high, low, close, volume) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (DayKLine line : lines) {
                stmt.setLong(1, line.getStockId());
                stmt.setDate(2, Date.valueOf(line.getTradeDate()));
                stmt.setBigDecimal(3, line.getOpen());
                stmt.setBigDecimal(4, line.getHigh());
                stmt.setBigDecimal(5, line.getLow());
                stmt.setBigDecimal(6, line.getClose());
                stmt.setLong(7, line.getVolume());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        }
    }
    
    private DayKLine mapResultSet(ResultSet rs) throws SQLException {
        DayKLine line = new DayKLine();
        line.setId(rs.getLong("id"));
        line.setStockId(rs.getLong("stock_id"));
        line.setTradeDate(rs.getDate("trade_date").toLocalDate());
        line.setOpen(rs.getBigDecimal("open"));
        line.setHigh(rs.getBigDecimal("high"));
        line.setLow(rs.getBigDecimal("low"));
        line.setClose(rs.getBigDecimal("close"));
        line.setVolume(rs.getLong("volume"));
        line.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return line;
    }
}
