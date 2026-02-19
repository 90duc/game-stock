package com.stockgame.init;

import com.stockgame.dao.*;
import com.stockgame.model.*;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataInitializer {
    private final UserDao userDao;
    private final StockDao stockDao;
    private final DayKLineDao dayKLineDao;
    private final IntradayKLineDao intradayKLineDao;
    private final GameSessionDao gameSessionDao;
    
    private static final String[] STOCK_NAMES = {
            "腾讯控股", "阿里巴巴", "中国平安", "招商银行", "茅台集团",
            "五粮液", "比亚迪", "宁德时代", "美团", "京东"
    };
    
    private static final String[] STOCK_CODES = {
            "00700", "09988", "02318", "03968", "600519",
            "000858", "01211", "300750", "03690", "09618"
    };
    
    public DataInitializer() {
        this.userDao = new UserDao();
        this.stockDao = new StockDao();
        this.dayKLineDao = new DayKLineDao();
        this.intradayKLineDao = new IntradayKLineDao();
        this.gameSessionDao = new GameSessionDao();
    }
    
    public void initialize() {
        try {
            System.out.println("开始初始化数据...");
            
            // 初始化数据库表
            DatabaseUtil.initDatabase();
            
            // 创建默认用户
            createDefaultUser();
            
            // 创建股票
            createStocks();
            
            System.out.println("数据初始化完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createDefaultUser() throws SQLException {
        User existingUser = userDao.getDefaultUser();
        if (existingUser != null) {
            System.out.println("默认用户已存在");
            return;
        }
        
        User user = new User();
        user.setUsername("player1");
        user.setBalance(new BigDecimal("1000000.00"));
        user.setFrozenBalance(BigDecimal.ZERO);
        
        userDao.save(user);
        System.out.println("创建默认用户: player1, 初始资金: 100万元");
    }
    
    private void createStocks() throws SQLException {
        List<Stock> existingStocks = stockDao.getAll();
        if (!existingStocks.isEmpty()) {
            System.out.println("股票数据已存在");
            return;
        }
        
        Random random = new Random();
        
        for (int i = 0; i < STOCK_NAMES.length; i++) {
            Stock stock = new Stock();
            stock.setStockCode(STOCK_CODES[i]);
            stock.setStockName(STOCK_NAMES[i]);
            
            // 随机生成初始价格 5-500元
            double basePrice = 5 + random.nextDouble() * 30;
            BigDecimal price = BigDecimal.valueOf(basePrice).setScale(2, RoundingMode.HALF_UP);
            stock.setCurrentPrice(price);
            stock.setPreviousClose(price);
            stock.setIsTrading(false);
            
            stockDao.save(stock);
            System.out.println("创建股票: " + stock.getStockName() + " (" + stock.getStockCode() + "), 价格: " + price);
            
            generateKLines(stock, price);
        }
    }
    
    private void generateKLines(Stock stock, BigDecimal basePrice) throws SQLException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(10);
        
        Random random = new Random();
        BigDecimal currentPrice = basePrice;
        
        // 生成日K线
        List<DayKLine> dayKLines = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            // 跳过周末
            if (date.getDayOfWeek().getValue() <= 5) {
                double changePercent = (random.nextDouble() - 0.5) * 0.04; // 正负2%的波动
                BigDecimal open = currentPrice;
                BigDecimal close = currentPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                        .setScale(2, RoundingMode.HALF_UP);
                
                BigDecimal high = open.compareTo(close) > 0 ? open : close;
                BigDecimal low = open.compareTo(close) < 0 ? open : close;
                
                // 添加日内波动
                high = high.multiply(BigDecimal.valueOf(1 + random.nextDouble() * 0.01))
                        .setScale(2, RoundingMode.HALF_UP);
                low = low.multiply(BigDecimal.valueOf(1 - random.nextDouble() * 0.01))
                        .setScale(2, RoundingMode.HALF_UP);
                
                DayKLine dayKLine = new DayKLine();
                dayKLine.setStockId(stock.getId());
                dayKLine.setTradeDate(date);
                dayKLine.setOpen(open);
                dayKLine.setHigh(high);
                dayKLine.setLow(low);
                dayKLine.setClose(close);
                dayKLine.setVolume((long) (random.nextInt(100000) + 10000));
                
                dayKLines.add(dayKLine);
                currentPrice = close;
            }
            date = date.plusDays(1);
        }
        
        dayKLineDao.saveBatch(dayKLines);
        System.out.println("生成 " + stock.getStockName() + " 的日K线数据: " + dayKLines.size() + " 条");

        // 生成最后一天的分时K线数据
        DayKLine lastDayKLine = dayKLineDao.getLastKLine(stock.getId());
        if (lastDayKLine != null) {
            generateIntradayKLines(stock, lastDayKLine);
        }
        
        // 更新股票的上一个交易日
        if (lastDayKLine != null) {
            stock.setLastKLineDate(lastDayKLine.getTradeDate());
            stock.setPreviousClose(lastDayKLine.getClose());
            stockDao.update(stock);
        }
    }
    
    private void generateIntradayKLines(Stock stock, DayKLine lastDayKLine) throws SQLException {
        List<IntradayKLine> intradayKLines = new ArrayList<>();
        Random random = new Random();
        
        LocalDate tradeDate = lastDayKLine.getTradeDate();
        BigDecimal currentPrice = lastDayKLine.getOpen();
        
        // 创建一个已结束的游戏会话
        GameSession gameSession = new GameSession();
        gameSession.setStockId(stock.getId());
        gameSession.setStartTime(tradeDate.atTime(9, 30));
        gameSession.setIsActive(false);
        gameSession.setEndTime(tradeDate.atTime(15, 0));
        gameSessionDao.save(gameSession);
        // 手动设置is_active=0，因为save方法默认是true
        gameSessionDao.endSession(gameSession.getId());
        
        // 9:30 - 11:30, 13:00 - 15:00
        int[] startMinutes = {9 * 60 + 30, 13 * 60};
        int[] endMinutes = {11 * 60 + 30, 15 * 60};
        
        BigDecimal high = currentPrice;
        BigDecimal low = currentPrice;
        
        for (int session = 0; session < 2; session++) {
            int minute = startMinutes[session];
            int end = endMinutes[session];
            
            while (minute <= end) {
                int hour = minute / 60;
                int min = minute % 60;
                LocalDateTime time = tradeDate.atTime(hour, min);
                
                double changePercent = (random.nextDouble() - 0.5) * 0.003;
                currentPrice = currentPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                        .setScale(2, RoundingMode.HALF_UP);
                
                if (currentPrice.compareTo(high) > 0) high = currentPrice;
                if (currentPrice.compareTo(low) < 0) low = currentPrice;
                
                IntradayKLine line = new IntradayKLine();
                line.setStockId(stock.getId());
                line.setTime(time);
                line.setPrice(currentPrice);
                line.setVolume((long) (random.nextInt(10000) + 1000));
                line.setGameSessionId(gameSession.getId());
                
                intradayKLines.add(line);
                minute++;
            }
        }
        
        for (IntradayKLine line : intradayKLines) {
            intradayKLineDao.save(line);
        }
        System.out.println("生成 " + stock.getStockName() + " 的分时K线数据: " + intradayKLines.size() + " 条");
        
        // 根据分时数据更新日K线
        lastDayKLine.setHigh(high);
        lastDayKLine.setLow(low);
        lastDayKLine.setClose(currentPrice);
        updateDayKLine(lastDayKLine);
    }
    
    private void updateDayKLine(DayKLine dayKLine) throws SQLException {
        String sql = "UPDATE day_kline SET high = ?, low = ?, close = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, dayKLine.getHigh());
            stmt.setBigDecimal(2, dayKLine.getLow());
            stmt.setBigDecimal(3, dayKLine.getClose());
            stmt.setLong(4, dayKLine.getId());
            stmt.executeUpdate();
            conn.commit();
        }
    }

    public static void main(String[] args) {
        DataInitializer initializer = new DataInitializer();
        initializer.initialize();
    }
}
