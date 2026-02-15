package com.stockgame.model;

import java.math.BigDecimal;

public interface KLine<T extends KLine<T>> extends Comparable<T> {
    Long getId();
    BigDecimal getOpen();
    BigDecimal getHigh();
    BigDecimal getLow();
    BigDecimal getClose();
    String getLabel();
    boolean hasOHLC();
}
