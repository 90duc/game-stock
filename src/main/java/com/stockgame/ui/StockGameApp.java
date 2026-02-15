package com.stockgame.ui;

import com.stockgame.init.DataInitializer;
import com.stockgame.model.User;
import com.stockgame.service.StockTradingService;
import com.stockgame.util.DatabaseUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.sql.SQLException;

public class StockGameApp extends Application {
    private StockTradingService tradingService;
    private User currentUser;
    private javafx.animation.Timeline refreshTimeline;
    
    @Override
    public void init() throws Exception {
        super.init();
        // 初始化数据
        DataInitializer initializer = new DataInitializer();
        initializer.initialize();
        
        tradingService = new StockTradingService();
        currentUser = tradingService.getUser(1L); // 获取默认用户
        
        // 程序启动时结束所有进行中的游戏（处理上次崩溃遗留的游戏）
        cleanupAllActiveGames();
    }
    
    // 结束所有进行中的游戏
    private void cleanupAllActiveGames() {
        try {
            System.out.println("检查并结束所有进行中的游戏...");
            tradingService.endAllActiveGames();
            System.out.println("清理完成");
        } catch (SQLException e) {
            System.err.println("清理进行中的游戏失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("股票模拟交易系统");
        
        TabPane tabPane = new TabPane();
        
        // 股票大厅
        StockHallView stockHallView = new StockHallView(tradingService, currentUser);
        Tab hallTab = new Tab("股票大厅", stockHallView.getView());
        hallTab.setClosable(false);
        
        // 个人账号
        AccountView accountView = new AccountView(tradingService, currentUser);
        Tab accountTab = new Tab("个人账号", accountView.getView());
        accountTab.setClosable(false);
        
        tabPane.getTabs().addAll(hallTab, accountTab);
        
        Scene scene = new Scene(tabPane, 1200, 800);
        primaryStage.setScene(scene);
        
        // 启动定时刷新
        startAutoRefresh(stockHallView, accountView);
        
        primaryStage.setOnCloseRequest(event -> {
            stopAutoRefresh();
            // 退出时结束所有进行中的游戏
            try {
                tradingService.endAllActiveGames();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseUtil.closeConnection();
                Platform.exit();
            }
        });
        
        primaryStage.show();
    }
    
    private void startAutoRefresh(StockHallView stockHallView, AccountView accountView) {
        refreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> {
                    Platform.runLater(() -> {
                        try {
                            stockHallView.refresh();
                            accountView.refresh();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            )
        );
        refreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimeline.play();
    }
    
    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
