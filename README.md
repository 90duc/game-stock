# 股票模拟交易系统

基于 Java 8 + JavaFX + SQLite 的股票交易模拟游戏。

## 项目说明

这是一个模拟中国A股交易的股票游戏，玩家可以用100万元虚拟资金进行股票交易。

---

## 一、项目架构

### 1.1 技术栈
- Java 8
- JavaFX 11 (UI框架)
- SQLite JDBC 3.36.0.3
- Hutool 5.7.1

### 1.2 项目结构

```
src/main/java/com/stockgame/
├── model/          # 实体类：User, Stock, Order, Position, TradeRecord, GameSession, KLine等
├── dao/            # 数据访问层：DatabaseUtil + 各表DAO
├── service/        # 业务逻辑层：StockTradingService
├── ui/             # JavaFX界面：StockGameApp, StockHallView, TradeView, AccountView, KLineView
├── util/           # 工具类：DatabaseUtil
└── init/           # 数据初始化：DataInitializer
```

---

## 二、数据库设计

### 2.1 核心表结构

```sql
-- 用户表
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE,
    balance DECIMAL(16,2),      -- 账户余额
    frozen_balance DECIMAL(16,2) -- 冻结资金
);

-- 股票表
CREATE TABLE stocks (
    id INTEGER PRIMARY KEY,
    stock_code TEXT,
    stock_name TEXT,
    current_price DECIMAL(10,2), -- 当前价格
    previous_close DECIMAL(10,2), -- 昨日收盘价（开盘基准价）
    is_trading INTEGER          -- 是否正在交易
);

-- 持仓表
CREATE TABLE positions (
    id INTEGER PRIMARY KEY,
    user_id, stock_id,
    quantity,                    -- 持仓数量
    frozen_quantity,             -- 冻结数量
    average_cost                 -- 平均成本
);

-- 委托单表
CREATE TABLE orders (
    id INTEGER PRIMARY KEY,
    user_id, stock_id,
    order_type TEXT,            -- BUY/SELL
    price_type TEXT,            -- LIMIT/MARKET
    quantity, price,
    status TEXT,                -- PENDING/FILLED/CANCELLED/EXPIRED
    filled_quantity
);

-- 成交记录表
CREATE TABLE trade_records (
    id INTEGER PRIMARY KEY,
    user_id, stock_id, order_id,
    trade_type, price, quantity, amount, commission
);

-- 游戏会话表
CREATE TABLE game_sessions (
    id INTEGER PRIMARY KEY,
    stock_id, start_time, end_time,
    is_active, base_price       -- 基准价（开盘价）
);

-- K线表：intraday_kline, day_kline, week_kline, month_kline
```

---

## 三、核心模型

### 3.1 Order.java (枚举定义)

```java
public class Order {
    public enum OrderType { BUY, SELL }
    public enum OrderPriceType { LIMIT, MARKET }
    public enum OrderStatus { PENDING, FILLED, CANCELLED, EXPIRED }
    
    // 冻结资金计算（买入单）
    public BigDecimal getFrozenAmount() {
        if (orderType == OrderType.BUY) {
            BigDecimal totalAmount = price.multiply(remainingQuantity);
            BigDecimal commission = totalAmount.multiply(0.00025).max(5);
            return totalAmount.add(commission);
        }
        return BigDecimal.ZERO;
    }
}
```

### 3.2 Position.java

```java
public class Position {
    public Integer getAvailableQuantity() { return quantity - frozenQuantity; }
    public BigDecimal getProfit() { return currentValue - averageCost * quantity; }
}
```

---

## 四、业务逻辑层 (StockTradingService)

### 4.1 核心常量

```java
private static final BigDecimal PRICE_LIMIT = new BigDecimal("0.10");  // 涨跌停10%
private static final int GAME_DURATION_MINUTES = 240;  // 4小时
```

### 4.2 价格波动逻辑

