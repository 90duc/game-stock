package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    public enum OrderType {
        BUY("买入"), SELL("卖出");
        
        private final String text;
        OrderType(String text) { this.text = text; }
        public String getText() { return text; }
    }
    
    public enum OrderPriceType {
        LIMIT("限价"), MARKET("市价");
        
        private final String text;
        OrderPriceType(String text) { this.text = text; }
        public String getText() { return text; }
    }
    
    public enum OrderStatus {
        PENDING("挂单中"), FILLED("已成交"), CANCELLED("已撤销"), EXPIRED("已过期");
        
        private final String text;
        OrderStatus(String text) { this.text = text; }
        public String getText() { return text; }
    }
    
    private Long id;
    private Long userId;
    private Long stockId;
    private OrderType orderType;
    private OrderPriceType priceType;
    private Integer quantity;
    private BigDecimal price;
    private OrderStatus status;
    private Integer filledQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long gameSessionId;
    
    public Order() {
        this.quantity = 0;
        this.price = BigDecimal.ZERO;
        this.filledQuantity = 0;
        this.status = OrderStatus.PENDING;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    
    public OrderPriceType getPriceType() { return priceType; }
    public void setPriceType(OrderPriceType priceType) { this.priceType = priceType; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public Integer getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(Integer filledQuantity) { this.filledQuantity = filledQuantity; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getGameSessionId() { return gameSessionId; }
    public void setGameSessionId(Long gameSessionId) { this.gameSessionId = gameSessionId; }
    
    public Integer getRemainingQuantity() {
        return quantity - filledQuantity;
    }
    
    public BigDecimal getFrozenAmount() {
        if (orderType == OrderType.BUY) {
            BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(getRemainingQuantity()));
            BigDecimal commissionRate = new BigDecimal("0.00025");
            BigDecimal commission = totalAmount.multiply(commissionRate);
            commission = commission.max(new BigDecimal("5"));
            return totalAmount.add(commission);
        }
        return BigDecimal.ZERO;
    }
}
