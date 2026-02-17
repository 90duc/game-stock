package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockHallView {
    private final StockTradingService tradingService;
    private final User currentUser;
    private final VBox view;
    private TableView<Stock> stockTable;
    private Label statusLabel;
    private final Map<Long, Stage> tradeStages = new HashMap<>();
    private final ObservableList<Stock> stockList = FXCollections.observableArrayList();
    
    private static final String FX_BG_ONE = "-fx-background-color: #0d0d0d;";
    private static final String FX_BG_TWO = "-fx-background-color: #1a1a1a;";
    private static final String FX_BG_HEADER = "-fx-background-color: #e67e22; -fx-text-fill:white;";
    
    public StockHallView(StockTradingService tradingService, User currentUser) {
        this.tradingService = tradingService;
        this.currentUser = currentUser;
        this.view = createView();
    }
    
    private VBox createView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0a0a0a;");
        // 标题
        Label titleLabel = new Label("股票交易大厅");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;-fx-text-fill:white;");
        
        // 股票列表
        stockTable = new TableView<>(stockList);
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stockTable.setStyle(FX_BG_ONE + "-fx-text-fill:white;");
        stockTable.setSelectionModel(null);
        
        TableColumn<Stock, String> codeCol = new TableColumn<>("股票代码");
        codeCol.setStyle(FX_BG_HEADER);
        Callback<TableColumn<Stock, String>, TableCell<Stock, String>> tableCellCallback = col -> new TableCell<Stock, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.WHITE);
                }
                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
            }
        };
        codeCol.setCellFactory(tableCellCallback);
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStockCode()));
        codeCol.setPrefWidth(50);
        
        TableColumn<Stock, String> nameCol = new TableColumn<>("股票名称");
        nameCol.setStyle(FX_BG_HEADER);
        nameCol.setCellFactory(tableCellCallback);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStockName()));
        nameCol.setPrefWidth(60);
        
        TableColumn<Stock, String> priceCol = new TableColumn<>("当前价格");
        priceCol.setStyle(FX_BG_HEADER);
        priceCol.setCellFactory(tableCellCallback);
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCurrentPrice().toString()));
        priceCol.setPrefWidth(70);
        
        TableColumn<Stock, String> prevCloseCol = new TableColumn<>("开盘价");
        prevCloseCol.setStyle(FX_BG_HEADER);
        prevCloseCol.setCellFactory(tableCellCallback);
        prevCloseCol.setCellValueFactory(data -> {
            BigDecimal lastGameOpenPrice = null;
            try {
                lastGameOpenPrice = tradingService.getLastGameOpenPrice(data.getValue().getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new SimpleStringProperty(lastGameOpenPrice != null ? lastGameOpenPrice.toString() : data.getValue().getPreviousClose().toString());
        });
        prevCloseCol.setPrefWidth(60);
        
        TableColumn<Stock, String> changeCol = new TableColumn<>("涨跌幅");
        changeCol.setStyle(FX_BG_HEADER);
        changeCol.setCellFactory(column -> new TableCell<Stock, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (item.startsWith("-")) {
                        setTextFill(Color.GREEN);
                    } else if (item.startsWith("0.00%")) {
                        setTextFill(Color.WHITE);
                    } else {
                        setTextFill(Color.RED);
                    }
                }
                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
            }
        });
        changeCol.setCellValueFactory(data -> {
            Stock stock = data.getValue();
            BigDecimal basePrice = null;
            try {
                basePrice = tradingService.getLastGameOpenPrice(stock.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (basePrice == null) {
                basePrice = stock.getPreviousClose();
            }
            BigDecimal change = stock.getCurrentPrice().subtract(basePrice);
            BigDecimal changePercent = change.divide(basePrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            return new SimpleStringProperty(String.format("%.2f%%", changePercent));
        });
        changeCol.setPrefWidth(60);
        
        TableColumn<Stock, String> statusCol = new TableColumn<>("状态");
        statusCol.setStyle(FX_BG_HEADER);
        statusCol.setCellFactory(tableCellCallback);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIsTrading() ? "交易中" : "未开始"));
        statusCol.setPrefWidth(50);
        
        // 操作列 - 显示"进入游戏"和"查看K线"按钮
        TableColumn<Stock, Void> actionCol = new TableColumn<>("操作");
        actionCol.setMinWidth(145);
        actionCol.setMaxWidth(145);
        actionCol.setStyle(FX_BG_HEADER);
        actionCol.setResizable(false);
        actionCol.setCellFactory(column -> new TableCell<Stock, Void>() {
            private final Button enterBtn = new Button("进入游戏");
            private final Button klineBtn = new Button("查看K线");
            private final HBox btnBox = new HBox(3);
            
            {
                enterBtn.setPrefWidth(60);
                enterBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                
                klineBtn.setPrefWidth(60);
                klineBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 10px;");
                
                btnBox.setAlignment(Pos.CENTER);
                btnBox.getChildren().addAll(enterBtn, klineBtn);
                
                enterBtn.setOnAction(event -> {
                    event.consume(); // 防止事件冒泡
                    Stock stock = getTableView().getItems().get(getIndex());
                    openTradeViewForStock(stock);
                });
                
                klineBtn.setOnAction(event -> {
                    event.consume();
                    Stock stock = getTableView().getItems().get(getIndex());
                    openKLineViewForStock(stock);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnBox);
                }

                setStyle(getIndex() % 2 == 0 ? FX_BG_ONE : FX_BG_TWO);
            }
        });
        
        stockTable.getColumns().addAll(codeCol, nameCol, priceCol, prevCloseCol, changeCol, statusCol, actionCol);

        // 状态栏
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 12px;");
        
        root.getChildren().addAll(titleLabel, stockTable, statusLabel);
        VBox.setVgrow(stockTable, Priority.ALWAYS);
        
        // 加载数据
        try {
            refresh();
        } catch (SQLException e) {
            statusLabel.setText("加载数据失败: " + e.getMessage());
        }
        
        return root;
    }
    
    public VBox getView() {
        return view;
    }
    
    public void refresh() throws SQLException {
        List<Stock> stocks = tradingService.getAllStocks();
        stockList.setAll(stocks);
    }
    
    private void openTradeViewForStock(Stock stock) {
        // 检查窗口是否已存在，存在则置顶
        Stage existingStage = tradeStages.get(stock.getId());
        if (existingStage != null) {
            existingStage.toFront();
            return;
        }

        try {
            // 刷新股票数据，确保获取最新状态
            Stock updatedStock = tradingService.getStockById(stock.getId());
            
            boolean showAsEnded = !updatedStock.getIsTrading();
            TradeView tradeView = new TradeView(tradingService, currentUser, updatedStock, showAsEnded);
            Stage stage = new Stage();
            stage.setTitle(updatedStock.getStockName() + " - 交易");
            stage.setScene(new Scene(tradeView.getView(), 800, 600));
            
            stage.setOnCloseRequest(e -> tradeStages.remove(stock.getId()));
            tradeStages.put(stock.getId(), stage);
            
            stage.show();
        } catch (SQLException e) {
            showAlert("打开交易界面失败: " + e.getMessage());
        }
    }
    
    private void openKLineViewForStock(Stock stock) {
        try {
            KLineView kLineView = new KLineView(tradingService, stock);
            Stage stage = new Stage();
            stage.setTitle(stock.getStockName() + " - K线图");
            stage.setScene(new Scene(kLineView.getView(), 900, 600));
            stage.show();
        } catch (Exception e) {
            showAlert("打开K线图失败: " + e.getMessage());
        }
    }
    
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
