package com.stockgame.model;

import java.math.BigDecimal;

public class IntradayKLineAdapter implements KLine<IntradayKLineAdapter> {
    private final IntradayKLine kline;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final boolean hasOHLC;
    
    public IntradayKLineAdapter(IntradayKLine kline) {
        this.kline = kline;
        this.open = kline.getPrice();
        this.high = kline.getPrice();
        this.low = kline.getPrice();
        this.hasOHLC = false;
    }
    
    public IntradayKLineAdapter(IntradayKLine kline, BigDecimal open, BigDecimal high, BigDecimal low) {
        this.kline = kline;
        this.open = open;
        this.high = high;
        this.low = low;
        this.hasOHLC = true;
    }
    
    public IntradayKLine getKline() {
        return kline;
    }
    
    @Override
    public Long getId() {
        return kline.getId();
    }
    
    @Override
    public BigDecimal getOpen() {
        return open;
    }

    @Override
    public BigDecimal getHigh() {
        return high;
    }

    @Override
    public BigDecimal getLow() {
        return low;
    }

    @Override
    public BigDecimal getClose() {
        return kline.getPrice();
    }

    @Override
    public String getLabel() {
        return kline.getTime() != null ? kline.getTime().toString().substring(11, 16) : "";
    }
    
    @Override
    public boolean hasOHLC() {
        return hasOHLC;
    }
    
    @Override
    public int compareTo(IntradayKLineAdapter other) {
        if (kline.getTime() != null && other.kline.getTime() != null) {
            int cmp = kline.getTime().compareTo(other.kline.getTime());
            if (cmp != 0) return cmp;
        }
        if (kline.getId() != null && other.kline.getId() != null) {
            return kline.getId().compareTo(other.kline.getId());
        }
        return 0;
    }
}
