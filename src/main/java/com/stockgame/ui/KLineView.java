package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;

public class KLineView {
    private final StockTradingService tradingService;
    private final Stock stock;
    private VBox view;
    private Canvas canvas;
    private ToggleGroup periodGroup;
    private static final int CANVAS_WIDTH = 850;
    private static final int CANVAS_HEIGHT = 450;
    
    public KLineView(StockTradingService tradingService, Stock stock) throws SQLException {
        this.tradingService = tradingService;
        this.stock = stock;
        this.view = createView();
        loadData("DAY");
    }
    
    private VBox createView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #1a1a1a;");
        
        // 标题
        Label titleLabel = new Label(stock.getStockName() + " (" + stock.getStockCode() + ") - K线图");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // 周期选择
        HBox periodBox = new HBox(10);
        periodGroup = new ToggleGroup();
        
        ToggleButton dayBtn = createPeriodButton("日K", "DAY");
        ToggleButton weekBtn = createPeriodButton("周K", "WEEK");
        ToggleButton monthBtn = createPeriodButton("月K", "MONTH");
        
        dayBtn.setSelected(true);
        periodBox.getChildren().addAll(dayBtn, weekBtn, monthBtn);
        
        // 蜡烛图画布
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // 图例
        HBox legendBox = new HBox(20);
        legendBox.setPadding(new Insets(5));
        
        Label redLabel = new Label("■ 涨");
        redLabel.setStyle("-fx-text-fill: #ff3333;");
        
        Label greenLabel = new Label("■ 跌");
        greenLabel.setStyle("-fx-text-fill: #33ff33;");
        
        legendBox.getChildren().addAll(redLabel, greenLabel);
        
        root.getChildren().addAll(titleLabel, periodBox, canvas, legendBox);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        
        return root;
    }
    
    private ToggleButton createPeriodButton(String text, String period) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(periodGroup);
        btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-min-width: 60;");
        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                loadData(period);
            }
        });
        return btn;
    }
    
    private void loadData(String period) {
        try {
            List<DayKLine> kLines = null;
            
            if ("DAY".equals(period)) {
                kLines = tradingService.getDayKLines(stock.getId(), 60);
            } else if ("WEEK".equals(period)) {
                kLines = tradingService.getDayKLines(stock.getId(), 120);
            } else if ("MONTH".equals(period)) {
                kLines = tradingService.getDayKLines(stock.getId(), 365);
            }
            
            if (kLines != null && !kLines.isEmpty()) {
                drawCandlestickChart(kLines);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void drawCandlestickChart(List<DayKLine> kLines) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // 清空画布
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // 计算价格范围
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (DayKLine line : kLines) {
            minPrice = Math.min(minPrice, line.getLow().doubleValue());
            maxPrice = Math.max(maxPrice, line.getHigh().doubleValue());
        }
        
        // 添加一些边距
        double priceRange = maxPrice - minPrice;
        minPrice -= priceRange * 0.05;
        maxPrice += priceRange * 0.05;
        priceRange = maxPrice - minPrice;
        
        // 绘制网格
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(0.5);
        
        // 水平网格线
        for (int i = 0; i <= 5; i++) {
            double y = 50 + i * (CANVAS_HEIGHT - 100) / 5.0;
            gc.strokeLine(60, y, CANVAS_WIDTH - 20, y);
            
            // 价格标签
            double price = maxPrice - i * priceRange / 5.0;
            gc.setFill(Color.web("#888"));
            gc.fillText(String.format("%.2f", price), 5, (int)y + 4);
        }
        
        // 垂直网格线
        int visibleCount = Math.min(kLines.size(), 50);
        double candleWidth = (CANVAS_WIDTH - 80.0) / visibleCount;
        
        for (int i = 0; i <= visibleCount; i += 10) {
            double x = 60 + i * candleWidth;
            gc.strokeLine(x, 50, x, CANVAS_HEIGHT - 50);
        }
        
        // 绘制K线
        int startIndex = Math.max(0, kLines.size() - visibleCount);
        
        for (int i = startIndex; i < kLines.size(); i++) {
            DayKLine line = kLines.get(i);
            int idx = i - startIndex;
            
            double x = 60 + idx * candleWidth + candleWidth / 2;
            
            double open = line.getOpen().doubleValue();
            double close = line.getClose().doubleValue();
            double high = line.getHigh().doubleValue();
            double low = line.getLow().doubleValue();
            
            // 转换为Y坐标
            double yOpen = 50 + (maxPrice - open) / priceRange * (CANVAS_HEIGHT - 100);
            double yClose = 50 + (maxPrice - close) / priceRange * (CANVAS_HEIGHT - 100);
            double yHigh = 50 + (maxPrice - high) / priceRange * (CANVAS_HEIGHT - 100);
            double yLow = 50 + (maxPrice - low) / priceRange * (CANVAS_HEIGHT - 100);
            
            // 确定涨跌颜色
            boolean isRising = close >= open;
            Color color = isRising ? Color.web("#ff3333") : Color.web("#33ff33");
            
            gc.setStroke(color);
            gc.setLineWidth(1);
            
            // 绘制影线
            gc.strokeLine(x, yHigh, x, yLow);
            
            // 绘制实体
            double bodyTop = Math.min(yOpen, yClose);
            double bodyBottom = Math.max(yOpen, yClose);
            double bodyHeight = Math.max(bodyBottom - bodyTop, 1);
            
            gc.setFill(color);
            gc.fillRect(x - candleWidth * 0.35, bodyTop, candleWidth * 0.7, bodyHeight);
            
            // 绘制日期（每隔一定间隔）
            if (idx % 10 == 0) {
                gc.setFill(Color.web("#888"));
                gc.fillText(line.getTradeDate().toString().substring(5), (int)x - 15, CANVAS_HEIGHT - 30);
            }
        }
        
        // 绘制边框
        gc.setStroke(Color.web("#555"));
        gc.setLineWidth(1);
        gc.strokeRect(60, 50, CANVAS_WIDTH - 80, CANVAS_HEIGHT - 100);
    }
    
    public VBox getView() {
        return view;
    }
}
