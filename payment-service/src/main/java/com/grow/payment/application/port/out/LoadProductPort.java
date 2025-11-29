package com.grow.payment.application.port.out;

import com.grow.payment.domain.Product;

import java.util.List;

public interface LoadProductPort {
    List<Product> getProducts(Long cartId, List<Long> productIds);
}
