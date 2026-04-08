package com.example.expense.model;

import jakarta.persistence.*;

@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double amount;
    private String category;
    private String type; // INCOME or EXPENSE
    private String description;
    
    private java.time.LocalDateTime dateTime;
    private Boolean isRecurring = false;
    private String billPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public double getAmount(){return amount;}
    public void setAmount(double amount){this.amount=amount;}
    public String getCategory(){return category;}
    public void setCategory(String category){this.category=category;}
    public String getType(){return type;}
    public void setType(String type){this.type=type;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public java.time.LocalDateTime getDateTime(){return dateTime;}
    public void setDateTime(java.time.LocalDateTime dateTime){this.dateTime=dateTime;}
    public Boolean getIsRecurring(){return isRecurring;}
    public void setIsRecurring(Boolean isRecurring){this.isRecurring=isRecurring;}
    public String getBillPath(){return billPath;}
    public void setBillPath(String billPath){this.billPath=billPath;}
    public User getUser(){return user;}
    public void setUser(User user){this.user=user;}
}