```java
// 每秒更新价格
public synchronized void updatePrice(Long stockId) {
    // 1. 生成波动价格（正负1%以内）
    double changePercent = (random.nextDouble() - 0.5) * 0.02;
    BigDecimal newPrice = currentPrice.multiply(1 + changePercent).setScale(2, HALF_UP);
    
    // 2. 涨跌停限制（向上取整两位小数）
    BigDecimal maxPrice = basePrice.multiply(1.10).setScale(2, UP);
    BigDecimal minPrice = basePrice.multiply(0.90).setScale(2, UP);
    
    // 3. 撮合挂单
    matchOrders(stockId, newPrice);
}
```

### 4.3 挂单撮合逻辑

```java
// 买入：市场价 <= 委托价 成交
// 卖出：市场价 >= 委托价 成交
private void matchOrders(Long stockId, BigDecimal currentPrice) {
    for (Order order : pendingOrders) {
        boolean match = (order.isBUY() && currentPrice.compareTo(order.getPrice()) <= 0) ||
                        (order.isSELL() && currentPrice.compareTo(order.getPrice()) >= 0);
        if (match) executeTrade(order, currentPrice);
    }
}
```

### 4.4 委托提交逻辑

```java
public void submitOrder(userId, stockId, orderType, priceType, price, quantity) {
    // 1. 验证涨跌停范围
    if (price < minPrice || price > maxPrice) throw new RuntimeException("超出涨跌停限制");
    
    // 2. 资金/持仓校验
    if (BUY) {
        if (availableBalance < totalNeeded) throw new RuntimeException("资金不足");
        freezeBalance(totalNeeded);  // 冻结资金
    } else {
        if (availableQuantity < quantity) throw new RuntimeException("持仓不足");
        freezeQuantity(quantity);    // 冻结持仓
    }
    
    // 3. 创建订单
    orderDao.save(order);
    
    // 4. 市价单立即撮合
    if (MARKET) matchOrders(stockId, currentPrice);
}
```

---

## 五、UI层设计

### 5.1 股票大厅 (StockHallView)

- TableView展示所有股票
- 状态列、操作列（含"进入游戏"按钮）
- 点击"进入游戏"：已存在则置顶，否则打开新窗口
- 关键代码：
```java
private final Map<Long, Stage> tradeStages = new HashMap<>();

private void openTradeViewForStock(Stock stock) {
    Stage existing = tradeStages.get(stock.getId());
    if (existing != null) { existing.toFront(); return; }  // 已存在则置顶
    
    Stage stage = new Stage();
    tradeStages.put(stock.getId(), stage);
    stage.show();
}
```

### 5.2 交易界面 (TradeView)

- **左侧**：分时走势图（Canvas绘制）
- **右上**：当前价格、涨跌幅、开盘价
- **右下**：买入/卖出按钮 → 弹出委托窗口
- **下方**：持仓列表、挂单列表、成交记录

#### 分时图绘制要点
```java
// 绘制当前价格线（数值在纵坐标左侧）
double yLast = TOP_MARGIN + (yAxisMax - lastPrice) / yAxisRange * DRAW_HEIGHT;
gc.fillText(String.format("%.2f", lastPrice), 5, yLast + 4);  // x=5对齐刻度
```

#### 委托弹窗提示后置顶
```java
private void showAlert(String message, Stage ownerStage) {
    Alert alert = new Alert(INFORMATION);
    alert.initOwner(ownerStage);
    alert.showAndWait();
    ownerStage.toFront();  // 关闭后置顶
}
```

### 5.3 自动刷新机制

```java
// 主程序启动定时刷新线程
new Thread(() -> {
    while (true) {
        Thread.sleep(1000);
        Platform.runLater(() -> {
            stockHallView.refresh();
            accountView.refresh();
        });
    }
}).start();
```

---

## 六、关键业务规则

| 规则 | 说明 |
|------|------|
| 涨跌停 | ±10%，超出部分向上取整两位 |
| 手续费 | 成交金额×0.025%，最低5元 |
| 游戏时长 | 4小时/局 |
| 初始资金 | 100万元 |
| 买入冻结 | 委托金额+手续费 |
| 卖出冻结 | 委托数量 |
| 撮合规则 | 价格优先、时间优先 |

---

## 七、构建运行

```bash
# 编译
mvn compile

# 运行
mvn exec:java

# 打包
mvn package
```

---

## 八、依赖配置 (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>11.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.36.0.3</version>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.7.1</version>
    </dependency>
</dependencies>
```
