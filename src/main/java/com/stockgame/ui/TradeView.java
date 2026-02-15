package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TradeView {
    private final StockTradingService tradingService;
    private final User currentUser;
    private Stock stock;
    private VBox view;
    private Canvas candlestickChart;
    private Pane chartContainer;
    private ScrollBar klineScrollBar;
    private boolean userScrollingKline = false;
    private boolean updatingScrollBar = false;
    private TableView<Order> orderTable;
    private Label priceLabel;
    private Button startEndGameBtn; // 开始/结束游戏按钮
    private javafx.animation.Timeline refreshTimeline;
    private int klineInterval = 1; // 默认1秒K线
    private static final int CHART_WIDTH = 480;
    private static final int CHART_HEIGHT = 350;
    private Long sessionId;
    
    public TradeView(StockTradingService tradingService, User currentUser, Stock stock) {
        this(tradingService, currentUser, stock, false);
    }
    
    public TradeView(StockTradingService tradingService, User currentUser, Stock stock, boolean showAsEnded) {
        this.tradingService = tradingService;
        this.currentUser = currentUser;
        // 检查并恢复游戏状态
        this.stock = checkAndRestoreGameState(stock);
        this.view = createView();
        // 只有在游戏已开始的情况下才启动定时器
        if (this.stock.getIsTrading()) {
            startRefreshTimer();
        }
    }
    
    // 检查游戏状态，如果游戏已超时则自动结束
    private Stock checkAndRestoreGameState(Stock stock) {
        if (stock.getIsTrading()) {
            try {
                // 检查游戏是否已超时
                if (tradingService.isGameTimeout(stock.getId())) {
                    System.out.println("游戏已超时，自动结束...");
                    tradingService.endGame(stock.getId());
                    // 重新获取更新后的股票状态
                    return tradingService.getStockById(stock.getId());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return stock;
    }
    
    // 创建K线类型选择按钮（用于活跃游戏界面）
    private ToggleButton createKlineTypeButton(String text, int interval, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setPrefWidth(50);
        btn.setStyle("-fx-font-size: 11px;");
        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                klineInterval = interval;
                userScrollingKline = false;
                try {
                    updateCandlestickChart();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
        return btn;
    }
    
    // 创建K线类型选择按钮（用于历史数据界面）
    private ToggleButton createHistoricalKlineTypeButton(String text, int interval, ToggleGroup group, Long sessionId) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setPrefWidth(50);
        btn.setStyle("-fx-font-size: 11px;");
        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                klineInterval = interval;
                // 刷新历史K线图（使用本局数据）
                refreshHistoricalChart(sessionId);
            }
        });
        return btn;
    }
    
    // 设置蜡烛图鼠标悬停提示
    private void setupCandlestickChartHover() {
        Tooltip tooltip = new Tooltip();
        tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #333; -fx-text-fill: white;");
        
        candlestickChart.setOnMouseMoved(event -> {
            try {
                // 获取当前显示的K线数据
                List<IntradayKLine> kLines = tradingService.getAllIntradayKLines(stock.getId());
                if (kLines == null || kLines.isEmpty()) {
                    tooltip.hide();
                    return;
                }
                
                // 根据当前klineInterval聚合数据
                kLines = aggregateKLines(kLines, klineInterval);
                
                double mouseX = event.getX();
                double mouseY = event.getY();
                
                // 获取实际画布尺寸（与绘制时保持一致）
                double canvasWidth = candlestickChart.getWidth();
                
                // 图表参数（与绘制时保持一致）
                final double LEFT_MARGIN = 60;
                final double RIGHT_MARGIN = 15;
                final double PIXELS_PER_UNIT = 8.0;
                // 最新蜡烛条右边保留4个蜡烛条宽度
                final double RIGHT_CANDLE_GAP = PIXELS_PER_UNIT * 4;
                final double DRAW_WIDTH = canvasWidth - LEFT_MARGIN - RIGHT_MARGIN;
                
                // 计算显示范围（与绘制时保持一致）
                double totalWidth = kLines.size() * PIXELS_PER_UNIT;
                double startX;
                // 右边保留至少4个蜡烛条宽度时才靠右，否则向左移动显示更多数据
                if (totalWidth <= DRAW_WIDTH - RIGHT_CANDLE_GAP) {
                    // 右边有足够空间，从左边开始（保持右边4个蜡烛条间隙）
                    startX = LEFT_MARGIN;
                } else {
                    // 数据超出图表范围，显示最新的部分（右侧）
                    startX = LEFT_MARGIN + DRAW_WIDTH - RIGHT_CANDLE_GAP - totalWidth;
                }
                
                // 查找鼠标悬停对应的K线索引
                int index = -1;
                if (mouseX >= LEFT_MARGIN && mouseX <= canvasWidth - RIGHT_MARGIN - RIGHT_CANDLE_GAP) {
                    double relativeX = mouseX - startX;
                    index = (int) (relativeX / PIXELS_PER_UNIT);
                }
                
                if (index >= 0 && index < kLines.size()) {
                    IntradayKLine kline = kLines.get(index);
                    
                    // 计算涨跌
                    double currentPrice = kline.getPrice().doubleValue();
                    double prevPrice = currentPrice;
                    if (index > 0) {
                        prevPrice = kLines.get(index - 1).getPrice().doubleValue();
                    }
                    boolean isRising = currentPrice >= prevPrice;
                    String changeStr = isRising ? "▲" : "▼";
                    
                    // 格式化时间
                    java.time.LocalDateTime time = kline.getTime();
                    String timeStr;
                    if (klineInterval >= 60) {
                        timeStr = String.format("%02d:%02d", time.getHour(), time.getMinute());
                    } else {
                        timeStr = String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
                    }
                    
                    // 设置提示内容
                    String tooltipText = String.format(
                        "时间: %s\n价格: %.2f %s",
                        timeStr,
                        currentPrice,
                        changeStr
                    );
                    tooltip.setText(tooltipText);
                    
                    // 显示提示
                    if (!tooltip.isShowing()) {
                        tooltip.show(candlestickChart, 
                            event.getScreenX() + 10, 
                            event.getScreenY() - 10);
                    } else {
                        tooltip.setX(event.getScreenX() + 10);
                        tooltip.setY(event.getScreenY() - 10);
                    }
                } else {
                    tooltip.hide();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        candlestickChart.setOnMouseExited(event -> {
            tooltip.hide();
        });
    }


    private VBox createView() {
        // 如果游戏未开始，根据showAsEnded决定显示哪个界面
        if (!stock.getIsTrading()) {
            // 显示结束游戏界面（带开始游戏按钮）
            return createGameEndedView();
        }
        
        // 游戏进行中，先创建view，再使用rebuildTradingView
        view = new VBox(10);
        view.setPadding(new Insets(10));
        rebuildTradingView();
        return view;
    }
    
    // 显示买入弹窗
    private void showBuyPopup() {
        Stage popupStage = new Stage();
        popupStage.setTitle("买入委托 - " + stock.getStockName());
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("买入委托");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ff3333;");
        
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton limitBtn = new RadioButton("限价");
        RadioButton marketBtn = new RadioButton("市价");
        limitBtn.setToggleGroup(typeGroup);
        marketBtn.setToggleGroup(typeGroup);
        limitBtn.setSelected(true);
        HBox typeBox = new HBox(20, limitBtn, marketBtn);
        
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER);
        Label priceLabel = new Label("价格:");
        priceLabel.setPrefWidth(50);
        TextField priceField = new TextField(stock.getCurrentPrice().toString());
        priceField.setPrefWidth(120);
        priceBox.getChildren().addAll(priceLabel, priceField);
        
        HBox qtyBox = new HBox(10);
        qtyBox.setAlignment(Pos.CENTER);
        Label qtyLabel = new Label("数量:");
        qtyLabel.setPrefWidth(50);
        TextField qtyField = new TextField("100");
        qtyField.setPrefWidth(120);
        qtyBox.getChildren().addAll(qtyLabel, qtyField);
        
        // 市价时禁用价格输入
        marketBtn.setOnAction(e -> priceField.setDisable(true));
        limitBtn.setOnAction(e -> priceField.setDisable(false));
        
        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);
        Button submitBtn = new Button("提交");
        submitBtn.setPrefWidth(80);
        submitBtn.setStyle("-fx-background-color: #ff3333; -fx-text-fill: white; -fx-font-weight: bold;");
        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(80);
        submitBtn.setOnAction(e -> {
            try {
                BigDecimal price = limitBtn.isSelected() ? 
                        new BigDecimal(priceField.getText()) : stock.getCurrentPrice();
                int qty = Integer.parseInt(qtyField.getText());
                Order.OrderPriceType priceType = limitBtn.isSelected() ? 
                        Order.OrderPriceType.LIMIT : Order.OrderPriceType.MARKET;
                
                tradingService.submitOrder(currentUser.getId(), stock.getId(), 
                        Order.OrderType.BUY, priceType, price, qty);
                popupStage.close();
                refresh();
            } catch (Exception ex) {
                showAlert("委托失败: " + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> popupStage.close());
        btnBox.getChildren().addAll(submitBtn, cancelBtn);
        
        root.getChildren().addAll(titleLabel, typeBox, priceBox, qtyBox, btnBox);
        
        Scene scene = new Scene(root, 280, 280);
        popupStage.setScene(scene);
        popupStage.show();
    }
    
    // 显示卖出弹窗
    private void showSellPopup() {
        Stage popupStage = new Stage();
        popupStage.setTitle("卖出委托 - " + stock.getStockName());
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("卖出委托");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00A000;");
        
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton limitBtn = new RadioButton("限价");
        RadioButton marketBtn = new RadioButton("市价");
        limitBtn.setToggleGroup(typeGroup);
        marketBtn.setToggleGroup(typeGroup);
        limitBtn.setSelected(true);
        HBox typeBox = new HBox(20, limitBtn, marketBtn);
        
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER);
        Label priceLabel = new Label("价格:");
        priceLabel.setPrefWidth(50);
        TextField priceField = new TextField(stock.getCurrentPrice().toString());
        priceField.setPrefWidth(120);
        priceBox.getChildren().addAll(priceLabel, priceField);
        
        HBox qtyBox = new HBox(10);
        qtyBox.setAlignment(Pos.CENTER);
        Label qtyLabel = new Label("数量:");
        qtyLabel.setPrefWidth(50);
        TextField qtyField = new TextField("100");
        qtyField.setPrefWidth(120);
        qtyBox.getChildren().addAll(qtyLabel, qtyField);
        
        // 市价时禁用价格输入
        marketBtn.setOnAction(e -> priceField.setDisable(true));
        limitBtn.setOnAction(e -> priceField.setDisable(false));
        
        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);
        Button submitBtn = new Button("提交");
        submitBtn.setPrefWidth(80);
        submitBtn.setStyle("-fx-background-color: #00A000; -fx-text-fill: white; -fx-font-weight: bold;");
        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(80);
        submitBtn.setOnAction(e -> {
            try {
                BigDecimal price = limitBtn.isSelected() ? 
                        new BigDecimal(priceField.getText()) : stock.getCurrentPrice();
                int qty = Integer.parseInt(qtyField.getText());
                Order.OrderPriceType priceType = limitBtn.isSelected() ? 
                        Order.OrderPriceType.LIMIT : Order.OrderPriceType.MARKET;
                
                tradingService.submitOrder(currentUser.getId(), stock.getId(), 
                        Order.OrderType.SELL, priceType, price, qty);
                popupStage.close();
                refresh();
            } catch (Exception ex) {
                showAlert("委托失败: " + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> popupStage.close());
        btnBox.getChildren().addAll(submitBtn, cancelBtn);
        
        root.getChildren().addAll(titleLabel, typeBox, priceBox, qtyBox, btnBox);
        
        Scene scene = new Scene(root, 280, 280);
        popupStage.setScene(scene);
        popupStage.show();
    }
    
    private void startRefreshTimer() {
        refreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> Platform.runLater(() -> {
                    try {
                        refresh();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                })
            )
        );
        refreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimeline.play();
    }
    
    private void updatePriceLabel(Stock s) {
        BigDecimal openPrice = s.getPreviousClose();
        BigDecimal currentPrice = s.getCurrentPrice();
        BigDecimal change = currentPrice.subtract(openPrice);
        BigDecimal changePercent = change.divide(openPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        String changeStr = String.format("%.2f%%", changePercent);
        String changeColor = change.compareTo(BigDecimal.ZERO) >= 0 ? "#ff3333" : "#00A000";
        
        // 左侧部分：当前价格和涨跌幅，固定100px宽度
        javafx.scene.text.TextFlow leftFlow = new javafx.scene.text.TextFlow();
        
        javafx.scene.text.Text t1 = new javafx.scene.text.Text("当前价格: ");
        t1.setFill(javafx.scene.paint.Color.BLACK);
        
        javafx.scene.text.Text t2 = new javafx.scene.text.Text(currentPrice.toString());
        t2.setFill(javafx.scene.paint.Color.web(changeColor));
        
        javafx.scene.text.Text t3 = new javafx.scene.text.Text("(");
        t3.setFill(javafx.scene.paint.Color.BLACK);
        
        javafx.scene.text.Text t4 = new javafx.scene.text.Text(changeStr);
        t4.setFill(javafx.scene.paint.Color.web(changeColor));
        
        javafx.scene.text.Text t5 = new javafx.scene.text.Text(")");
        t5.setFill(javafx.scene.paint.Color.BLACK);
        
        leftFlow.getChildren().addAll(t1, t2, t3, t4, t5);
        leftFlow.setPrefWidth(180);
        
        // 右侧：开盘价
        javafx.scene.text.TextFlow rightFlow = new javafx.scene.text.TextFlow();
        
        javafx.scene.text.Text t6 = new javafx.scene.text.Text("开盘价:");
        t6.setFill(javafx.scene.paint.Color.BLACK);
        
        javafx.scene.text.Text t7 = new javafx.scene.text.Text(openPrice.toString());
        t7.setFill(javafx.scene.paint.Color.BLACK);
        
        rightFlow.getChildren().addAll(t6, t7);
        
        // 用HBox组合左右两部分
        HBox container = new HBox(0);
        container.getChildren().addAll(leftFlow, rightFlow);
        
        priceLabel.setGraphic(container);
    }
    
    public void refresh() throws SQLException {
        // 安全检查：确保UI组件已初始化
        if (priceLabel == null || orderTable == null) {
            return;
        }
        
        // 更新价格
        Stock updatedStock = tradingService.getStockById(stock.getId());
        
        updatePriceLabel(updatedStock);
        
        // 更新蜡烛图
        updateCandlestickChart();
        
        // 更新挂单列表 - 保存选中状态
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
        Long selectedOrderId = selectedOrder != null ? selectedOrder.getId() : null;
        
        List<Order> orders = tradingService.getUserOrders(currentUser.getId());
        orderTable.getItems().setAll(
            orders.stream()
                .filter(o -> o.getStockId().equals(stock.getId()) && 
                        o.getStatus() == Order.OrderStatus.PENDING)
                .collect(java.util.stream.Collectors.toList())
        );
        
        // 恢复选中状态
        if (selectedOrderId != null) {
            for (int i = 0; i < orderTable.getItems().size(); i++) {
                if (orderTable.getItems().get(i).getId().equals(selectedOrderId)) {
                    orderTable.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
    
    // 更新蜡烛图
    private void updateCandlestickChart() throws SQLException {

        // 获取该股票的所有分时数据（不依赖用户订单）
        List<IntradayKLine> kLines = tradingService.getAllIntradayKLines(stock.getId());
        // 根据klineInterval聚合数据
        List<IntradayKLine> aggregatedKLines = aggregateKLines(kLines, klineInterval);
        
        // 更新滚动条范围
        if (klineScrollBar != null && candlestickChart != null) {
            double totalWidth = aggregatedKLines.size() * 8.0;
            double drawWidth = candlestickChart.getWidth() - 60 - 15;
            double rightGap = 8.0 * 4;
            double maxScroll = Math.max(0, totalWidth - drawWidth + rightGap);
            
            if (maxScroll > 0) {
                updatingScrollBar = true;
                klineScrollBar.setMax(maxScroll);
                klineScrollBar.setVisibleAmount(50);
                klineScrollBar.setDisable(false);
                klineScrollBar.setVisible(true);
                
                // 如果用户没有手动拖动，始终保持在最右侧
                if (!userScrollingKline) {
                    klineScrollBar.setValue(maxScroll);
                }
                updatingScrollBar = false;
            } else {
                klineScrollBar.setVisible(false);
                klineScrollBar.setMax(0);
                userScrollingKline = false;
            }
        }
        
        drawCandlestickChart(aggregatedKLines);
    }
    
    // 根据时间间隔聚合K线数据
    private List<IntradayKLine> aggregateKLines(List<IntradayKLine> kLines, int intervalSeconds) {
        if (kLines == null || kLines.isEmpty() || intervalSeconds <= 1) {
            return kLines;
        }
        
        List<IntradayKLine> result = new java.util.ArrayList<>();
        
        // 按时间间隔分组
        java.util.Map<Long, java.util.List<IntradayKLine>> groups = new java.util.HashMap<>();
        
        for (IntradayKLine line : kLines) {
            // 计算该数据点属于哪个时间组（从游戏开始时间计算的秒数）
            long secondsFromStart = line.getTime().toEpochSecond(java.time.ZoneOffset.UTC);
            long groupKey = secondsFromStart / intervalSeconds;
            
            groups.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(line);
        }
        
        // 对每个组计算OHLC
        java.util.List<Long> sortedKeys = new java.util.ArrayList<>(groups.keySet());
        java.util.Collections.sort(sortedKeys);
        
        for (Long key : sortedKeys) {
            java.util.List<IntradayKLine> group = groups.get(key);
            if (group.isEmpty()) continue;
            
            // 计算该组的OHLC
            IntradayKLine aggregated = new IntradayKLine();
            aggregated.setStockId(group.get(0).getStockId());
            aggregated.setTime(group.get(group.size() / 2).getTime()); // 取中间时间
            
            // Open: 第一个价格, High: 最高价格, Low: 最低价格, Close: 最后一个价格
            double open = group.get(0).getPrice().doubleValue();
            double close = group.get(group.size() - 1).getPrice().doubleValue();
            double high = open;
            double low = open;
            long volume = 0;
            
            for (IntradayKLine line : group) {
                double price = line.getPrice().doubleValue();
                high = Math.max(high, price);
                low = Math.min(low, price);
                volume += line.getVolume();
            }
            
            aggregated.setPrice(group.get(group.size() - 1).getPrice()); // 收盘价作为price
            aggregated.setVolume(volume);
            aggregated.setGameSessionId(group.get(0).getGameSessionId());
            
            result.add(aggregated);
        }
        
        return result;
    }
    
    // 绘制专业分时K线图 - 开盘价固定中间，Y轴随价格波动动态调整
    private void drawCandlestickChart(List<IntradayKLine> kLines) {
        GraphicsContext gc = candlestickChart.getGraphicsContext2D();
        
        // 获取Canvas实际尺寸
        double canvasWidth = candlestickChart.getWidth();
        double canvasHeight = candlestickChart.getHeight();
        
        // 清空画布
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        
        if (kLines == null || kLines.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("暂无数据", canvasWidth / 2 - 30, canvasHeight / 2);
            return;
        }
        
        // 图表参数 - 使用实际Canvas尺寸
        final double LEFT_MARGIN = 60;
        final double RIGHT_MARGIN = 15;
        final double TOP_MARGIN = 15;
        final double BOTTOM_MARGIN = 25;
        final double PIXELS_PER_UNIT = 8.0;
        final double DRAW_WIDTH = canvasWidth - LEFT_MARGIN - RIGHT_MARGIN;
        // 最新蜡烛条右边保留4个蜡烛条宽度（仅在滚动到最右侧时）
        double maxScrollForGap = Math.max(0, kLines.size() * PIXELS_PER_UNIT - DRAW_WIDTH);
        double currentScroll = klineScrollBar != null ? klineScrollBar.getValue() : 0;
        final double RIGHT_CANDLE_GAP;
        if (maxScrollForGap > 0 && Math.abs(currentScroll - maxScrollForGap) < 1) {
            RIGHT_CANDLE_GAP = PIXELS_PER_UNIT * 4;
        } else {
            RIGHT_CANDLE_GAP = 0;
        }
        final double DRAW_HEIGHT = canvasHeight - TOP_MARGIN - BOTTOM_MARGIN;
        final double CANDLE_WIDTH = PIXELS_PER_UNIT * 0.7;
        
        // 获取开盘价（第一个价格）作为基准
        double openPrice = kLines.get(0).getPrice().doubleValue();
        
        // 获取当前所有价格的最大偏离值
        double maxDeviation = 0;
        double currentMax = openPrice;
        double currentMin = openPrice;
        
        for (IntradayKLine line : kLines) {
            double price = line.getPrice().doubleValue();
            double deviation = Math.abs(price - openPrice);
            maxDeviation = Math.max(maxDeviation, deviation);
            currentMax = Math.max(currentMax, price);
            currentMin = Math.min(currentMin, price);
        }
        
        // 最小显示范围（确保即使波动很小时也能看清）
        double minDisplayRange = openPrice * 0.02; // 至少显示2%的范围
        
        // 动态计算Y轴范围 - 以开盘价为基准，对称扩展
        // 随着价格波动，范围逐渐扩大，但始终保持在图表中心
        double displayRange = Math.max(maxDeviation * 1.2, minDisplayRange / 2);
        double yAxisMax = openPrice + displayRange;
        double yAxisMin = openPrice - displayRange;
        double yAxisRange = yAxisMax - yAxisMin;
        
        // 计算开盘价的Y坐标（应该在图表正中间）
        double yOpen = TOP_MARGIN + (yAxisMax - openPrice) / yAxisRange * DRAW_HEIGHT;
        
        // 计算显示范围 - 根据滚动条位置计算起始X坐标
        double totalWidth = kLines.size() * PIXELS_PER_UNIT;
        double maxScrollVal = Math.max(0, totalWidth - DRAW_WIDTH + RIGHT_CANDLE_GAP);
        
        double scrollValue = klineScrollBar != null ? klineScrollBar.getValue() : maxScrollVal;
        
        // 绘制背景网格和坐标轴
        drawGridWithOpenPrice(gc, LEFT_MARGIN, TOP_MARGIN, DRAW_WIDTH, DRAW_HEIGHT, BOTTOM_MARGIN,
                yAxisMin, yAxisMax, yAxisRange, openPrice, yOpen, kLines, klineInterval, canvasWidth, scrollValue, maxScrollVal);
        
        double startX;
        if (totalWidth <= DRAW_WIDTH - RIGHT_CANDLE_GAP) {
            startX = LEFT_MARGIN;
        } else {
            startX = LEFT_MARGIN - scrollValue;
        }
        
        // 绘制K线
        for (int i = 0; i < kLines.size(); i++) {
            double x = startX + i * PIXELS_PER_UNIT + PIXELS_PER_UNIT / 2;
            
            // 只绘制在可见区域内的蜡烛
            if (x < LEFT_MARGIN - PIXELS_PER_UNIT || x > canvasWidth - RIGHT_MARGIN - RIGHT_CANDLE_GAP + PIXELS_PER_UNIT) {
                continue;
            }
            
            IntradayKLine currentLine = kLines.get(i);
            double currentPrice = currentLine.getPrice().doubleValue();
            
            // 获取前一个价格
            double prevPrice = (i > 0) ? kLines.get(i - 1).getPrice().doubleValue() : currentPrice;
            
            // 计算Y坐标
            double yCurrent = TOP_MARGIN + (yAxisMax - currentPrice) / yAxisRange * DRAW_HEIGHT;
            double yPrev = TOP_MARGIN + (yAxisMax - prevPrice) / yAxisRange * DRAW_HEIGHT;
            
            // 涨跌颜色
            boolean isRising = currentPrice >= prevPrice;
            Color color = isRising ? Color.web("#ff3333") : Color.web("#00A000");
            
            // 绘制影线
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeLine(x, Math.min(yCurrent, yPrev) - 2, x, Math.max(yCurrent, yPrev) + 2);
            
            // 绘制实体
            double bodyTop = Math.min(yCurrent, yPrev);
            double bodyBottom = Math.max(yCurrent, yPrev);
            double bodyHeight = Math.max(bodyBottom - bodyTop, 1);
            
            gc.setFill(color);
            gc.fillRect(x - CANDLE_WIDTH / 2, bodyTop, CANDLE_WIDTH, bodyHeight);
        }
        
        // 绘制最新价格指示线
        if (!kLines.isEmpty()) {
            double lastPrice = kLines.get(kLines.size() - 1).getPrice().doubleValue();
            double yLast = TOP_MARGIN + (yAxisMax - lastPrice) / yAxisRange * DRAW_HEIGHT;
            
            // 计算最后一个蜡烛的位置
            double lastX = startX + (kLines.size() - 1) * PIXELS_PER_UNIT + PIXELS_PER_UNIT / 2;
            
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1);
            gc.setLineDashes(5, 5);
            gc.strokeLine(LEFT_MARGIN, yLast, canvasWidth - RIGHT_MARGIN, yLast);
            gc.setLineDashes();
            
            // 数值显示在纵坐标左边，与刻度对齐
            gc.setFill(Color.YELLOW);
            gc.fillText(String.format("%.2f", lastPrice), 5, yLast + 4);
        }
    }
    
    // 绘制背景网格
    private void drawGrid(GraphicsContext gc, double left, double top, double width, double height, 
                         double minPrice, double maxPrice, double priceRange) {
        gc.setStroke(Color.web("#1f1f1f"));
        gc.setLineWidth(0.5);
        
        // 水平网格线 - 5条
        for (int i = 0; i <= 4; i++) {
            double y = top + i * height / 4.0;
            gc.strokeLine(left, y, left + width, y);
            
            // 价格标签
            double price = maxPrice - i * priceRange / 4.0;
            gc.setFill(Color.web("#666"));
            gc.fillText(String.format("%.2f", price), 5, (int)y + 4);
        }
        
        // 垂直网格线 - 时间刻度
        for (int i = 0; i <= 6; i++) {
            double x = left + i * width / 6.0;
            gc.strokeLine(x, top, x, top + height);
        }
        
        // 绘制边框
        gc.setStroke(Color.web("#444"));
        gc.setLineWidth(1);
        gc.strokeRect(left, top, width, height);
    }
    
    // 绘制背景网格和横坐标时间轴
    private void drawGridWithTimeAxis(GraphicsContext gc, double left, double top, double width, double height, 
                                      double bottomMargin, double minPrice, double maxPrice, double priceRange,
                                      List<IntradayKLine> kLines, int intervalSeconds) {
        gc.setStroke(Color.web("#1f1f1f"));
        gc.setLineWidth(0.5);
        
        // 水平网格线 - 5条
        for (int i = 0; i <= 4; i++) {
            double y = top + i * height / 4.0;
            gc.strokeLine(left, y, left + width, y);
            
            // 价格标签
            double price = maxPrice - i * priceRange / 4.0;
            gc.setFill(Color.web("#666"));
            gc.fillText(String.format("%.2f", price), 5, (int)y + 4);
        }
        
        // 垂直网格线和时间刻度
        int timeLabelInterval = 10; // 每10个蜡烛显示一个时间标签
        double pixelsPerUnit = 8.0;
        double totalWidth = kLines.size() * pixelsPerUnit;
        double startX = left + width - totalWidth;
        
        for (int i = 0; i < kLines.size(); i += timeLabelInterval) {
            double x = startX + i * pixelsPerUnit + pixelsPerUnit / 2;
            
            if (x >= left && x <= left + width) {
                // 绘制垂直网格线
                gc.strokeLine(x, top, x, top + height);
                
                // 绘制时间标签
                IntradayKLine kline = kLines.get(i);
                java.time.LocalDateTime time = kline.getTime();
                String timeStr;
                
                if (intervalSeconds >= 60) {
                    // 分钟级别显示 时:分
                    timeStr = String.format("%02d:%02d", time.getHour(), time.getMinute());
                } else {
                    // 秒级别显示 分:秒
                    timeStr = String.format("%02d:%02d", time.getMinute(), time.getSecond());
                }
                
                gc.setFill(Color.web("#666"));
                gc.fillText(timeStr, (int)x - 15, (int)(top + height + 15));
            }
        }
        
        // 绘制X轴线
        gc.setStroke(Color.web("#444"));
        gc.setLineWidth(1);
        gc.strokeLine(left, top + height, left + width, top + height);
        
        // 绘制Y轴线
        gc.strokeLine(left, top, left, top + height);
    }
    
    // 绘制背景网格和开盘线（开盘线固定在中间）
    private void drawGridWithOpenPrice(GraphicsContext gc, double left, double top, double width, double height,
                                       double bottomMargin, double minPrice, double maxPrice, double priceRange,
                                       double openPrice, double yOpen, List<IntradayKLine> kLines, int intervalSeconds, 
                                       double canvasWidth, double scrollValue, double maxScrollVal) {
        gc.setStroke(Color.web("#1f1f1f"));
        gc.setLineWidth(0.5);
        
        // 水平网格线 - 以开盘线为中心对称分布
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            // 计算相对于开盘线的偏移
            double ratio = (double) i / gridLines; // 0到1
            double price;
            if (i <= gridLines / 2) {
                // 上半部分（高于开盘价）
                price = openPrice + priceRange * 0.5 * (1 - ratio * 2);
            } else {
                // 下半部分（低于开盘价）
                price = openPrice - priceRange * 0.5 * ((ratio - 0.5) * 2);
            }
            
            double y = top + (maxPrice - price) / priceRange * height;
            gc.strokeLine(left, y, left + width, y);
            
            // 价格标签
            gc.setFill(Color.web("#666"));
            gc.fillText(String.format("%.2f", price), 5, (int)y + 4);
        }
        
        // 绘制开盘线（白色虚线，固定在图表中间）
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.setLineDashes(10, 5);
        gc.strokeLine(left, yOpen, left + width, yOpen);
        gc.setLineDashes();
        
        // 显示开盘价数值 - 放在左侧Y轴刻度区域内
        gc.setFill(Color.WHITE);
        gc.fillText(String.format("%.2f", openPrice), 5, (int)yOpen + 4);
        
        // 垂直网格线和时间刻度 - 与K线绘制使用相同的startX逻辑
        int timeLabelInterval = 10;
        double pixelsPerUnit = 8.0;
        double totalWidth = kLines.size() * pixelsPerUnit;
        double rightGap = maxScrollVal > 0 ? pixelsPerUnit * 4 : 0;
        
        double startX;
        if (totalWidth <= width - rightGap) {
            startX = left;
        } else {
            startX = left - scrollValue;
        }
        
        for (int i = 0; i < kLines.size(); i += timeLabelInterval) {
            double x = startX + i * pixelsPerUnit + pixelsPerUnit / 2;
            
            if (x >= left && x <= left + width) {
                gc.setStroke(Color.web("#1f1f1f"));
                gc.strokeLine(x, top, x, top + height);
                
                IntradayKLine kline = kLines.get(i);
                java.time.LocalDateTime time = kline.getTime();
                String timeStr;
                
                if (intervalSeconds >= 60) {
                    timeStr = String.format("%02d:%02d", time.getHour(), time.getMinute());
                } else {
                    timeStr = String.format("%02d:%02d", time.getMinute(), time.getSecond());
                }
                
                gc.setFill(Color.web("#666"));
                gc.fillText(timeStr, (int)x - 15, (int)(top + height + 15));
            }
        }
        
        // 绘制边框
        gc.setStroke(Color.web("#444"));
        gc.setLineWidth(1);
        gc.strokeRect(left, top, width, height);
    }
    
    // 绘制移动平均线
    private void drawAverageLine(GraphicsContext gc, List<IntradayKLine> kLines, double startX, double candleWidth,
                                 double left, double top, double height, double minPrice, double priceRange) {
        gc.setStroke(Color.web("#ffff00"));
        gc.setLineWidth(1.5);
        
        int maPeriod = 5;  // 5周期均线
        
        for (int i = maPeriod - 1; i < kLines.size(); i++) {
            // 计算MA5
            double sum = 0;
            for (int j = i - maPeriod + 1; j <= i; j++) {
                sum += kLines.get(j).getPrice().doubleValue();
            }
            double ma = sum / maPeriod;
            double y = top + (minPrice + priceRange - ma) / priceRange * height;
            double x = startX + i * candleWidth + candleWidth / 2;
            
            if (i == maPeriod - 1) {
                gc.beginPath();
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();
    }
    
    private void cancelSelectedOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("请选择要撤销的挂单");
            return;
        }
        
        try {
            tradingService.cancelOrder(selected.getId());
            showAlert("挂单已撤销");
            refresh();
        } catch (Exception e) {
            showAlert("撤销失败: " + e.getMessage());
        }
    }
    
    private void endGame() {
        try {
            // 结束游戏
            tradingService.endGame(stock.getId());
            
            // 停止定时器
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }
            
            // 刷新股票状态
            Stock updatedStock = tradingService.getStockById(stock.getId());
            this.stock = updatedStock;
            
            // 改变按钮文本
            startEndGameBtn.setText("开始游戏");
            startEndGameBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
            startEndGameBtn.setOnAction(e -> startGame());
            
            // 显示历史K线图
            showGameEndedView();
            
        } catch (Exception e) {
            showAlert("结束游戏失败: " + e.getMessage());
        }
    }
    
    // 重新开始游戏
    private void startGame() {
        try {
            tradingService.startGame(stock.getId());
            sessionId = getLastSessionId();
            // 刷新股票状态
            Stock updatedStock = tradingService.getStockById(stock.getId());
            this.stock = updatedStock;
            
            // 重新创建完整的交易界面
            view.getChildren().clear();
            rebuildTradingView();
            
            // 启动定时器
            startRefreshTimer();
            
        } catch (Exception e) {
            showAlert("开始游戏失败: " + e.getMessage());
        }
    }
    
    // 重建交易界面
    private void rebuildTradingView() {
        // 顶部区域：左侧标题和价格，右侧按钮
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        
        // 左侧：标题和价格
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(stock.getStockName() + " (" + stock.getStockCode() + ")");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        priceLabel = new Label();
        priceLabel.setStyle("-fx-font-size: 14px;");
        updatePriceLabel(stock);
        titleBox.getChildren().addAll(titleLabel, priceLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        // 右侧：操作按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button buyPopupBtn = new Button("买入");
        buyPopupBtn.setPrefWidth(80);
        buyPopupBtn.setPrefHeight(35);
        buyPopupBtn.setStyle("-fx-background-color: #ff3333; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        buyPopupBtn.setOnAction(e -> showBuyPopup());
        
        Button sellPopupBtn = new Button("卖出");
        sellPopupBtn.setPrefWidth(80);
        sellPopupBtn.setPrefHeight(35);
        sellPopupBtn.setStyle("-fx-background-color: #00A000; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        sellPopupBtn.setOnAction(e -> showSellPopup());
        
        Button viewKLineBtn = new Button("查看K线");
        viewKLineBtn.setPrefWidth(80);
        viewKLineBtn.setPrefHeight(35);
        viewKLineBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px;");
        viewKLineBtn.setOnAction(e -> showKLineView());
        
        // 重新开始/结束游戏按钮
        startEndGameBtn = new Button("结束游戏");
        startEndGameBtn.setPrefWidth(80);
        startEndGameBtn.setPrefHeight(35);
        startEndGameBtn.setOnAction(e -> endGame());
        
        buttonBox.getChildren().addAll(buyPopupBtn, sellPopupBtn, viewKLineBtn, startEndGameBtn);
        topPanel.getChildren().addAll(titleBox, buttonBox);
        
        // 中间：蜡烛图区域
        chartContainer = new Pane();
        chartContainer.setPrefHeight(CHART_HEIGHT);
        chartContainer.setMinHeight(CHART_HEIGHT);
        chartContainer.setStyle("-fx-background-color: #0a0a0a;");
        VBox.setVgrow(chartContainer, Priority.ALWAYS);
        
        // K线类型选择
        HBox klineTypeBox = new HBox(5);
        klineTypeBox.setAlignment(Pos.CENTER);
        
        ToggleGroup klineGroup = new ToggleGroup();
        
        ToggleButton sec1Btn = createKlineTypeButton("1秒", 1, klineGroup);
        ToggleButton sec5Btn = createKlineTypeButton("5秒", 5, klineGroup);
        ToggleButton sec10Btn = createKlineTypeButton("10秒", 10, klineGroup);
        ToggleButton sec30Btn = createKlineTypeButton("30秒", 30, klineGroup);
        ToggleButton min1Btn = createKlineTypeButton("1分", 60, klineGroup);
        ToggleButton min5Btn = createKlineTypeButton("5分", 300, klineGroup);
        ToggleButton min10Btn = createKlineTypeButton("10分", 600, klineGroup);
        ToggleButton min30Btn = createKlineTypeButton("30分", 1800, klineGroup);
        ToggleButton hour1Btn = createKlineTypeButton("1时", 3600, klineGroup);
        
        sec1Btn.setSelected(true);
        
        klineTypeBox.getChildren().addAll(sec1Btn, sec5Btn, sec10Btn, sec30Btn, min1Btn, min5Btn, min10Btn, min30Btn, hour1Btn);
        
        Label chartTitle = new Label("分时走势图");
        chartTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        candlestickChart = new Canvas(800, CHART_HEIGHT);
        
        // 监听容器大小变化，调整Canvas大小
        chartContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                candlestickChart.setWidth(newVal.doubleValue());
                try {
                    updateCandlestickChart();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        chartContainer.getChildren().add(candlestickChart);
        
        // 添加K线图滚动条
        klineScrollBar = new ScrollBar();
        klineScrollBar.setOrientation(Orientation.HORIZONTAL);
        klineScrollBar.setMin(0);
        klineScrollBar.setMax(0);
        klineScrollBar.setValue(0);
        klineScrollBar.setVisible(false);
        
        // 监听滚动条变化
        klineScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingScrollBar) {
                userScrollingKline = true;
            }
            try {
                updateCandlestickChart();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        // 图表和滚动条使用VBox组合
        VBox chartBox = new VBox(0);
        chartBox.getChildren().addAll(chartContainer, klineScrollBar);
        
        // 滚动条高度
        klineScrollBar.setPrefHeight(15);
        
        // 添加鼠标悬停提示
        setupCandlestickChartHover();
        
        // 下方：挂单列表
        VBox orderPanel = new VBox(5);
        
        // 挂单列表标题和撤销按钮
        HBox orderHeader = new HBox(10);
        orderHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label orderListLabel = new Label("当前挂单");
        orderListLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(orderListLabel, Priority.ALWAYS);
        
        Button cancelBtn = new Button("撤销选中挂单");
        cancelBtn.setOnAction(e -> cancelSelectedOrder());
        
        orderHeader.getChildren().addAll(orderListLabel, cancelBtn);
        
        orderTable = new TableView<>();
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.setPrefHeight(180);
        
        TableColumn<Order, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getOrderType().name()));
        
        TableColumn<Order, String> priceTypeCol = new TableColumn<>("价格类型");
        priceTypeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPriceType().name()));
        
        TableColumn<Order, String> priceCol = new TableColumn<>("价格");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPrice().toString()));
        
        TableColumn<Order, String> qtyCol = new TableColumn<>("数量");
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantity())));
        
        TableColumn<Order, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStatus().name()));
        
        orderTable.getColumns().addAll(typeCol, priceTypeCol, priceCol, qtyCol, statusCol);
        
        orderPanel.getChildren().addAll(orderHeader, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        
        // 组装主界面
        view.getChildren().addAll(topPanel, klineTypeBox, chartTitle, chartBox, orderPanel);
        VBox.setVgrow(chartBox, Priority.ALWAYS);
    }
    
    // 创建游戏结束后的界面（历史K线图）- 返回VBox供createView使用
    private VBox createGameEndedView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // 查询最后一次游戏的sessionId（该用户该股票的最后一次游戏）
        sessionId = getLastSessionId();
        
        // 顶部区域
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(stock.getStockName() + " (" + stock.getStockCode() + ")");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        priceLabel = new Label();
        priceLabel.setStyle("-fx-font-size: 14px;");
        updatePriceLabel(stock);
        titleBox.getChildren().addAll(titleLabel, priceLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        // 右侧按钮区域（结束游戏界面只显示查看K线和开始游戏按钮）
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button viewKLineBtn = new Button("查看K线");
        viewKLineBtn.setPrefWidth(80);
        viewKLineBtn.setPrefHeight(35);
        viewKLineBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px;");
        viewKLineBtn.setOnAction(e -> showKLineView());
        
        // 创建开始游戏按钮
        Button startGameBtn = new Button("开始游戏");
        startGameBtn.setPrefWidth(100);
        startGameBtn.setPrefHeight(35);
        startGameBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        startGameBtn.setOnAction(e -> startGame());
        
        buttonBox.getChildren().addAll(viewKLineBtn, startGameBtn);
        topPanel.getChildren().addAll(titleBox, buttonBox);
        
        // 中间区域：显示历史K线图（带K线类型选择和鼠标悬停功能）
        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        
        Label historyLabel = new Label("本局游戏走势图");
        historyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        // K线类型选择按钮（使用历史数据专用按钮）
        HBox klineTypeBox = new HBox(5);
        klineTypeBox.setAlignment(Pos.CENTER);
        
        ToggleGroup klineGroup = new ToggleGroup();
        
        ToggleButton sec1Btn = createHistoricalKlineTypeButton("1秒", 1, klineGroup, sessionId);
        ToggleButton sec5Btn = createHistoricalKlineTypeButton("5秒", 5, klineGroup, sessionId);
        ToggleButton sec10Btn = createHistoricalKlineTypeButton("10秒", 10, klineGroup, sessionId);
        ToggleButton sec30Btn = createHistoricalKlineTypeButton("30秒", 30, klineGroup, sessionId);
        ToggleButton min1Btn = createHistoricalKlineTypeButton("1分", 60, klineGroup, sessionId);
        ToggleButton min5Btn = createHistoricalKlineTypeButton("5分", 300, klineGroup, sessionId);
        ToggleButton min10Btn = createHistoricalKlineTypeButton("10分", 600, klineGroup, sessionId);
        ToggleButton min30Btn = createHistoricalKlineTypeButton("30分", 1800, klineGroup, sessionId);
        ToggleButton hour1Btn = createHistoricalKlineTypeButton("1时", 3600, klineGroup, sessionId);
        
        sec1Btn.setSelected(true);
        
        klineTypeBox.getChildren().addAll(sec1Btn, sec5Btn, sec10Btn, sec30Btn, min1Btn, min5Btn, min10Btn, min30Btn, hour1Btn);
        
        // 创建Canvas容器
        Pane chartContainer = new Pane();
        chartContainer.setPrefHeight(350);
        chartContainer.setMinHeight(350);
        chartContainer.setStyle("-fx-background-color: #0a0a0a;");
        
        // 创建Canvas显示历史K线
        candlestickChart = new Canvas(800, 350);
        
        // 监听容器大小变化
        chartContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                candlestickChart.setWidth(newVal.doubleValue());
                refreshHistoricalChart(sessionId);
            }
        });
        
        chartContainer.getChildren().add(candlestickChart);
        
        // 添加鼠标悬停提示
        setupHistoricalCandlestickChartHover(sessionId);
        
        // 加载并显示历史数据
        refreshHistoricalChart(sessionId);
        
        centerBox.getChildren().addAll(historyLabel, klineTypeBox, chartContainer);
        
        // 下方：显示本局成交记录统计
        VBox statsPanel = new VBox(5);
        
        Label statsLabel = new Label("本局交易统计");
        statsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        try {
            List<TradeRecord> tradeRecords = tradingService.getUserTradeRecords(currentUser.getId());
            // 根据sessionId筛选本局交易记录
            long buyCount, sellCount;
            if (sessionId != null) {
                buyCount = tradeRecords.stream()
                    .filter(r -> r.getTradeType() == Order.OrderType.BUY
                        && r.getStockId().equals(stock.getId())
                        && sessionId.equals(r.getGameSessionId()))
                    .count();
                sellCount = tradeRecords.stream()
                    .filter(r -> r.getTradeType() == Order.OrderType.SELL
                        && r.getStockId().equals(stock.getId())
                        && sessionId.equals(r.getGameSessionId()))
                    .count();
            } else {
                // 如果没有sessionId，显示无数据
                buyCount = 0;
                sellCount = 0;
            }
            
            Label statsContent = new Label(String.format("买入: %d 笔, 卖出: %d 笔", buyCount, sellCount));
            statsContent.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            statsPanel.getChildren().addAll(statsLabel, statsContent);
        } catch (SQLException e) {
            Label errorLabel = new Label("加载交易统计失败");
            errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff0000;");
            statsPanel.getChildren().addAll(statsLabel, errorLabel);
        }
        
        // 状态标签
        Label statusLabel = new Label("游戏已结束 - 点击开始游戏进行新一轮交易");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        root.getChildren().addAll(topPanel, centerBox, statsPanel, statusLabel);
        
        return root;
    }
    
    // 显示游戏结束后的界面（供内部调用，复用createGameEndedView）
    private void showGameEndedView() {
        // 清空并重新构建视图
        view.getChildren().clear();
        VBox endedView = createGameEndedView();
        view.getChildren().addAll(endedView.getChildren());
    }
    
    // 刷新历史K线图（只显示本局数据）
    private void refreshHistoricalChart(Long sessionId) {
        if (sessionId == null) {
            GraphicsContext gc = candlestickChart.getGraphicsContext2D();
            gc.setFill(Color.web("#0a0a0a"));
            gc.fillRect(0, 0, candlestickChart.getWidth(), candlestickChart.getHeight());
            gc.setFill(Color.GRAY);
            gc.fillText("暂无历史数据", candlestickChart.getWidth() / 2 - 40, candlestickChart.getHeight() / 2);
            return;
        }
        
        try {
            List<IntradayKLine> historicalKLines = tradingService.getIntradayKLinesBySession(stock.getId(), sessionId);

            if (historicalKLines != null && !historicalKLines.isEmpty()) {
                // 根据当前klineInterval聚合数据
                List<IntradayKLine> aggregatedKLines = aggregateKLines(historicalKLines, klineInterval);
                drawCandlestickChart(aggregatedKLines);
            } else {
                GraphicsContext gc = candlestickChart.getGraphicsContext2D();
                gc.setFill(Color.web("#0a0a0a"));
                gc.fillRect(0, 0, candlestickChart.getWidth(), candlestickChart.getHeight());
                gc.setFill(Color.GRAY);
                gc.fillText("暂无历史数据", candlestickChart.getWidth() / 2 - 40, candlestickChart.getHeight() / 2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // 设置历史K线图鼠标悬停提示（只显示本局数据）
    private void setupHistoricalCandlestickChartHover(Long sessionId) {
        Tooltip tooltip = new Tooltip();
        tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #333; -fx-text-fill: white;");
        
        candlestickChart.setOnMouseMoved(event -> {
            try {
                // 获取本局游戏的历史K线数据
                List<IntradayKLine> kLines = tradingService.getIntradayKLinesBySession(stock.getId(), sessionId);
                if (kLines == null || kLines.isEmpty()) {
                    tooltip.hide();
                    return;
                }
                
                // 根据当前klineInterval聚合数据
                kLines = aggregateKLines(kLines, klineInterval);
                
                double mouseX = event.getX();
                double mouseY = event.getY();
                
                // 获取实际画布尺寸
                double canvasWidth = candlestickChart.getWidth();
                
                // 图表参数（与绘制时保持一致）
                final double LEFT_MARGIN = 60;
                final double RIGHT_MARGIN = 15;
                final double PIXELS_PER_UNIT = 8.0;
                final double DRAW_WIDTH = canvasWidth - LEFT_MARGIN - RIGHT_MARGIN;
                
                // 计算显示范围（与绘制时保持一致）
                double totalWidth = kLines.size() * PIXELS_PER_UNIT;
                double startX;
                if (totalWidth <= DRAW_WIDTH) {
                    startX = LEFT_MARGIN;
                } else {
                    startX = LEFT_MARGIN + DRAW_WIDTH - totalWidth;
                }
                
                // 查找鼠标悬停对应的K线索引
                int index = -1;
                if (mouseX >= LEFT_MARGIN && mouseX <= canvasWidth - RIGHT_MARGIN) {
                    double relativeX = mouseX - startX;
                    index = (int) (relativeX / PIXELS_PER_UNIT);
                }
                
                if (index >= 0 && index < kLines.size()) {
                    IntradayKLine kline = kLines.get(index);
                    
                    // 计算涨跌
                    double currentPrice = kline.getPrice().doubleValue();
                    double prevPrice = currentPrice;
                    if (index > 0) {
                        prevPrice = kLines.get(index - 1).getPrice().doubleValue();
                    }
                    boolean isRising = currentPrice >= prevPrice;
                    String changeStr = isRising ? "▲" : "▼";
                    
                    // 格式化时间
                    java.time.LocalDateTime time = kline.getTime();
                    String timeStr;
                    if (klineInterval >= 60) {
                        timeStr = String.format("%02d:%02d", time.getHour(), time.getMinute());
                    } else {
                        timeStr = String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
                    }
                    
                    // 设置提示内容
                    String tooltipText = String.format(
                        "时间: %s\n价格: %.2f %s",
                        timeStr,
                        currentPrice,
                        changeStr
                    );
                    tooltip.setText(tooltipText);
                    
                    // 显示提示
                    if (!tooltip.isShowing()) {
                        tooltip.show(candlestickChart, 
                            event.getScreenX() + 10, 
                            event.getScreenY() - 10);
                    } else {
                        tooltip.setX(event.getScreenX() + 10);
                        tooltip.setY(event.getScreenY() - 10);
                    }
                } else {
                    tooltip.hide();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        candlestickChart.setOnMouseExited(event -> {
            tooltip.hide();
        });
    }
    
    // 显示K线图
    private void showKLineView() {
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
    
    // 获取最后一次游戏的sessionId
    private Long getLastSessionId() {
        try {
            GameSession session = tradingService.getLastStockGameSession(stock.getId());
            if (session != null && !session.getIsActive()) {
                return session.getId();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public VBox getView() {
        return view;
    }
}
