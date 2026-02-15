package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Position {
    private Long id;
    private Long userId;
    private Long stockId;
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private Integer frozenQuantity;
    private BigDecimal averageCost;
    private BigDecimal currentValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Position() {
        this.quantity = 0;
        this.frozenQuantity = 0;
        this.averageCost = BigDecimal.ZERO;
        this.currentValue = BigDecimal.ZERO;
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
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getFrozenQuantity() { return frozenQuantity; }
    public void setFrozenQuantity(Integer frozenQuantity) { this.frozenQuantity = frozenQuantity; }
    
    public Integer getAvailableQuantity() {
        return quantity - frozenQuantity;
    }
    
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public BigDecimal getProfit() {
        if (currentValue != null && averageCost != null) {
            return currentValue.subtract(averageCost.multiply(BigDecimal.valueOf(quantity)));
        }
        return BigDecimal.ZERO;
    }
}
