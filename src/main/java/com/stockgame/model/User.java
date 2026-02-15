package com.stockgame.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private LocalDateTime createdAt;
    
    public User() {
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public BigDecimal getFrozenBalance() { return frozenBalance; }
    public void setFrozenBalance(BigDecimal frozenBalance) { this.frozenBalance = frozenBalance; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public BigDecimal getAvailableBalance() {
        return balance.subtract(frozenBalance);
    }
}
