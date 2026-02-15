package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DayKLine implements KLine<DayKLine> {
    private Long id;
    private Long stockId;
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private LocalDateTime createdAt;
    
    public DayKLine() {
        this.open = BigDecimal.ZERO;
        this.high = BigDecimal.ZERO;
        this.low = BigDecimal.ZERO;
        this.close = BigDecimal.ZERO;
        this.volume = 0L;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String getLabel() {
        return tradeDate != null ? tradeDate.toString() : "";
    }
    
    @Override
    public boolean hasOHLC() {
        return true;
    }
    
    @Override
    public int compareTo(DayKLine other) {
        if (tradeDate != null && other.tradeDate != null) {
            int cmp = tradeDate.compareTo(other.tradeDate);
            if (cmp != 0) return cmp;
        }
        if (id != null && other.id != null) {
            return id.compareTo(other.id);
        }
        return 0;
    }
}
