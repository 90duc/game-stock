package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MonthKLine implements KLine<MonthKLine> {
    private Long id;
    private Long stockId;
    private LocalDate monthStart;
    private LocalDate monthEnd;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private LocalDateTime createdAt;
    
    public MonthKLine() {
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
    
    public LocalDate getMonthStart() { return monthStart; }
    public void setMonthStart(LocalDate monthStart) { this.monthStart = monthStart; }
    
    public LocalDate getMonthEnd() { return monthEnd; }
    public void setMonthEnd(LocalDate monthEnd) { this.monthEnd = monthEnd; }
    
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
        return monthEnd != null ? monthEnd.toString() : "";
    }
    
    @Override
    public boolean hasOHLC() {
        return true;
    }
    
    @Override
    public int compareTo(MonthKLine other) {
        if (monthEnd != null && other.monthEnd != null) {
            int cmp = monthEnd.compareTo(other.monthEnd);
            if (cmp != 0) return cmp;
        }
        if (id != null && other.id != null) {
            return id.compareTo(other.id);
        }
        return 0;
    }
}
