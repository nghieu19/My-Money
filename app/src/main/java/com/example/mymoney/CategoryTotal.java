package com.example.mymoney;

// Class dùng để nhận kết quả từ truy vấn getTopExpenses()
public class CategoryTotal {
    public String category;
    public double total;

    public CategoryTotal(String category, double total) {
        this.category = category;
        this.total = total;
    }

    public CategoryTotal() {}
}
