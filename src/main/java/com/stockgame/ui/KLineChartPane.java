package com.stockgame.ui;

import com.stockgame.model.KLine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

public class KLineChartPane {
    private VBox view;
    private Canvas canvas;
    private Slider scrollSlider;
    private Label hoverLabel;
    private List<KLine> currentKLines;
    private static final int CANVAS_WIDTH = 850;
    private static final int CANVAS_HEIGHT = 450;
    private double maxPriceValue;
    private double minPriceValue;
    private Consumer<String> periodChangeListener;
    
    public KLineChartPane() {
        this.view = createView();
    }
    
    public void setPeriodChangeListener(Consumer<String> listener) {
        this.periodChangeListener = listener;
    }
    
    public VBox getView() {
        return view;
    }
    
    public void loadData(List<? extends KLine> kLines) {

        currentKLines = (List<KLine>) (List<?>) kLines;
        scrollSlider.setValue(100);
        drawCandlestickChart(currentKLines);

    }
    
    private VBox createView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #1a1a1a;");
        
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
                int maxDisplay = Math.min(50, currentKLines.size());
                int startIndex = (int) ((currentKLines.size() - maxDisplay) * (sliderValue / 100));
                startIndex = Math.max(0, Math.min(startIndex, currentKLines.size() - maxDisplay));
                
                double candleWidth = (CANVAS_WIDTH - 80.0) / 50;
                int idx = (int) ((x - 60) / candleWidth);
                
                if (idx >= 0 && idx < maxDisplay && startIndex + idx >= 0 && startIndex + idx < currentKLines.size()) {
                    KLine line = currentKLines.get(startIndex + idx);
                    
                    String text = buildTooltipText(line);
                    
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
        
        root.getChildren().addAll(chartPane, sliderBox);
        VBox.setVgrow(chartPane, Priority.ALWAYS);
        
        return root;
    }
    
    private String buildTooltipText(KLine line) {
        StringBuilder sb = new StringBuilder();
        sb.append(line.getLabel()).append("\n");
        
        boolean hasOHLC = line.hasOHLC();
        if (hasOHLC) {
            sb.append("开盘: ").append(line.getOpen()).append("\n");
            sb.append("收盘: ").append(line.getClose()).append("\n");
            sb.append("最高: ").append(line.getHigh()).append("\n");
            sb.append("最低: ").append(line.getLow());
        } else {
            sb.append("当前: ").append(line.getClose());
        }
        
        return sb.toString();
    }
    
    private void drawCandlestickChart(List<KLine> kLines) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        if (kLines == null || kLines.isEmpty()) {
            gc.setFill(Color.web("#666"));
            gc.fillText("暂无数据", CANVAS_WIDTH / 2 - 30, CANVAS_HEIGHT / 2);
            return;
        }
        
        double sliderValue = scrollSlider.getValue();
        int maxDisplay = Math.min(50, kLines.size());
        int startIndex = (int) ((kLines.size() - maxDisplay) * (sliderValue / 100));
        startIndex = Math.max(0, Math.min(startIndex, kLines.size() - maxDisplay));
        
        int displayCount = maxDisplay;
        
        if (displayCount <= 0) return;
        
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (int i = 0; i < displayCount; i++) {
            KLine line = kLines.get(startIndex +  i);
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
        
        double candleWidth = (CANVAS_WIDTH - 80.0) / 50;
        
        for (int i = 0; i <= displayCount; i += 10) {
            double x = 60 + i * candleWidth;
            gc.strokeLine(x, 50, x, CANVAS_HEIGHT - 50);
        }
        
        boolean hasOHLC = kLines.get(0).hasOHLC();
        
        for (int i = 0; i < displayCount; i++) {
            KLine line = kLines.get(startIndex + i);
            
            double x = 60 + i * candleWidth + candleWidth / 2;
            
            double open, close, high, low;
            if (hasOHLC) {
                open = line.getOpen().doubleValue();
                close = line.getClose().doubleValue();
            } else {
                open = close = line.getClose().doubleValue();
            }
            high = line.getHigh().doubleValue();
            low = line.getLow().doubleValue();
            
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
                gc.fillText(line.getLabel(), (int)x - 15, CANVAS_HEIGHT - 30);
            }
        }
        
        gc.setStroke(Color.web("#555"));
        gc.setLineWidth(1);
        gc.strokeRect(60, 50, CANVAS_WIDTH - 80, CANVAS_HEIGHT - 100);
    }
}
