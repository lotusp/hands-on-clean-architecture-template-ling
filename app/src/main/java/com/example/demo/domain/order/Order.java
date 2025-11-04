package com.example.demo.domain.order;

import com.example.demo.domain.Identities;
import com.example.demo.domain.merchant.MerchantId;
import com.example.demo.domain.user.UserId;
import java.time.Instant;
import java.util.List;
import lombok.Getter;

@Getter
public class Order {
    private final OrderId id;
    private final OrderNumber orderNumber;
    private final UserId userId;
    private final MerchantId merchantId;
    private final List<OrderItem> items;
    private final DeliveryInfo deliveryInfo;
    private final String remark;
    private OrderStatus status;
    private final Pricing pricing;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating new orders
    public Order(
        UserId userId,
        MerchantId merchantId,
        List<OrderItem> items,
        DeliveryInfo deliveryInfo,
        String remark
    ) {
        // Validate items list is not empty (Requirement 1.1.6)
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("订单必须至少包含一个餐品");
        }

        // Validate remark length if present (Requirement 1.1.9)
        if (remark != null && remark.length() > 200) {
            throw new IllegalArgumentException("备注长度不能超过200字符");
        }

        // Note: Validation that all items belong to the same merchant (Requirement 1.1.5)
        // is handled at the application service layer since OrderItem doesn't contain merchantId

        this.id = new OrderId(Identities.generateId());
        this.orderNumber = new OrderNumber(); // Auto-generate (Requirement 1.1.2)
        this.userId = userId;
        this.merchantId = merchantId;
        this.items = List.copyOf(items); // Immutable copy
        this.deliveryInfo = deliveryInfo;
        this.remark = remark;
        this.status = OrderStatus.PENDING_PAYMENT; // Set initial status (Requirement 1.1.3)
        this.pricing = Pricing.calculate(items); // Auto-calculate pricing (Requirement 1.2.4)
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for reconstituting orders from persistence
    public Order(
        OrderId id,
        OrderNumber orderNumber,
        UserId userId,
        MerchantId merchantId,
        List<OrderItem> items,
        DeliveryInfo deliveryInfo,
        String remark,
        OrderStatus status,
        Pricing pricing,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.merchantId = merchantId;
        this.items = items;
        this.deliveryInfo = deliveryInfo;
        this.remark = remark;
        this.status = status;
        this.pricing = pricing;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("只有待支付状态的订单才能支付");
        }
        this.status = OrderStatus.PAID;
        updatedAt = Instant.now();
    }
}
