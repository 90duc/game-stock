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

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public class KLineChartPane {
    private VBox view;
    private Canvas canvas;
    private Slider scrollSlider;
    private Label hoverLabel;
    private List<KLine> currentKLines;
    private static final int CANVAS_HEIGHT = 350;
    private static final int CHART_MARGIN_TOP = 15;
    private static final int CHART_MARGIN_BOTTOM = 25;
    private static final int CHART_MARGIN_LEFT = 40;
    private static final int CHART_MARGIN_RIGHT = 0;

    private static final int CANDLE_MAX_SIZE = 100;
    private double maxPriceValue;
    private double minPriceValue;
    private BigDecimal openPrice;
    private BigDecimal currentPrice;
    private Consumer<String> periodChangeListener;
    
    public KLineChartPane() {
        this.view = createView();
    }

    public VBox getView() {
        return view;
    }
    
    public void loadData(List<? extends KLine> kLines) {
        loadData(kLines, null, null);
    }
    
    public void loadData(List<? extends KLine> kLines, BigDecimal openPrice, BigDecimal currentPrice) {
        this.openPrice = openPrice;
        this.currentPrice = currentPrice;
        currentKLines = (List<KLine>) kLines;
        scrollSlider.setValue(100);
        drawCandlestickChart();
    }
    
    private VBox createView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setMinWidth(850);
        root.setMinHeight(CANVAS_HEIGHT + 50);
        
        canvas = new Canvas(850, CANVAS_HEIGHT);
        
        hoverLabel = new Label();
        hoverLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: #444; -fx-padding: 5 8 5 8; -fx-background-radius: 3; -fx-alignment: center;");
        hoverLabel.setVisible(false);
        hoverLabel.setMouseTransparent(true);
        hoverLabel.setWrapText(true);
        
        AnchorPane chartPane = new AnchorPane();
        chartPane.setMinSize(400, 200);
        chartPane.getChildren().add(canvas);
        chartPane.getChildren().add(hoverLabel);
        
        chartPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.doubleValue() > 0) {
                canvas.setWidth(newVal.doubleValue());
                if (currentKLines != null) {
                    drawCandlestickChart();
                }
            }
        });
        
        scrollSlider = new Slider();
        scrollSlider.setMin(0);
        scrollSlider.setMax(100);
        scrollSlider.setValue(100);
        scrollSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentKLines != null) {
                drawCandlestickChart();
            }
        });

        Label label = new Label("范围:");
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-alignment: center;");
        HBox sliderBox = new HBox(label, scrollSlider);

        sliderBox.setAlignment(Pos.CENTER_LEFT);
        
        canvas.setOnMouseMoved(e -> {
            double x = e.getX();
            double y = e.getY();
            double canvasWidth = canvas.getWidth();
            
            if (currentKLines != null && y >= CHART_MARGIN_TOP && y <= CANVAS_HEIGHT - CHART_MARGIN_BOTTOM) {
                double sliderValue = scrollSlider.getValue();
                int maxDisplay = Math.min(CANDLE_MAX_SIZE, currentKLines.size());
                int startIndex = (int) ((currentKLines.size() - maxDisplay) * (sliderValue / 100));
                startIndex = Math.max(0, Math.min(startIndex, currentKLines.size() - maxDisplay));
                
                double candleWidth = (canvasWidth - CHART_MARGIN_LEFT - CHART_MARGIN_RIGHT) / CANDLE_MAX_SIZE;
                int idx = (int) ((x - CHART_MARGIN_LEFT) / candleWidth);
                
                if (idx >= 0 && idx < maxDisplay && startIndex + idx >= 0 && startIndex + idx < currentKLines.size()) {
                    KLine line = currentKLines.get(startIndex + idx);
                    
                    String text = buildTooltipText(line);
                    
                    double candleX = CHART_MARGIN_LEFT + idx * candleWidth;
                    
                    double priceRange = maxPriceValue - minPriceValue;
                    double highY = CHART_MARGIN_TOP + (maxPriceValue - line.getHigh().doubleValue()) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
                    
                    hoverLabel.setText(text);
                    double labelWidth = hoverLabel.getWidth() > 0 ? hoverLabel.getWidth() : 80;
                    double candleRight = candleX + candleWidth;
                    if (candleRight + 20 + labelWidth > canvasWidth - CHART_MARGIN_RIGHT) {
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
    
    private void drawCandlestickChart() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double canvasWidth = canvas.getWidth();
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, canvasWidth, CANVAS_HEIGHT);
        
        if (currentKLines == null || currentKLines.isEmpty()) {
            gc.setFill(Color.web("#666"));
            gc.fillText("暂无数据", canvasWidth / 2 - 30, CANVAS_HEIGHT / 2);
            return;
        }
        
        double sliderValue = scrollSlider.getValue();
        int maxDisplay = Math.min(CANDLE_MAX_SIZE, currentKLines.size());
        int startIndex = (int) ((currentKLines.size() - maxDisplay) * (sliderValue / 100));
        startIndex = Math.max(0, Math.min(startIndex, currentKLines.size() - maxDisplay));
        
        int displayCount = maxDisplay;
        
        if (displayCount <= 0) return;
        
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (int i = 0; i < displayCount; i++) {
            KLine line = currentKLines.get(startIndex +  i);
            minPrice = Math.min(minPrice, line.getLow().doubleValue());
            maxPrice = Math.max(maxPrice, line.getHigh().doubleValue());
        }
        
        double priceRange = maxPrice - minPrice;
        minPrice -= priceRange * 0.05;
        maxPrice += priceRange * 0.05;
        priceRange = maxPrice - minPrice;
        
        minPriceValue = minPrice;
        maxPriceValue = maxPrice;
        
        final double candleWidth = (canvasWidth - CHART_MARGIN_LEFT - CHART_MARGIN_RIGHT) / CANDLE_MAX_SIZE;
        
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(0.5);
        
        for (int i = 0; i <= 5; i++) {
            double y = CHART_MARGIN_TOP + i * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM) / 5.0;
            gc.strokeLine(CHART_MARGIN_LEFT, y, canvasWidth - CHART_MARGIN_RIGHT, y);
            
            double price = maxPrice - i * priceRange / 5.0;
            gc.setFill(Color.web("#888"));
            gc.fillText(String.format("%.2f", price), 0, (int)y + 4);
        }
        
        for (int i = 0; i <= displayCount; i += 10) {
            double x = CHART_MARGIN_LEFT + i * candleWidth;
            gc.strokeLine(x, CHART_MARGIN_TOP, x, CANVAS_HEIGHT - CHART_MARGIN_BOTTOM);
        }

        
        for (int i = 0; i < displayCount; i++) {
            KLine line = currentKLines.get(startIndex + i);
            
            double x = CHART_MARGIN_LEFT + i * candleWidth + candleWidth / 2;
            
            double open, close, high, low;
            open = line.getOpen().doubleValue();
            close = line.getClose().doubleValue();
            high = line.getHigh().doubleValue();
            low = line.getLow().doubleValue();
            
            double yOpen = CHART_MARGIN_TOP + (maxPrice - open) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            double yClose = CHART_MARGIN_TOP + (maxPrice - close) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            double yHigh = CHART_MARGIN_TOP + (maxPrice - high) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            double yLow = CHART_MARGIN_TOP + (maxPrice - low) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            
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
                double len = line.getLabel().length()/2.0 * gc.getFont().getSize();
                gc.fillText(line.getLabel(), (int)(x - len) + 10, CANVAS_HEIGHT - CHART_MARGIN_BOTTOM + 20);
            }
        }
        
        // 画开盘价线和当前价线
        if (openPrice != null && priceRange > 0) {
            double openY = CHART_MARGIN_TOP + (maxPrice - openPrice.doubleValue()) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.setLineDashes(5, 5);
            gc.strokeLine(CHART_MARGIN_LEFT, openY, canvasWidth - CHART_MARGIN_RIGHT, openY);
            gc.setLineDashes(null);
            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%.2f", openPrice), 0, (int)openY + 4);
        }
        
        if (currentPrice != null && priceRange > 0) {
            double currentY = CHART_MARGIN_TOP + (maxPrice - currentPrice.doubleValue()) / priceRange * (CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1);
            gc.setLineDashes(5, 5);
            gc.strokeLine(CHART_MARGIN_LEFT, currentY, canvasWidth - CHART_MARGIN_RIGHT, currentY);
            gc.setLineDashes(null);
            gc.setFill(Color.web("#FFD700"));
            gc.fillText(String.format("%.2f", currentPrice), 0, (int)currentY - 5);
        }
        
        gc.setStroke(Color.web("#555"));
        gc.setLineWidth(1);
        gc.strokeRect(CHART_MARGIN_LEFT, CHART_MARGIN_TOP, canvasWidth - CHART_MARGIN_LEFT - CHART_MARGIN_RIGHT, CANVAS_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM);
    }
}
