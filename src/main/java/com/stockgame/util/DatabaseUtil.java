package com.stockgame.util;

import java.sql.*;

public class DatabaseUtil {
    // 使用多线程模式
    private static final String DB_URL = "jdbc:sqlite:stock_game.db?journal_mode=WAL&synchronous=NORMAL";
    // 使用ThreadLocal确保每个线程有自己的连接
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);
            // 设置 busy timeout 避免并发访问冲突
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");
            }
            connectionHolder.set(conn);
        }
        return conn;
    }
    
    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connectionHolder.remove();
            }
        }
    }
    
    public static void initDatabase() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            
            // 用户表
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "balance DECIMAL(20, 4) NOT NULL DEFAULT 1000000.0000," +
                    "frozen_balance DECIMAL(20, 4) NOT NULL DEFAULT 0.0000," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // 股票表
            stmt.execute("CREATE TABLE IF NOT EXISTS stocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_code TEXT NOT NULL UNIQUE," +
                    "stock_name TEXT NOT NULL," +
                    "current_price DECIMAL(20, 4) NOT NULL DEFAULT 0.0000," +
                    "previous_close DECIMAL(20, 4) NOT NULL DEFAULT 0.0000," +
                    "is_trading BOOLEAN DEFAULT 0," +
                    "game_start_time TIMESTAMP," +
                    "game_end_time TIMESTAMP," +
                    "last_kline_date DATE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // 持仓表
            stmt.execute("CREATE TABLE IF NOT EXISTS positions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "stock_id INTEGER NOT NULL," +
                    "stock_code TEXT NOT NULL," +
                    "stock_name TEXT NOT NULL," +
                    "quantity INTEGER NOT NULL DEFAULT 0," +
                    "frozen_quantity INTEGER NOT NULL DEFAULT 0," +
                    "average_cost DECIMAL(20, 4) NOT NULL DEFAULT 0.0000," +
                    "current_value DECIMAL(20, 4)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)," +
                    "UNIQUE(user_id, stock_id)" +
                    ")");
            
            // 日K线表 - 移除日期唯一约束，允许同一天多条记录
            stmt.execute("CREATE TABLE IF NOT EXISTS day_kline (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_id INTEGER NOT NULL," +
                    "trade_date DATE NOT NULL," +
                    "open DECIMAL(20, 4) NOT NULL," +
                    "high DECIMAL(20, 4) NOT NULL," +
                    "low DECIMAL(20, 4) NOT NULL," +
                    "close DECIMAL(20, 4) NOT NULL," +
                    "volume INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)" +
                    ")");
            
            // 周K线表
            stmt.execute("CREATE TABLE IF NOT EXISTS week_kline (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_id INTEGER NOT NULL," +
                    "week_start DATE NOT NULL," +
                    "week_end DATE NOT NULL," +
                    "open DECIMAL(20, 4) NOT NULL," +
                    "high DECIMAL(20, 4) NOT NULL," +
                    "low DECIMAL(20, 4) NOT NULL," +
                    "close DECIMAL(20, 4) NOT NULL," +
                    "volume INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)," +
                    "UNIQUE(stock_id, week_start)" +
                    ")");
            
            // 月K线表
            stmt.execute("CREATE TABLE IF NOT EXISTS month_kline (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_id INTEGER NOT NULL," +
                    "month_start DATE NOT NULL," +
                    "month_end DATE NOT NULL," +
                    "open DECIMAL(20, 4) NOT NULL," +
                    "high DECIMAL(20, 4) NOT NULL," +
                    "low DECIMAL(20, 4) NOT NULL," +
                    "close DECIMAL(20, 4) NOT NULL," +
                    "volume INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)," +
                    "UNIQUE(stock_id, month_start)" +
                    ")");
            
            // 分时K线表
            stmt.execute("CREATE TABLE IF NOT EXISTS intraday_kline (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_id INTEGER NOT NULL," +
                    "time TIMESTAMP NOT NULL," +
                    "price DECIMAL(20, 4) NOT NULL," +
                    "volume INTEGER DEFAULT 0," +
                    "game_session_id INTEGER," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)" +
                    ")");
            
            // 挂单表
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "stock_id INTEGER NOT NULL," +
                    "order_type TEXT NOT NULL," +
                    "price_type TEXT NOT NULL," +
                    "quantity INTEGER NOT NULL," +
                    "price DECIMAL(20, 4) NOT NULL," +
                    "status TEXT NOT NULL DEFAULT 'PENDING'," +
                    "filled_quantity INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "game_session_id INTEGER," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)" +
                    ")");
            
            // 成交记录表
            stmt.execute("CREATE TABLE IF NOT EXISTS trade_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "stock_id INTEGER NOT NULL," +
                    "stock_code TEXT NOT NULL," +
                    "stock_name TEXT NOT NULL," +
                    "trade_type TEXT NOT NULL," +
                    "quantity INTEGER NOT NULL," +
                    "price DECIMAL(20, 4) NOT NULL," +
                    "total_amount DECIMAL(20, 4) NOT NULL," +
                    "trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "game_session_id INTEGER," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)" +
                    ")");
            
            // 游戏会话表
            stmt.execute("CREATE TABLE IF NOT EXISTS game_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "stock_id INTEGER NOT NULL," +
                    "start_time TIMESTAMP NOT NULL," +
                    "end_time TIMESTAMP," +
                    "is_active BOOLEAN DEFAULT 1," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (stock_id) REFERENCES stocks(id)" +
                    ")");
            
            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_stock ON orders(stock_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_positions_user ON positions(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_day_kline_stock ON day_kline(stock_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_intraday_stock ON intraday_kline(stock_id)");
            
            conn.commit();
            System.out.println("数据库初始化完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
