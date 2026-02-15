package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class IntradayKLine {
    private Long id;
    private Long stockId;
    private LocalDateTime time;
    private BigDecimal price;
    private Long volume;
    private Long gameSessionId;
    private LocalDateTime createdAt;
    
    public IntradayKLine() {
        this.price = BigDecimal.ZERO;
        this.volume = 0L;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    
    public Long getGameSessionId() { return gameSessionId; }
    public void setGameSessionId(Long gameSessionId) { this.gameSessionId = gameSessionId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
