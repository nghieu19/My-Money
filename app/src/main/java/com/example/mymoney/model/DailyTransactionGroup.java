package com.example.mymoney.model;

import com.example.mymoney.database.entity.Transaction;

import java.util.List;

/**
 * Model class to represent a group of transactions for a specific date
 */
public class DailyTransactionGroup {
    private String date;
    private String fullDate;
    private long timestamp;
    private double totalIncome;
    private double totalExpense;
    private List<Transaction> transactions;
    private boolean isExpanded;   // expanded hay k

    public DailyTransactionGroup(String date, String fullDate, long timestamp, List<Transaction> transactions) {
        this.date = date;
        this.fullDate = fullDate;
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.isExpanded = true;  // Default to expanded
        calculateTotals();
    }

    private void calculateTotals() {
        totalIncome = 0;
        totalExpense = 0;
        for (Transaction transaction : transactions) {
            if ("income".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            } else if ("expense".equals(transaction.getType())) {
                totalExpense += transaction.getAmount();
            }
        }
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFullDate() {
        return fullDate;
    }

    public void setFullDate(String fullDate) {
        this.fullDate = fullDate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        calculateTotals();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }
}
