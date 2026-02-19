package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AccountView {
    private final StockTradingService tradingService;
    private final User currentUser;
    private final VBox view;
    private Label balanceLabel;
    private Label frozenLabel;
    private Label availableLabel;
    private Label stockValueLabel;
    private Label netAssetLabel;
    private TableView<Position> positionTable;
    private TableView<TradeRecord> tradeTable;
    private TableView<Order> orderTable;
    private TableView<GameSession> gameTable;
    
    private static final String FX_BG_ONE = "-fx-background-color: #0d0d0d;";
    private static final String FX_BG_TWO = "-fx-background-color: #1a1a1a;";
    private static final String FX_BG_HEADER = "-fx-background-color: #e67e22; -fx-text-fill:white;";
    
    private <T> javafx.util.Callback<TableColumn<T, String>, TableCell<T, String>> createCell() {
        return col -> new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(javafx.scene.paint.Color.WHITE);

                }
                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
            }
        };
    }
    
    public AccountView(StockTradingService tradingService, User currentUser) {
        this.tradingService = tradingService;
        this.currentUser = currentUser;
        this.view = createView();
    }
    
    private VBox createView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0a0a0a;");
        
        // 标题
        Label titleLabel = new Label("个人账号");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // 账户信息
        GridPane accountInfo = new GridPane();
        accountInfo.setHgap(20);
        accountInfo.setVgap(10);
        
        balanceLabel = new Label("账户余额: 1000000.00");
        balanceLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        frozenLabel = new Label("冻结资金: 0.00");
        frozenLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        availableLabel = new Label("可用资金: 1000000.00");
        availableLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        stockValueLabel = new Label("股票价值: 0.00");
        stockValueLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        netAssetLabel = new Label("净资产: 1000000.00");
        netAssetLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        accountInfo.addRow(0, balanceLabel, frozenLabel, availableLabel);
        accountInfo.addRow(1, stockValueLabel, netAssetLabel);
        
        // 创建TabPane标签页
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // 1. 股票持仓标签页
        Tab positionTab = new Tab("股票持仓");
        positionTable = createPositionTable();
        positionTab.setContent(positionTable);
        
        // 2. 成交记录标签页
        Tab tradeTab = new Tab("成交记录");
        tradeTable = createTradeTable();
        tradeTab.setContent(tradeTable);
        
        // 3. 挂单记录标签页
        Tab orderTab = new Tab("挂单记录");
        orderTable = createOrderTable();
        orderTab.setContent(orderTable);
        
        // 4. 游戏记录标签页
        Tab gameTab = new Tab("游戏记录");
        gameTable = createGameTable();
        gameTab.setContent(gameTable);
        
        tabPane.getTabs().addAll(positionTab, tradeTab, orderTab, gameTab);
        
        root.getChildren().addAll(titleLabel, accountInfo, tabPane);
        
        return root;
    }
    
    private TableView<Position> createPositionTable() {
        TableView<Position> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(FX_BG_ONE + "-fx-text-fill:white;");
        
        TableColumn<Position, String> stockCodeCol = new TableColumn<>("股票代码");
        stockCodeCol.setStyle(FX_BG_HEADER);
        Callback<TableColumn<Position, String>, TableCell<Position, String>> tableColumnTableCellCallback = col -> new TableCell<Position, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    setTextFill(Color.WHITE);
                }
                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
            }
        };
        stockCodeCol.setCellFactory(tableColumnTableCellCallback);
        stockCodeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStockCode()));
        
        TableColumn<Position, String> stockNameCol = new TableColumn<>("股票名称");
        stockNameCol.setStyle(FX_BG_HEADER);
        stockNameCol.setCellFactory(tableColumnTableCellCallback);
        stockNameCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStockName()));
        
        TableColumn<Position, String> quantityCol = new TableColumn<>("持仓数量");
        quantityCol.setStyle(FX_BG_HEADER);
        quantityCol.setCellFactory(tableColumnTableCellCallback);
        quantityCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantity())));
        
        TableColumn<Position, String> avgCostCol = new TableColumn<>("成本价");
        avgCostCol.setStyle(FX_BG_HEADER);
        avgCostCol.setCellFactory(tableColumnTableCellCallback);
        avgCostCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAverageCost().toString()));
        
        TableColumn<Position, String> currentValueCol = new TableColumn<>("当前市值");
        currentValueCol.setStyle(FX_BG_HEADER);
        currentValueCol.setCellFactory(tableColumnTableCellCallback);
        currentValueCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCurrentValue() != null ? 
                        data.getValue().getCurrentValue().toString() : "0.00"));
        
        TableColumn<Position, String> profitCol = new TableColumn<>("盈亏");
        profitCol.setStyle(FX_BG_HEADER);
        profitCol.setCellFactory(col -> new TableCell<Position, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
                if (item != null) {
                    setText(item);
                    try {
                        BigDecimal val = new BigDecimal(item);
                        if (val.compareTo(BigDecimal.ZERO) >= 0) {
                            setTextFill(javafx.scene.paint.Color.RED);
                        } else {
                            setTextFill(javafx.scene.paint.Color.GREEN);
                        }
                    } catch (Exception e) {
                        setTextFill(javafx.scene.paint.Color.WHITE);
                    }
                }
            }
        });
        profitCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProfit().toString()));
        
        table.getColumns().addAll(stockCodeCol, stockNameCol, quantityCol, 
                avgCostCol, currentValueCol, profitCol);
        
        return table;
    }
    
    private TableView<TradeRecord> createTradeTable() {
        TableView<TradeRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(FX_BG_ONE + "-fx-text-fill:white;");
        
        TableColumn<TradeRecord, String> tradeTimeCol = new TableColumn<>("成交时间");
        tradeTimeCol.setStyle(FX_BG_HEADER);
        tradeTimeCol.setCellFactory(this.<TradeRecord>createCell());
        tradeTimeCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getTradeTime).map(Object::toString).orElse("")));
        
        TableColumn<TradeRecord, String> tradeStockCol = new TableColumn<>("股票");
        tradeStockCol.setStyle(FX_BG_HEADER);
        tradeStockCol.setCellFactory(this.<TradeRecord>createCell());
        tradeStockCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getStockName).orElse("")));
        
        TableColumn<TradeRecord, String> tradeTypeCol = new TableColumn<>("类型");
        tradeTypeCol.setStyle(FX_BG_HEADER);
        tradeTypeCol.setCellFactory(this.<TradeRecord>createCell());
        tradeTypeCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getTradeType).map(Order.OrderType::getText).orElse("")));
        
        TableColumn<TradeRecord, String> tradePriceCol = new TableColumn<>("成交价格");
        tradePriceCol.setStyle(FX_BG_HEADER);
        tradePriceCol.setCellFactory(this.<TradeRecord>createCell());
        tradePriceCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getPrice).map(BigDecimal::toString).orElse("")));
        
        TableColumn<TradeRecord, String> tradeQtyCol = new TableColumn<>("成交数量");
        tradeQtyCol.setStyle(FX_BG_HEADER);
        tradeQtyCol.setCellFactory(this.<TradeRecord>createCell());
        tradeQtyCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getQuantity).map(String::valueOf).orElse("")));
        
        TableColumn<TradeRecord, String> tradeAmountCol = new TableColumn<>("成交金额");
        tradeAmountCol.setStyle(FX_BG_HEADER);
        tradeAmountCol.setCellFactory(this.<TradeRecord>createCell());
        tradeAmountCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(TradeRecord::getTotalAmount).map(BigDecimal::toString).orElse("")));
        
        table.getColumns().addAll(tradeTimeCol, tradeStockCol, tradeTypeCol, tradePriceCol, tradeQtyCol, tradeAmountCol);
        
        return table;
    }
    
    private TableView<Order> createOrderTable() {
        TableView<Order> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(FX_BG_ONE + "-fx-text-fill:white;");
        
        TableColumn<Order, String> orderTimeCol = new TableColumn<>("挂单时间");
        orderTimeCol.setStyle(FX_BG_HEADER);
        orderTimeCol.setCellFactory(this.<Order>createCell());
        orderTimeCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getCreatedAt).map(Object::toString).orElse("")));
        
        TableColumn<Order, String> orderTypeCol = new TableColumn<>("类型");
        orderTypeCol.setStyle(FX_BG_HEADER);
        orderTypeCol.setCellFactory(this.<Order>createCell());
        orderTypeCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getOrderType).map(Order.OrderType::getText).orElse("")));
        
        TableColumn<Order, String> orderPriceCol = new TableColumn<>("委托价格");
        orderPriceCol.setStyle(FX_BG_HEADER);
        orderPriceCol.setCellFactory(this.<Order>createCell());
        orderPriceCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getPrice).map(BigDecimal::toString).orElse("")));
        
        TableColumn<Order, String> orderQtyCol = new TableColumn<>("委托数量");
        orderQtyCol.setStyle(FX_BG_HEADER);
        orderQtyCol.setCellFactory(this.<Order>createCell());
        orderQtyCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getQuantity).map(String::valueOf).orElse("")));
        
        TableColumn<Order, String> filledQtyCol = new TableColumn<>("成交数量");
        filledQtyCol.setStyle(FX_BG_HEADER);
        filledQtyCol.setCellFactory(this.<Order>createCell());
        filledQtyCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getFilledQuantity).map(String::valueOf).orElse("")));
        
        TableColumn<Order, String> orderStatusCol = new TableColumn<>("状态");
        orderStatusCol.setStyle(FX_BG_HEADER);
        orderStatusCol.setCellFactory(this.<Order>createCell());
        orderStatusCol.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(data.getValue()).map(Order::getStatus).map(Order.OrderStatus::getText).orElse("")));
        
        table.getColumns().addAll(orderTimeCol, orderTypeCol, orderPriceCol, orderQtyCol, filledQtyCol, orderStatusCol);
        
        return table;
    }
    
    private TableView<GameSession> createGameTable() {
        TableView<GameSession> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(FX_BG_ONE + "-fx-text-fill:white;");
        
        TableColumn<GameSession, String> gameIdCol = new TableColumn<>("游戏ID");
        gameIdCol.setStyle(FX_BG_HEADER);
        gameIdCol.setCellFactory(this.<GameSession>createCell());
        gameIdCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getId())));
        
        TableColumn<GameSession, String> stockCol = new TableColumn<>("股票");
        stockCol.setStyle(FX_BG_HEADER);
        stockCol.setCellFactory(this.<GameSession>createCell());
        stockCol.setCellValueFactory(data -> {
            try {
                Stock stock = tradingService.getStockById(data.getValue().getStockId());
                return new SimpleStringProperty(stock != null ? stock.getStockName() : "未知");
            } catch (SQLException e) {
                return new SimpleStringProperty("未知");
            }
        });
        
        TableColumn<GameSession, String> startTimeCol = new TableColumn<>("开始时间");
        startTimeCol.setStyle(FX_BG_HEADER);
        startTimeCol.setCellFactory(this.<GameSession>createCell());
        startTimeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartTime().toString()));
        
        TableColumn<GameSession, String> endTimeCol = new TableColumn<>("结束时间");
        endTimeCol.setStyle(FX_BG_HEADER);
        endTimeCol.setCellFactory(this.<GameSession>createCell());
        endTimeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEndTime() != null ? 
                        data.getValue().getEndTime().toString() : "进行中"));
        
        TableColumn<GameSession, String> statusCol = new TableColumn<>("状态");
        statusCol.setStyle(FX_BG_HEADER);
        statusCol.setCellFactory(this.<GameSession>createCell());
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIsActive() ? "进行中" : "已结束"));
        
        table.getColumns().addAll(gameIdCol, stockCol, startTimeCol, endTimeCol, statusCol);
        
        return table;
    }
    
    public VBox getView() {
        return view;
    }
    
    public void refresh() throws SQLException {
        User user = tradingService.getUser(currentUser.getId());
        balanceLabel.setText(String.format("账户余额: %.2f", user.getBalance()));
        frozenLabel.setText(String.format("冻结资金: %.2f", user.getFrozenBalance()));
        availableLabel.setText(String.format("可用资金: %.2f", user.getAvailableBalance()));
        
        // 更新持仓并计算股票价值
        List<Position> positions = tradingService.getUserPositions(user.getId());
        positionTable.getItems().clear();
        positionTable.getItems().addAll(positions);
        
        BigDecimal stockValue = BigDecimal.ZERO;
        for (Position pos : positions) {
            if (pos.getCurrentValue() != null) {
                stockValue = stockValue.add(pos.getCurrentValue());
            }
        }
        stockValueLabel.setText(String.format("股票价值: %.2f", stockValue));
        
        BigDecimal netAsset = user.getBalance().add(stockValue);
        netAssetLabel.setText(String.format("净资产: %.2f", netAsset));
        
        // 更新成交记录
        List<TradeRecord> records = tradingService.getUserTradeRecords(user.getId());
        tradeTable.getItems().clear();
        tradeTable.getItems().addAll(records);
        
        // 更新挂单
        List<Order> orders = tradingService.getUserOrders(user.getId());
        orderTable.getItems().clear();
        orderTable.getItems().addAll(orders);
        
        // 更新游戏记录
        List<GameSession> games = tradingService.getUserGameSessions(user.getId());
        gameTable.getItems().clear();
        gameTable.getItems().addAll(games);
    }
}
