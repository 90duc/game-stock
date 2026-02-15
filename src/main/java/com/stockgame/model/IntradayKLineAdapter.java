package com.stockgame.model;

import java.math.BigDecimal;

public class IntradayKLineAdapter implements KLine<IntradayKLineAdapter> {
    private final IntradayKLine kline;
    
    public IntradayKLineAdapter(IntradayKLine kline) {
        this.kline = kline;
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
        return kline.getPrice();
    }

    @Override
    public BigDecimal getHigh() {
        return kline.getPrice();
    }

    @Override
    public BigDecimal getLow() {
        return kline.getPrice();
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
        return false;
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
