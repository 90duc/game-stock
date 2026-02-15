package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Stock {
    private Long id;
    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private Boolean isTrading;
    private LocalDateTime gameStartTime;
    private LocalDateTime gameEndTime;
    private LocalDate lastKLineDate;
    private LocalDateTime createdAt;
    
    public Stock() {
        this.currentPrice = BigDecimal.ZERO;
        this.previousClose = BigDecimal.ZERO;
        this.isTrading = false;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
    
    public Boolean getIsTrading() { return isTrading; }
    public void setIsTrading(Boolean isTrading) { this.isTrading = isTrading; }
    
    public LocalDateTime getGameStartTime() { return gameStartTime; }
    public void setGameStartTime(LocalDateTime gameStartTime) { this.gameStartTime = gameStartTime; }
    
    public LocalDateTime getGameEndTime() { return gameEndTime; }
    public void setGameEndTime(LocalDateTime gameEndTime) { this.gameEndTime = gameEndTime; }
    
    public LocalDate getLastKLineDate() { return lastKLineDate; }
    public void setLastKLineDate(LocalDate lastKLineDate) { this.lastKLineDate = lastKLineDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
