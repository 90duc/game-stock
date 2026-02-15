# 股票模拟交易系统

基于 Java 8 + JavaFX + SQLite 的股票交易模拟游戏。

## 项目说明

这是一个模拟中国A股交易的股票游戏，玩家可以用100万元虚拟资金进行股票交易。

### 功能特性

- **股票大厅**: 展示所有股票及实时价格
- **模拟交易**: 4小时模拟一天的股票走势
- **挂单系统**: 支持限价单和市价单
- **K线图表**: 日K、周K、月K线展示
- **实时更新**: UI每秒从数据库刷新数据
- **持仓管理**: 查看持仓、成交记录、挂单记录

### 技术栈

- Java 8
- JavaFX (UI)
- SQLite (数据库)
- BigDecimal (金钱计算)

## 项目结构

```
src/main/java/com/stockgame/
├── model/          # 实体类
│   ├── User.java
│   ├── Stock.java
│   ├── Position.java
│   ├── Order.java
│   ├── TradeRecord.java
│   ├── DayKLine.java
│   ├── WeekKLine.java
│   ├── MonthKLine.java
│   ├── IntradayKLine.java
│   └── GameSession.java
├── dao/            # 数据访问层
│   ├── UserDao.java
│   ├── StockDao.java
│   ├── PositionDao.java
│   ├── OrderDao.java
│   ├── TradeRecordDao.java
│   ├── DayKLineDao.java
│   ├── WeekKLineDao.java
│   ├── MonthKLineDao.java
│   ├── IntradayKLineDao.java
│   └── GameSessionDao.java
├── service/        # 业务逻辑层
│   └── StockTradingService.java
├── ui/             # JavaFX界面
│   ├── StockGameApp.java
│   ├── StockHallView.java
│   ├── TradeView.java
│   ├── AccountView.java
│   └── KLineView.java
├── util/           # 工具类
│   └── DatabaseUtil.java
└── init/           # 数据初始化
    └── DataInitializer.java
```

## 运行环境要求

- JDK 8 (包含JavaFX)
- SQLite JDBC驱动

## 快速开始

### 1. 下载依赖

从 [SQLite JDBC Releases](https://github.com/xerial/sqlite-jdbc/releases) 下载 `sqlite-jdbc-3.36.0.3.jar`，放入 `lib` 目录。

### 2. 编译

**Linux/Mac:**
```bash
chmod +x build.sh
./build.sh
```

**Windows:**
```cmd
build.bat
```

### 3. 运行

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```

**Windows:**
```cmd
run.bat
```

### 4. 手动编译运行

如果脚本无法运行，可以手动执行：

```bash
# 创建输出目录
mkdir -p out

# 编译
javac -cp "lib/sqlite-jdbc-3.36.0.3.jar" \
    -d out \
    src/main/java/com/stockgame/**/*.java

# 运行
java -cp "out:lib/sqlite-jdbc-3.36.0.3.jar" \
    com.stockgame.ui.StockGameApp
```

## 游戏玩法

1. **初始资金**: 100万元
2. **股票列表**: 10只模拟股票
3. **游戏时长**: 每局4小时，模拟一天交易
4. **涨跌停**: 正负10%
5. **委托类型**: 
   - 限价单: 指定价格买卖
   - 市价单: 按当前价格买卖

### 操作流程

1. 打开程序后，在"股票大厅"查看所有股票
2. 选择一只股票，点击"开始游戏"启动交易
3. 点击"进入交易"打开交易界面
4. 在交易界面可以:
   - 查看分时走势图
   - 提交买入/卖出委托
   - 查看当前挂单
   - 撤销未成交的挂单
5. 在"个人账号"界面可以:
   - 查看账户余额和持仓
   - 查看成交记录
   - 查看所有挂单
   - 查看参与过的游戏记录

## 数据库表结构

- **users**: 用户表
- **stocks**: 股票表
- **positions**: 持仓表
- **orders**: 挂单表
- **trade_records**: 成交记录表
- **day_kline**: 日K线表
- **week_kline**: 周K线表
- **month_kline**: 月K线表
- **intraday_kline**: 分时K线表
- **game_sessions**: 游戏会话表

## 注意事项

1. 首次运行会自动初始化数据库和测试数据
2. 默认用户名为 "player1"
3. 数据存储在当前目录的 `stock_game.db` 文件中
4. 游戏超时或主动结束后，未成交的挂单会自动撤销

## 开发说明

- UI与业务逻辑分离
- 所有金钱计算使用 BigDecimal
- UI每秒定时从数据库刷新
- 交易逻辑实时写入数据库

## 许可证

MIT License
