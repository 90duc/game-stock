package com.stockgame.dao;

import com.stockgame.model.GameSession;
import com.stockgame.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameSessionDao {
    
    public GameSession getActiveByStock(Long stockId) throws SQLException {
        String sql = "SELECT * FROM game_sessions WHERE stock_id = ? AND is_active = 1 LIMIT 1";
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
    
    public List<GameSession> getAllActive() throws SQLException {
        List<GameSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM game_sessions WHERE is_active = 1";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
        }
        return sessions;
    }
    
    public GameSession getById(Long id) throws SQLException {
        String sql = "SELECT * FROM game_sessions WHERE id = ?";
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
    
    public void save(GameSession session) throws SQLException {
        String sql = "INSERT INTO game_sessions (stock_id, start_time, is_active) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, session.getStockId());
            stmt.setTimestamp(2, Timestamp.valueOf(session.getStartTime()));
            stmt.setBoolean(3, session.getIsActive());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                session.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void endSession(Long sessionId) throws SQLException {
        String sql = "UPDATE game_sessions SET is_active = 0, end_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public List<GameSession> getByUserId(Long userId) throws SQLException {
        List<GameSession> sessions = new ArrayList<>();
        String sql = "SELECT DISTINCT gs.* FROM game_sessions gs " +
                "JOIN orders o ON gs.id = o.game_session_id " +
                "WHERE o.user_id = ? ORDER BY gs.start_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
        }
        return sessions;
    }
    
    public List<GameSession> getByStockId(Long stockId) throws SQLException {
        List<GameSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM game_sessions WHERE stock_id = ? ORDER BY start_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
        }
        return sessions;
    }
    
    public GameSession getLastByStock(Long stockId) throws SQLException {
        String sql = "SELECT * FROM game_sessions WHERE stock_id = ? ORDER BY start_time DESC LIMIT 1";
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
    
    private GameSession mapResultSet(ResultSet rs) throws SQLException {
        GameSession session = new GameSession();
        session.setId(rs.getLong("id"));
        session.setStockId(rs.getLong("stock_id"));
        session.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        session.setIsActive(rs.getBoolean("is_active"));
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            session.setEndTime(endTime.toLocalDateTime());
        }
        
        session.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return session;
    }
}
