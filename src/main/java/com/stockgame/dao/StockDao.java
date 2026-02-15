package com.stockgame.dao;

import com.stockgame.model.Stock;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StockDao {
    
    public List<Stock> getAll() throws SQLException {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stocks ORDER BY stock_code";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stocks.add(mapResultSet(rs));
            }
        }
        return stocks;
    }
    
    public Stock getById(Long id) throws SQLException {
        String sql = "SELECT * FROM stocks WHERE id = ?";
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
    
    public void save(Stock stock) throws SQLException {
        String sql = "INSERT INTO stocks (stock_code, stock_name, current_price, previous_close, " +
                "is_trading, last_kline_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, stock.getStockCode());
            stmt.setString(2, stock.getStockName());
            stmt.setBigDecimal(3, stock.getCurrentPrice());
            stmt.setBigDecimal(4, stock.getPreviousClose());
            stmt.setBoolean(5, stock.getIsTrading());
            if (stock.getLastKLineDate() != null) {
                stmt.setDate(6, Date.valueOf(stock.getLastKLineDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                stock.setId(rs.getLong(1));
            }
            conn.commit();
        }
    }
    
    public void update(Stock stock) throws SQLException {
        String sql = "UPDATE stocks SET current_price = ?, previous_close = ?, is_trading = ?, " +
                "game_start_time = ?, game_end_time = ?, last_kline_date = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, stock.getCurrentPrice());
            stmt.setBigDecimal(2, stock.getPreviousClose());
            stmt.setBoolean(3, stock.getIsTrading());
            if (stock.getGameStartTime() != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(stock.getGameStartTime()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }
            if (stock.getGameEndTime() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(stock.getGameEndTime()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            if (stock.getLastKLineDate() != null) {
                stmt.setDate(6, Date.valueOf(stock.getLastKLineDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            stmt.setLong(7, stock.getId());
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public void updatePrice(Long stockId, BigDecimal price) throws SQLException {
        String sql = "UPDATE stocks SET current_price = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, price);
            stmt.setLong(2, stockId);
            stmt.executeUpdate();
            conn.commit();
        }
    }
    
    public List<Stock> getTradingStocks() throws SQLException {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stocks WHERE is_trading = 1";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stocks.add(mapResultSet(rs));
            }
        }
        return stocks;
    }
    
    private Stock mapResultSet(ResultSet rs) throws SQLException {
        Stock stock = new Stock();
        stock.setId(rs.getLong("id"));
        stock.setStockCode(rs.getString("stock_code"));
        stock.setStockName(rs.getString("stock_name"));
        stock.setCurrentPrice(rs.getBigDecimal("current_price"));
        stock.setPreviousClose(rs.getBigDecimal("previous_close"));
        stock.setIsTrading(rs.getBoolean("is_trading"));
        
        Timestamp gameStartTime = rs.getTimestamp("game_start_time");
        if (gameStartTime != null) {
            stock.setGameStartTime(gameStartTime.toLocalDateTime());
        }
        
        Timestamp gameEndTime = rs.getTimestamp("game_end_time");
        if (gameEndTime != null) {
            stock.setGameEndTime(gameEndTime.toLocalDateTime());
        }
        
        Date lastKLineDate = rs.getDate("last_kline_date");
        if (lastKLineDate != null) {
            stock.setLastKLineDate(lastKLineDate.toLocalDate());
        }
        
        stock.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return stock;
    }
}
