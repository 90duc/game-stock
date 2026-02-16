package com.stockgame.ui;

import com.stockgame.model.*;
import com.stockgame.service.StockTradingService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KLineView {
    private final StockTradingService tradingService;
    private final Stock stock;
    private final VBox view;
    private final KLineChartPane chartPane;
    private ToggleGroup periodGroup;
    private Slider scrollSlider;
    
    public KLineView(StockTradingService tradingService, Stock stock) throws SQLException {
        this.tradingService = tradingService;
        this.stock = stock;
        this.chartPane = new KLineChartPane();
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
        
        VBox chartView = chartPane.getView();
        
        HBox legendBox = new HBox(20);
        legendBox.setPadding(new Insets(5));
        
        Label redLabel = new Label("■ 涨");
        redLabel.setStyle("-fx-text-fill: #ff3333;");
        
        Label greenLabel = new Label("■ 跌");
        greenLabel.setStyle("-fx-text-fill: #00A000;");
        
        legendBox.getChildren().addAll(redLabel, greenLabel);
        
        root.getChildren().addAll(titleLabel, periodBox, chartView, legendBox);
        
        return root;
    }
    
    private ToggleButton createPeriodButton(String text, String period) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(periodGroup);
        btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-min-width: 60;");
        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                btn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 60;");
            } else {
                btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-min-width: 60;");
            }
        });
        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                loadData(period);
            }
        });
        return btn;
    }
    
    private void loadData(String period) {
        try {
            List<? extends KLine> kLines = null;
            
            if ("DAY".equals(period)) {
                kLines = tradingService.getDayKLines(stock.getId(), Integer.MAX_VALUE);
            } else if ("WEEK".equals(period)) {
                kLines = tradingService.getWeekKLines(stock.getId(),  Integer.MAX_VALUE);
            } else if ("MONTH".equals(period)) {
                kLines = tradingService.getMonthKLines(stock.getId(),  Integer.MAX_VALUE);
            }

            if(kLines == null){
                kLines = new ArrayList<>();
            }

            kLines.sort(Comparator.naturalOrder());
            chartPane.loadData(kLines);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public VBox getView() {
        return view;
    }
}
