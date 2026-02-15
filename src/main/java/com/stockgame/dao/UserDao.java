package com.stockgame.dao;

import com.stockgame.model.User;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;

public class UserDao {
    
    public User getById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
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
    
    public User getDefaultUser() throws SQLException {
        String sql = "SELECT * FROM users LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }
    
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, balance, frozen_balance) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setBigDecimal(2, user.getBalance());
            stmt.setBigDecimal(3, user.getFrozenBalance());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET balance = ?, frozen_balance = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, user.getBalance());
            stmt.setBigDecimal(2, user.getFrozenBalance());
            stmt.setLong(3, user.getId());
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void updateBalance(Long userId, BigDecimal balance, BigDecimal frozenBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ?, frozen_balance = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, balance);
            stmt.setBigDecimal(2, frozenBalance);
            stmt.setLong(3, userId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    private User mapResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setFrozenBalance(rs.getBigDecimal("frozen_balance"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }
}
