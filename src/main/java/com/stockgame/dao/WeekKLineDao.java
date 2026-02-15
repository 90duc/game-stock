package com.stockgame.dao;

import com.stockgame.model.WeekKLine;
import com.stockgame.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WeekKLineDao {
    
    public List<WeekKLine> getByStockId(Long stockId, int limit) throws SQLException {
        List<WeekKLine> lines = new ArrayList<>();
        String sql = "SELECT * FROM week_kline WHERE stock_id = ? ORDER BY week_start DESC LIMIT ?";
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
    
    public void save(WeekKLine line) throws SQLException {
        String sql = "INSERT INTO week_kline (stock_id, week_start, week_end, open, high, low, close, volume) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, line.getStockId());
            stmt.setDate(2, Date.valueOf(line.getWeekStart()));
            stmt.setDate(3, Date.valueOf(line.getWeekEnd()));
            stmt.setBigDecimal(4, line.getOpen());
            stmt.setBigDecimal(5, line.getHigh());
            stmt.setBigDecimal(6, line.getLow());
            stmt.setBigDecimal(7, line.getClose());
            stmt.setLong(8, line.getVolume());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                line.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    private WeekKLine mapResultSet(ResultSet rs) throws SQLException {
        WeekKLine line = new WeekKLine();
        line.setId(rs.getLong("id"));
        line.setStockId(rs.getLong("stock_id"));
        line.setWeekStart(rs.getDate("week_start").toLocalDate());
        line.setWeekEnd(rs.getDate("week_end").toLocalDate());
        line.setOpen(rs.getBigDecimal("open"));
        line.setHigh(rs.getBigDecimal("high"));
        line.setLow(rs.getBigDecimal("low"));
        line.setClose(rs.getBigDecimal("close"));
        line.setVolume(rs.getLong("volume"));
        line.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return line;
    }
}
