package com.stockgame.dao;

import com.stockgame.model.IntradayKLine;
import com.stockgame.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IntradayKLineDao {
    
    public List<IntradayKLine> getByStockAndSession(Long stockId, Long gameSessionId) throws SQLException {
        List<IntradayKLine> lines = new ArrayList<>();
        String sql = "SELECT * FROM intraday_kline WHERE stock_id = ? AND game_session_id = ? ORDER BY time";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            stmt.setLong(2, gameSessionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lines.add(mapResultSet(rs));
            }
        }
        return lines;
    }
    
    public List<IntradayKLine> getRecentByStock(Long stockId, int limit) throws SQLException {
        List<IntradayKLine> lines = new ArrayList<>();
        String sql = "SELECT * FROM intraday_kline WHERE stock_id = ? ORDER BY time DESC LIMIT ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lines.add(0, mapResultSet(rs));
            }
        }
        return lines;
    }
    
    public void save(IntradayKLine line) throws SQLException {
        String sql = "INSERT INTO intraday_kline (stock_id, time, price, volume, game_session_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, line.getStockId());
            stmt.setTimestamp(2, Timestamp.valueOf(line.getTime()));
            stmt.setBigDecimal(3, line.getPrice());
            stmt.setLong(4, line.getVolume());
            if (line.getGameSessionId() != null) {
                stmt.setLong(5, line.getGameSessionId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                line.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void deleteBySession(Long gameSessionId) throws SQLException {
        String sql = "DELETE FROM intraday_kline WHERE game_session_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, gameSessionId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public List<IntradayKLine> getActiveByStock(Long stockId) throws SQLException {
        List<IntradayKLine> lines = new ArrayList<>();
        // 查询该股票最新活跃会话的分时数据
        String sql = "SELECT ik.* FROM intraday_kline ik " +
                "JOIN game_sessions gs ON ik.game_session_id = gs.id " +
                "WHERE ik.stock_id = ? AND gs.is_active = 1 " +
                "ORDER BY ik.time";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lines.add(mapResultSet(rs));
            }
        }
        return lines;
    }
    
    // 查询该股票所有历史分时数据（包括已结束的游戏）
    public List<IntradayKLine> getAllByStock(Long stockId) throws SQLException {
        List<IntradayKLine> lines = new ArrayList<>();
        String sql = "SELECT * FROM intraday_kline WHERE stock_id = ? ORDER BY time DESC LIMIT 1000";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lines.add(0, mapResultSet(rs)); // 保持时间升序
            }
        }
        return lines;
    }
    
    private IntradayKLine mapResultSet(ResultSet rs) throws SQLException {
        IntradayKLine line = new IntradayKLine();
        line.setId(rs.getLong("id"));
        line.setStockId(rs.getLong("stock_id"));
        line.setTime(rs.getTimestamp("time").toLocalDateTime());
        line.setPrice(rs.getBigDecimal("price"));
        line.setVolume(rs.getLong("volume"));
        
        Long gameSessionId = rs.getLong("game_session_id");
        if (!rs.wasNull()) {
            line.setGameSessionId(gameSessionId);
        }
        
        line.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return line;
    }
}
