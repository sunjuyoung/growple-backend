package com.grow.payment.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Data
public class Product {

    private  Long id;
    private  Long amount;
    private  Integer quantity;
    private  String name;
    private  Long sellerId;

    public Product(Long id, Long amount, Integer quantity, String name, Long sellerId) {
        this.id = id;
        this.amount = amount;
        this.quantity = quantity;
        this.name = name;
        this.sellerId = sellerId;
    }




}
