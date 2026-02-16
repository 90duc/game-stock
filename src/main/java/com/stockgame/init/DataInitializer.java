package com.stockgame.init;

import com.stockgame.dao.*;
import com.stockgame.model.*;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataInitializer {
    private final UserDao userDao;
    private final StockDao stockDao;
    private final DayKLineDao dayKLineDao;
    
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
            
            // 生成一年的K线数据
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

        
        // 更新股票的上一个交易日
        if (!dayKLines.isEmpty()) {
            DayKLine lastDayKLine = dayKLines.get(dayKLines.size() - 1);
            stock.setLastKLineDate(lastDayKLine.getTradeDate());
            stock.setPreviousClose(lastDayKLine.getClose());
            stockDao.update(stock);
        }
    }

    public static void main(String[] args) {
        DataInitializer initializer = new DataInitializer();
        initializer.initialize();
    }
}
