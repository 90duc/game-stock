package com.stockgame.service;

import com.stockgame.model.*;
import com.stockgame.dao.*;
import com.stockgame.util.DatabaseUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class StockTradingService {
    private final StockDao stockDao;
    private final OrderDao orderDao;
    private final TradeRecordDao tradeRecordDao;
    private final PositionDao positionDao;
    private final UserDao userDao;
    private final DayKLineDao dayKLineDao;
    private final IntradayKLineDao intradayKLineDao;
    private final GameSessionDao gameSessionDao;
    
    private final Map<Long, Timer> gameTimers;
    private static final int GAME_DURATION_MINUTES = 240;
    private static final BigDecimal PRICE_LIMIT = new BigDecimal("0.10");
    
    public StockTradingService() {
        this.stockDao = new StockDao();
        this.orderDao = new OrderDao();
        this.tradeRecordDao = new TradeRecordDao();
        this.positionDao = new PositionDao();
        this.userDao = new UserDao();
        this.dayKLineDao = new DayKLineDao();
        this.intradayKLineDao = new IntradayKLineDao();
        this.gameSessionDao = new GameSessionDao();
        this.gameTimers = new HashMap<>();
    }
    
    // 开始游戏 - 同步方法避免并发问题
    public synchronized void startGame(Long stockId) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null) {
            throw new RuntimeException("股票不存在");
        }
        
        if (stock.getIsTrading()) {
            // 游戏已经在进行中，检查是否是"僵尸"游戏（没有定时器）
            if (gameTimers.containsKey(stockId)) {
                throw new RuntimeException("该股票游戏正在进行中");
            } else {
                // 恢复游戏定时器
                System.out.println("恢复游戏定时器...");
                GameSession session = gameSessionDao.getActiveByStock(stockId);
                if (session != null) {
                    startPriceGenerator(stockId, session.getId());
                    startGameEndTimer(stockId, session.getId());
                    return;
                } else {
                    // 没有活跃会话，结束游戏并重新开始
                    endGame(stockId);
                }
            }
        }
        
        // 创建游戏会话
        GameSession session = new GameSession();
        session.setStockId(stockId);
        session.setStartTime(LocalDateTime.now());
        gameSessionDao.save(session);
        
        // 更新股票状态
        stock.setIsTrading(true);
        stock.setGameStartTime(LocalDateTime.now());
        stock.setGameEndTime(LocalDateTime.now().plusMinutes(GAME_DURATION_MINUTES));
        
        // 使用最后一次游戏的收盘价格作为开盘价
        BigDecimal lastGameClosePrice = getLastGameClosePrice(stockId);
        if (lastGameClosePrice != null) {
            stock.setCurrentPrice(lastGameClosePrice);
            stock.setPreviousClose(lastGameClosePrice);
        } else {
            // 如果没有历史游戏数据，使用日K线收盘价
            DayKLine lastDayKLine = dayKLineDao.getLastKLine(stockId);
            if (lastDayKLine != null) {
                stock.setCurrentPrice(lastDayKLine.getClose());
                stock.setPreviousClose(lastDayKLine.getClose());
            }
        }
        
        stockDao.update(stock);
        
        // 立即保存一条开盘价K线
        saveOpeningKLine(stockId, session.getId(), stock.getCurrentPrice());
        
        // 启动价格生成定时器
        startPriceGenerator(stockId, session.getId());
        
        // 启动游戏结束定时器
        startGameEndTimer(stockId, session.getId());
    }
    
    // 保存开盘价K线
    private void saveOpeningKLine(Long stockId, Long sessionId, BigDecimal price) throws SQLException {
        IntradayKLine kLine = new IntradayKLine();
        kLine.setStockId(stockId);
        kLine.setGameSessionId(sessionId);
        kLine.setTime(LocalDateTime.now());
        kLine.setPrice(price);
        kLine.setVolume(0L); // 开盘价成交量为0
        intradayKLineDao.save(kLine);
    }
    
    // 主动结束游戏
    public void endGame(Long stockId) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null || !stock.getIsTrading()) {
            return;
        }
        
        GameSession session = gameSessionDao.getActiveByStock(stockId);
        if (session != null) {
            cleanupGame(stockId, session.getId());
        }
    }
    
    // 检查游戏是否已超时
    public boolean isGameTimeout(Long stockId) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null || !stock.getIsTrading()) {
            return false;
        }
        
        GameSession session = gameSessionDao.getActiveByStock(stockId);
        if (session == null) {
            return true; // 没有活跃会话，应该结束
        }
        
        // 检查是否超过5分钟
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime now = LocalDateTime.now();
        long minutesPassed = java.time.Duration.between(startTime, now).toMinutes();
        
        return minutesPassed >= GAME_DURATION_MINUTES;
    }
    
    // 清理游戏
    private void cleanupGame(Long stockId, Long sessionId) throws SQLException {
        // 释放冻结资金（内部会取消挂单）
        releaseFrozenFunds(sessionId);
        
        // 结束游戏会话
        gameSessionDao.endSession(sessionId);
        
        // 更新股票状态
        Stock stock = stockDao.getById(stockId);
        stock.setIsTrading(false);
        stock.setGameEndTime(LocalDateTime.now());
        
        // 生成日K线
        generateDayKLine(stockId, sessionId);
        
        // 注意：不更新 previous_close，保持为本次游戏的开盘价（即上收价）
        
        stockDao.update(stock);
        
        // 取消定时器
        Timer timer = gameTimers.remove(stockId);
        if (timer != null) {
            timer.cancel();
        }
    }
    
    // 释放冻结资金 - 通过会话ID获取相关用户
    private void releaseFrozenFunds(Long sessionId) throws SQLException {
        // 获取该会话的所有相关用户（在取消订单前获取）
        List<Order> orders = orderDao.getPendingOrdersBySession(sessionId);
        Set<Long> userIds = new HashSet<>();
        for (Order order : orders) {
            userIds.add(order.getUserId());
        }
        
        // 取消所有挂单
        orderDao.cancelAllPendingOrders(sessionId);
        
        // 释放这些用户的冻结资金和冻结股票
        for (Long userId : userIds) {
            User user = userDao.getById(userId);
            // 重新计算该用户的冻结资金（基于其他未成交的买单）
            BigDecimal frozenBalance = orderDao.getFrozenBalanceByUser(userId);
            user.setFrozenBalance(frozenBalance);
            userDao.update(user);
            
            // 释放该用户所有持仓的冻结股票数量
            List<Position> positions = positionDao.getByUserId(userId);
            for (Position pos : positions) {
                int frozenSellQty = orderDao.getFrozenSellQuantity(userId, pos.getStockId());
                pos.setFrozenQuantity(frozenSellQty);
                positionDao.update(pos);
            }
            
            System.out.println("释放用户 " + userId + " 的冻结资金: " + frozenBalance);
        }
    }
    
    // 生成日K线
    private void generateDayKLine(Long stockId, Long sessionId) throws SQLException {
        List<IntradayKLine> intradayLines = intradayKLineDao.getByStockAndSession(stockId, sessionId);
        if (intradayLines.isEmpty()) {
            return;
        }
        
        BigDecimal open = intradayLines.get(0).getPrice();
        BigDecimal close = intradayLines.get(intradayLines.size() - 1).getPrice();
        BigDecimal high = open;
        BigDecimal low = open;
        long volume = 0;
        
        for (IntradayKLine line : intradayLines) {
            BigDecimal price = line.getPrice();
            if (price.compareTo(high) > 0) {
                high = price;
            }
            if (price.compareTo(low) < 0) {
                low = price;
            }
            volume += line.getVolume();
        }
        
        DayKLine dayKLine = new DayKLine();
        dayKLine.setStockId(stockId);
        dayKLine.setTradeDate(LocalDate.now());
        dayKLine.setOpen(open);
        dayKLine.setHigh(high);
        dayKLine.setLow(low);
        dayKLine.setClose(close);
        dayKLine.setVolume(volume);
        
        dayKLineDao.save(dayKLine);
    }
    
    // 启动价格生成器
    private void startPriceGenerator(Long stockId, Long sessionId) {
        Timer timer = new Timer();
        gameTimers.put(stockId, timer);
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    generatePrice(stockId, sessionId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000); // 每秒生成一次价格
    }
    
    // 启动游戏结束定时器
    private void startGameEndTimer(Long stockId, Long sessionId) {
        Timer timer = gameTimers.get(stockId);
        if (timer == null) {
            timer = new Timer();
            gameTimers.put(stockId, timer);
        }
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    cleanupGame(stockId, sessionId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, GAME_DURATION_MINUTES * 60 * 1000); // 5分钟后结束
    }
    
    // 生成价格
    private void generatePrice(Long stockId, Long sessionId) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null || !stock.getIsTrading()) {
            return;
        }
        
        BigDecimal basePrice = stock.getPreviousClose();
        BigDecimal currentPrice = stock.getCurrentPrice();
        
        // 生成波动价格（正负10%以内）
        Random random = new Random();
        double changePercent = (random.nextDouble() - 0.5) * 0.02; // 正负1%的波动
        BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                .setScale(2, RoundingMode.HALF_UP);
        
        // 确保价格在涨跌停范围内
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.ONE.add(PRICE_LIMIT)).setScale(2, RoundingMode.UP);
        BigDecimal minPrice = basePrice.multiply(BigDecimal.ONE.subtract(PRICE_LIMIT)).setScale(2, RoundingMode.UP);
        
        if (newPrice.compareTo(maxPrice) > 0) {
            newPrice = maxPrice;
        }
        if (newPrice.compareTo(minPrice) < 0) {
            newPrice = minPrice;
        }
        
        // 更新股票价格
        stockDao.updatePrice(stockId, newPrice);
        
        // 保存分时数据
        IntradayKLine intradayKLine = new IntradayKLine();
        intradayKLine.setStockId(stockId);
        intradayKLine.setTime(LocalDateTime.now());
        intradayKLine.setPrice(newPrice);
        intradayKLine.setVolume((long) random.nextInt(10000));
        intradayKLine.setGameSessionId(sessionId);
        intradayKLineDao.save(intradayKLine);
        
        // 匹配挂单
        matchOrders(stockId, newPrice, sessionId);
        
        // 更新持仓市值
        positionDao.updateAllCurrentValues();
    }
    
    // 匹配挂单
    private void matchOrders(Long stockId, BigDecimal currentPrice, Long sessionId) throws SQLException {
        List<Order> pendingOrders = orderDao.getPendingOrdersByStock(stockId, sessionId);
        
        for (Order order : pendingOrders) {
            if (order.getOrderType() == Order.OrderType.BUY) {
                // 买单：限价单价格 >= 当前价格，或市价单
                boolean shouldMatch = order.getPriceType() == Order.OrderPriceType.MARKET ||
                        order.getPrice().compareTo(currentPrice) >= 0;
                
                if (shouldMatch) {
                    executeTrade(order, currentPrice, sessionId);
                }
            } else {
                // 卖单：限价单价格 <= 当前价格，或市价单
                boolean shouldMatch = order.getPriceType() == Order.OrderPriceType.MARKET ||
                        order.getPrice().compareTo(currentPrice) <= 0;
                
                if (shouldMatch) {
                    executeTrade(order, currentPrice, sessionId);
                }
            }
        }
    }
    
    // 执行交易
    private void executeTrade(Order order, BigDecimal price, Long sessionId) throws SQLException {
        int quantity = order.getRemainingQuantity();
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));
        
        // 计算手续费: 万分之2.5, 最低5元
        BigDecimal commissionRate = new BigDecimal("0.00025");
        BigDecimal commission = totalAmount.multiply(commissionRate);
        commission = commission.max(new BigDecimal("5"));
        
        // 计算印花税: 万分之一, 仅卖出收取
        BigDecimal stampDutyRate = new BigDecimal("0.0001");
        BigDecimal stampDuty = BigDecimal.ZERO;
        if (order.getOrderType() == Order.OrderType.SELL) {
            stampDuty = totalAmount.multiply(stampDutyRate);
        }
        
        BigDecimal totalCost = totalAmount.add(commission).add(stampDuty);
        
        User user = userDao.getById(order.getUserId());
        Stock stock = stockDao.getById(order.getStockId());
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            // 买入 - 下单时已冻结资金，直接扣减
            user.setFrozenBalance(user.getFrozenBalance().subtract(order.getFrozenAmount()));
            user.setBalance(user.getBalance().subtract(totalCost));
            userDao.update(user);
            
            // 增加持仓
            Position position = positionDao.getByUserAndStock(order.getUserId(), order.getStockId());
            if (position == null) {
                position = new Position();
                position.setUserId(order.getUserId());
                position.setStockId(order.getStockId());
                position.setStockCode(stock.getStockCode());
                position.setStockName(stock.getStockName());
                position.setQuantity(quantity);
                position.setAverageCost(price);
                positionDao.save(position);
            } else {
                // 更新持仓成本和数量
                BigDecimal totalCostPosition = position.getAverageCost().multiply(BigDecimal.valueOf(position.getQuantity()))
                        .add(totalAmount);
                int newQuantity = position.getQuantity() + quantity;
                BigDecimal newAvgCost = totalCostPosition.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
                position.setQuantity(newQuantity);
                position.setAverageCost(newAvgCost);
                positionDao.update(position);
            }
        } else {
            // 卖出
            Position position = positionDao.getByUserAndStock(order.getUserId(), order.getStockId());
            if (position == null || position.getQuantity() < quantity) {
                return; // 持仓不足
            }
            
            // 增加资金(成交金额 - 手续费 - 印花税)
            BigDecimal netAmount = totalAmount.subtract(commission).subtract(stampDuty);
            user.setBalance(user.getBalance().add(netAmount));
            userDao.update(user);
            
            // 减少持仓
            position.setQuantity(position.getQuantity() - quantity);
            
            // 释放冻结的股票数量
            int currentFrozen = position.getFrozenQuantity() != null ? position.getFrozenQuantity() : 0;
            position.setFrozenQuantity(Math.max(0, currentFrozen - quantity));
            
            if (position.getQuantity() == 0) {
                position.setAverageCost(BigDecimal.ZERO);
            }
            positionDao.update(position);
        }
        
        // 更新订单状态
        order.setFilledQuantity(order.getQuantity());
        order.setStatus(Order.OrderStatus.FILLED);
        orderDao.update(order);
        
        // 保存成交记录
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setUserId(order.getUserId());
        tradeRecord.setStockId(order.getStockId());
        tradeRecord.setStockCode(stock.getStockCode());
        tradeRecord.setStockName(stock.getStockName());
        tradeRecord.setTradeType(order.getOrderType());
        tradeRecord.setQuantity(quantity);
        tradeRecord.setPrice(price);
        tradeRecord.setTotalAmount(totalAmount);
        tradeRecord.setGameSessionId(sessionId);
        tradeRecordDao.save(tradeRecord);
    }
    
    // 提交订单
    public void submitOrder(Long userId, Long stockId, Order.OrderType orderType, 
                           Order.OrderPriceType priceType, BigDecimal price, 
                           int quantity) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null) {
            throw new RuntimeException("股票不存在");
        }
        
        if (!stock.getIsTrading()) {
            throw new RuntimeException("该股票游戏未开始");
        }
        
        // 验证价格范围
        BigDecimal basePrice = stock.getPreviousClose();
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.ONE.add(PRICE_LIMIT)).setScale(2, RoundingMode.UP);
        BigDecimal minPrice = basePrice.multiply(BigDecimal.ONE.subtract(PRICE_LIMIT)).setScale(2, RoundingMode.UP);
        
        if (priceType == Order.OrderPriceType.LIMIT) {
            if (price.compareTo(maxPrice) > 0 || price.compareTo(minPrice) < 0) {
                throw new RuntimeException("委托价格超出涨跌停限制");
            }
        }
        
        User user = userDao.getById(userId);
        
        if (orderType == Order.OrderType.BUY) {
            // 买入检查 - 需要验证成交金额+手续费
            BigDecimal orderAmount = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal commissionRate = new BigDecimal("0.00025");
            BigDecimal commission = orderAmount.multiply(commissionRate);
            commission = commission.max(new BigDecimal("5"));
            BigDecimal totalRequired = orderAmount.add(commission);
            
            if (user.getAvailableBalance().compareTo(totalRequired) < 0) {
                throw new RuntimeException("可用资金不足(含手续费)");
            }
            
            // 冻结资金(使用Order的getFrozenAmount方法,已包含手续费)
            Order tempOrder = new Order();
            tempOrder.setOrderType(orderType);
            tempOrder.setPrice(price);
            tempOrder.setQuantity(quantity);
            tempOrder.setFilledQuantity(0);
            user.setFrozenBalance(user.getFrozenBalance().add(tempOrder.getFrozenAmount()));
            userDao.update(user);
        } else {
            // 卖出检查 - 需要冻结股票
            Position position = positionDao.getByUserAndStock(userId, stockId);
            int frozenQty = orderDao.getFrozenSellQuantity(userId, stockId);
            int availableQty = (position != null ? position.getQuantity() : 0) - frozenQty;
            if (availableQty < quantity) {
                throw new RuntimeException("持仓不足(已冻结: " + frozenQty + ")");
            }
            
            // 冻结股票 - 更新持仓的冻结数量
            if (position != null) {
                position.setFrozenQuantity(frozenQty + quantity);
                positionDao.update(position);
            }
        }
        
        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setStockId(stockId);
        order.setOrderType(orderType);
        order.setPriceType(priceType);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setStatus(Order.OrderStatus.PENDING);
        
        GameSession session = gameSessionDao.getActiveByStock(stockId);
        if (session != null) {
            order.setGameSessionId(session.getId());
        }
        
        orderDao.save(order);
    }
    
    // 取消订单
    public void cancelOrder(Long orderId) throws SQLException {
        Order order = orderDao.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只能取消待成交订单");
        }
        
        orderDao.cancelOrder(orderId);
        
        // 如果是买单，释放冻结资金
        if (order.getOrderType() == Order.OrderType.BUY) {
            User user = userDao.getById(order.getUserId());
            BigDecimal frozenAmount = order.getFrozenAmount();
            user.setFrozenBalance(user.getFrozenBalance().subtract(frozenAmount));
            userDao.update(user);
        } else if (order.getOrderType() == Order.OrderType.SELL) {
            // 释放冻结的股票
            Position position = positionDao.getByUserAndStock(order.getUserId(), order.getStockId());
            if (position != null) {
                int currentFrozen = position.getFrozenQuantity() != null ? position.getFrozenQuantity() : 0;
                int newFrozen = Math.max(0, currentFrozen - order.getRemainingQuantity());
                position.setFrozenQuantity(newFrozen);
                positionDao.update(position);
            }
        }
    }
    
    public List<Stock> getAllStocks() throws SQLException {
        return stockDao.getAll();
    }
    
    public Stock getStockById(Long id) throws SQLException {
        return stockDao.getById(id);
    }
    
    public List<Order> getUserOrders(Long userId) throws SQLException {
        return orderDao.getByUserId(userId);
    }
    
    public List<Position> getUserPositions(Long userId) throws SQLException {
        return positionDao.getByUserId(userId);
    }
    
    public List<TradeRecord> getUserTradeRecords(Long userId) throws SQLException {
        return tradeRecordDao.getByUserId(userId);
    }
    
    public User getUser(Long userId) throws SQLException {
        return userDao.getById(userId);
    }
    
    public List<DayKLine> getDayKLines(Long stockId) throws SQLException {
        return dayKLineDao.getByStockId(stockId, Integer.MAX_VALUE);
    }
    
    public List<WeekKLine> getWeekKLines(Long stockId) throws SQLException {
        List<DayKLine> dayKLines = dayKLineDao.getByStockId(stockId, Integer.MAX_VALUE);
        return aggregateToWeekKLine(dayKLines);
    }
    
    public List<MonthKLine> getMonthKLines(Long stockId) throws SQLException {
        List<DayKLine> dayKLines = dayKLineDao.getByStockId(stockId, Integer.MAX_VALUE);
        return aggregateToMonthKLine(dayKLines);
    }
    
    private List<WeekKLine> aggregateToWeekKLine(List<DayKLine> dayKLines) {
        if (dayKLines == null || dayKLines.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按日期升序排序，确保周K线连续
        List<DayKLine> sortedDays = new ArrayList<>(dayKLines);
        
        List<WeekKLine> weekKLines = new ArrayList<>();
        Map<Integer, List<DayKLine>> weekGroups = new LinkedHashMap<>();
        
        for (DayKLine day : sortedDays) {
            LocalDate date = day.getTradeDate();
            if (date == null) continue;
            int weekKey = date.getYear() * 100 + date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            weekGroups.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(day);
        }
        
        for (List<DayKLine> group : weekGroups.values()) {
            if (group.isEmpty()) continue;
            
            WeekKLine week = new WeekKLine();
            week.setStockId(group.get(0).getStockId());
            week.setWeekStart(group.get(0).getTradeDate());
            week.setWeekEnd(group.get(group.size() - 1).getTradeDate());
            week.setOpen(group.get(0).getOpen());
            week.setClose(group.get(group.size() - 1).getClose());
            
            BigDecimal high = group.stream()
                .map(DayKLine::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            week.setHigh(high);
            
            BigDecimal low = group.stream()
                .map(DayKLine::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            week.setLow(low);
            
            week.setVolume(group.stream().mapToLong(DayKLine::getVolume).sum());
            weekKLines.add(week);
        }

        return weekKLines;
    }
    
    private List<MonthKLine> aggregateToMonthKLine(List<DayKLine> dayKLines) {
        if (dayKLines == null || dayKLines.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按日期升序排序，确保月K线连续
        List<DayKLine> sortedDays = new ArrayList<>(dayKLines);
        sortedDays.sort(Comparator.comparing(DayKLine::getTradeDate));
        
        List<MonthKLine> monthKLines = new ArrayList<>();
        Map<Integer, List<DayKLine>> monthGroups = new LinkedHashMap<>();
        
        for (DayKLine day : sortedDays) {
            LocalDate date = day.getTradeDate();
            if (date == null) continue;
            int monthKey = date.getYear() * 100 + date.getMonthValue();
            monthGroups.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(day);
        }
        
        for (List<DayKLine> group : monthGroups.values()) {
            if (group.isEmpty()) continue;
            
            MonthKLine month = new MonthKLine();
            month.setStockId(group.get(0).getStockId());
            month.setMonthStart(group.get(0).getTradeDate());
            month.setMonthEnd(group.get(group.size() - 1).getTradeDate());
            month.setOpen(group.get(0).getOpen());
            month.setClose(group.get(group.size() - 1).getClose());
            
            BigDecimal high = group.stream()
                .map(DayKLine::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            month.setHigh(high);
            
            BigDecimal low = group.stream()
                .map(DayKLine::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            month.setLow(low);
            
            month.setVolume(group.stream().mapToLong(DayKLine::getVolume).sum());
            monthKLines.add(month);
        }
        

        return monthKLines;
    }
    
    public List<IntradayKLine> getIntradayKLines(Long stockId, Long sessionId) throws SQLException {
        return intradayKLineDao.getByStockAndSession(stockId, sessionId);
    }
    
    // 获取股票活跃会话的所有分时数据
    public List<IntradayKLine> getAllIntradayKLines(Long stockId) throws SQLException {
        return intradayKLineDao.getActiveByStock(stockId);
    }
    
    // 获取股票所有历史分时数据（包括已结束的游戏）
    public List<IntradayKLine> getAllHistoricalIntradayKLines(Long stockId) throws SQLException {
        return intradayKLineDao.getAllByStock(stockId);
    }
    
    // 获取指定会话的分时数据
    public List<IntradayKLine> getIntradayKLinesBySession(Long stockId, Long sessionId) throws SQLException {
        return intradayKLineDao.getByStockAndSession(stockId, sessionId);
    }
    
    // 获取最后一次游戏的收盘价格
    public BigDecimal getLastGameClosePrice(Long stockId) throws SQLException {
        // 获取该股票最新的已结束游戏会话
        String sql = "SELECT id FROM game_sessions WHERE stock_id = ? AND is_active = 0 ORDER BY end_time DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long lastSessionId = rs.getLong("id");
                // 获取该会话的最后一条分时数据（收盘价格）
                List<IntradayKLine> kLines = intradayKLineDao.getByStockAndSession(stockId, lastSessionId);
                if (kLines != null && !kLines.isEmpty()) {
                    // 返回最后一条记录的价格（收盘价格）
                    return kLines.get(kLines.size() - 1).getPrice();
                }
            }
        }
        return null;
    }
    
    // 获取上收价（最后一次已结束游戏的收盘价 = 本次/下次游戏的开盘价）
    public BigDecimal getLastGameOpenPrice(Long stockId) throws SQLException {
        Stock stock = stockDao.getById(stockId);
        if (stock == null) {
            return null;
        }
        
        // previous_close 始终保持为游戏的开盘价（上收价）
        return stock.getPreviousClose();
    }
    
    public List<GameSession> getUserGameSessions(Long userId) throws SQLException {
        return gameSessionDao.getByUserId(userId);
    }
    
    public List<GameSession> getStockGameSessions(Long stockId) throws SQLException {
        return gameSessionDao.getByStockId(stockId);
    }
    
    public GameSession getLastStockGameSession(Long stockId) throws SQLException {
        return gameSessionDao.getLastByStock(stockId);
    }
    
    // 结束所有进行中的游戏（程序启动时调用）
    public void endAllActiveGames() throws SQLException {
        List<Stock> stocks = stockDao.getTradingStocks();
        for (Stock stock : stocks) {
            System.out.println("结束游戏: " + stock.getStockName() + " (" + stock.getStockCode() + ")");
            endGame(stock.getId());
        }
    }
}
