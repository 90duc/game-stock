package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeRecord {
    private Long id;
    private Long userId;
    private Long stockId;
    private String stockCode;
    private String stockName;
    private Order.OrderType tradeType;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private LocalDateTime tradeTime;
    private Long gameSessionId;
    
    public TradeRecord() {
        this.quantity = 0;
        this.price = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    
    public Order.OrderType getTradeType() { return tradeType; }
    public void setTradeType(Order.OrderType tradeType) { this.tradeType = tradeType; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }
    
    public Long getGameSessionId() { return gameSessionId; }
    public void setGameSessionId(Long gameSessionId) { this.gameSessionId = gameSessionId; }
}
