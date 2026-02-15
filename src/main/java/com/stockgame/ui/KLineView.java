package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private Slider scrollSlider;
    private Label hoverLabel;
    private List<DayKLine> currentKLines;
    private static final int CANVAS_WIDTH = 850;
    private static final int CANVAS_HEIGHT = 450;
    private double maxPriceValue;
    private double minPriceValue;
    
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
        
        Label titleLabel = new Label(stock.getStockName() + " (" + stock.getStockCode() + ") - K线图");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox periodBox = new HBox(10);
        periodGroup = new ToggleGroup();
        
        ToggleButton dayBtn = createPeriodButton("日K", "DAY");
        ToggleButton weekBtn = createPeriodButton("周K", "WEEK");
        ToggleButton monthBtn = createPeriodButton("月K", "MONTH");
        
        dayBtn.setSelected(true);
        periodBox.getChildren().addAll(dayBtn, weekBtn, monthBtn);
        
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        
        hoverLabel = new Label();
        hoverLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: #444; -fx-padding: 5 8 5 8; -fx-background-radius: 3; -fx-alignment: center;");
        hoverLabel.setVisible(false);
        hoverLabel.setMouseTransparent(true);
        hoverLabel.setWrapText(true);
        
        AnchorPane chartPane = new AnchorPane();
        chartPane.getChildren().add(canvas);
        chartPane.getChildren().add(hoverLabel);
        
        scrollSlider = new Slider();
        scrollSlider.setMin(0);
        scrollSlider.setMax(100);
        scrollSlider.setValue(100);
        scrollSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentKLines != null) {
                drawCandlestickChart(currentKLines);
            }
        });
        
        HBox sliderBox = new HBox(new Label("范围:"), scrollSlider);
        sliderBox.setAlignment(Pos.CENTER_LEFT);
        
        canvas.setOnMouseMoved(e -> {
            double x = e.getX();
            double y = e.getY();
            
            if (currentKLines != null && y >= 50 && y <= CANVAS_HEIGHT - 50) {
                double sliderValue = scrollSlider.getValue();
                int maxDisplay = 50;
                int startIndex = (int) ((currentKLines.size() - maxDisplay) * (1 - sliderValue / 100));
                startIndex = Math.max(0, Math.min(startIndex, currentKLines.size() - maxDisplay));
                
                double candleWidth = (CANVAS_WIDTH - 80.0) / maxDisplay;
                int idx = (int) ((x - 60) / candleWidth);
                
                if (idx >= 0 && idx < maxDisplay && startIndex + maxDisplay - 1 - idx < currentKLines.size()) {
                    DayKLine line = currentKLines.get(startIndex + maxDisplay - 1 - idx);
                    
                    String text = line.getTradeDate() + "\n" +
                            "开盘: " + line.getOpen() + "\n" +
                            "收盘: " + line.getClose() + "\n" +
                            "最高: " + line.getHigh() + "\n" +
                            "最低: " + line.getLow();
                    
                    double candleX = 60 + idx * candleWidth;
                    
                    double priceRange = maxPriceValue - minPriceValue;
                    double highY = 50 + (maxPriceValue - line.getHigh().doubleValue()) / priceRange * (CANVAS_HEIGHT - 100);
                    
                    hoverLabel.setText(text);
                    double labelWidth = hoverLabel.getWidth() > 0 ? hoverLabel.getWidth() : 80;
                    double candleRight = candleX + candleWidth;
                    if (candleRight + 20 + labelWidth > CANVAS_WIDTH - 20) {
                        AnchorPane.setLeftAnchor(hoverLabel, candleX - labelWidth - 5);
                    } else {
                        AnchorPane.setLeftAnchor(hoverLabel, candleX + 20);
                    }
                    AnchorPane.setTopAnchor(hoverLabel, highY - 60);
                    hoverLabel.setVisible(true);
                } else {
                    hoverLabel.setVisible(false);
                }
            } else {
                hoverLabel.setVisible(false);
            }
        });
        
        canvas.setOnMouseExited(e -> hoverLabel.setVisible(false));
        
        HBox legendBox = new HBox(20);
        legendBox.setPadding(new Insets(5));
        
        Label redLabel = new Label("■ 涨");
        redLabel.setStyle("-fx-text-fill: #ff3333;");
        
        Label greenLabel = new Label("■ 跌");
        greenLabel.setStyle("-fx-text-fill: #00A000;");
        
        legendBox.getChildren().addAll(redLabel, greenLabel);
        
        root.getChildren().addAll(titleLabel, periodBox, chartPane, sliderBox, legendBox);
        VBox.setVgrow(chartPane, Priority.ALWAYS);
        
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
                currentKLines = kLines;
                scrollSlider.setValue(100);
                drawCandlestickChart(kLines);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void drawCandlestickChart(List<DayKLine> kLines) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        double sliderValue = scrollSlider.getValue();
        int maxDisplay = 50;
        int startIndex = (int) ((kLines.size() - maxDisplay) * (1 - sliderValue / 100));
        startIndex = Math.max(0, Math.min(startIndex, kLines.size() - maxDisplay));
        
        int displayCount = Math.min(maxDisplay, kLines.size() - startIndex);
        
        if (displayCount <= 0) return;
        
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (int i = startIndex; i < kLines.size(); i++) {
            DayKLine line = kLines.get(i);
            minPrice = Math.min(minPrice, line.getLow().doubleValue());
            maxPrice = Math.max(maxPrice, line.getHigh().doubleValue());
        }
        
        double priceRange = maxPrice - minPrice;
        minPrice -= priceRange * 0.05;
        maxPrice += priceRange * 0.05;
        priceRange = maxPrice - minPrice;
        
        minPriceValue = minPrice;
        maxPriceValue = maxPrice;
        
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(0.5);
        
        for (int i = 0; i <= 5; i++) {
            double y = 50 + i * (CANVAS_HEIGHT - 100) / 5.0;
            gc.strokeLine(60, y, CANVAS_WIDTH - 20, y);
            
            double price = maxPrice - i * priceRange / 5.0;
            gc.setFill(Color.web("#888"));
            gc.fillText(String.format("%.2f", price), 5, (int)y + 4);
        }
        
        double candleWidth = (CANVAS_WIDTH - 80.0) / Math.min(displayCount, 50);
        
        for (int i = 0; i <= Math.min(displayCount, 50); i += 10) {
            double x = 60 + i * candleWidth;
            gc.strokeLine(x, 50, x, CANVAS_HEIGHT - 50);
        }
        
        for (int i = 0; i < displayCount; i++) {
            DayKLine line = kLines.get(startIndex + displayCount - 1 - i);
            
            double x = 60 + i * candleWidth + candleWidth / 2;
            
            double open = line.getOpen().doubleValue();
            double close = line.getClose().doubleValue();
            double high = line.getHigh().doubleValue();
            double low = line.getLow().doubleValue();
            
            double yOpen = 50 + (maxPrice - open) / priceRange * (CANVAS_HEIGHT - 100);
            double yClose = 50 + (maxPrice - close) / priceRange * (CANVAS_HEIGHT - 100);
            double yHigh = 50 + (maxPrice - high) / priceRange * (CANVAS_HEIGHT - 100);
            double yLow = 50 + (maxPrice - low) / priceRange * (CANVAS_HEIGHT - 100);
            
            boolean isRising = close >= open;
            Color color = isRising ? Color.web("#ff3333") : Color.web("#00A000");
            
            gc.setStroke(color);
            gc.setLineWidth(1);
            
            gc.strokeLine(x, yHigh, x, yLow);
            
            double bodyTop = Math.min(yOpen, yClose);
            double bodyBottom = Math.max(yOpen, yClose);
            double bodyHeight = Math.max(bodyBottom - bodyTop, 1);
            
            gc.setFill(color);
            gc.fillRect(x - candleWidth * 0.35, bodyTop, candleWidth * 0.7, bodyHeight);
            
            if (i % 10 == 0) {
                gc.setFill(Color.web("#888"));
                gc.fillText(line.getTradeDate().toString().substring(5), (int)x - 15, CANVAS_HEIGHT - 30);
            }
        }
        
        gc.setStroke(Color.web("#555"));
        gc.setLineWidth(1);
        gc.strokeRect(60, 50, CANVAS_WIDTH - 80, CANVAS_HEIGHT - 100);
    }
    
    public VBox getView() {
        return view;
    }
}
