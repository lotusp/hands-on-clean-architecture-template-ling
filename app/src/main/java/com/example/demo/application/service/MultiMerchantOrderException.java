package com.example.demo.application.service;

/**
 * Exception thrown when an order contains items from multiple merchants.
 * Requirement 1.1.5: Orders can only contain items from a single merchant.
 */
public class MultiMerchantOrderException extends RuntimeException {
    public MultiMerchantOrderException() {
        super("订单只能包含同一商家的餐品");
    }
}
