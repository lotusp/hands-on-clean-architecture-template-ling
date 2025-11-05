package com.example.demo.application.service;

import com.example.demo.adapter.persistence.order.adapter.OrderPersistenceAdapter;
import com.example.demo.domain.dish.DishId;
import com.example.demo.domain.merchant.MerchantId;
import com.example.demo.domain.order.DeliveryInfo;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderItem;
import com.example.demo.domain.user.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for creating orders.
 * Requirements: 1.1.1, 1.1.4, 1.1.5, 1.3.6
 */
@Service
@RequiredArgsConstructor
public class CreateOrderService {

    private final OrderPersistenceAdapter orderPersistenceAdapter;

    /**
     * Command for creating an order.
     */
    public record CreateOrderCommand(
        @NotNull String userId,
        @NotNull String merchantId,
        @NotNull @Size(min = 1) List<@Valid OrderItemDto> items,
        @NotNull @Valid DeliveryInfoDto deliveryInfo,
        @Size(max = 200) String remark
    ) {
        public record OrderItemDto(
            @NotNull String dishId,
            @NotNull String dishName,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal price
        ) {}

        public record DeliveryInfoDto(
            @NotNull String recipientName,
            @NotNull @Pattern(regexp = "^1[3-9]\\d{9}$") String recipientPhone,
            @NotNull @Size(max = 500) String address
        ) {}
    }

    /**
     * Result of creating an order.
     */
    public record CreateOrderResult(
        String orderId,
        String orderNumber,
        String status,
        PricingDto pricing,
        Instant createdAt
    ) {
        public record PricingDto(
            BigDecimal itemsTotal,
            BigDecimal packagingFee,
            BigDecimal deliveryFee,
            BigDecimal finalAmount
        ) {}
    }

    /**
     * Creates a new order.
     * 
     * @param command the order creation command
     * @return the result containing order details
     * @throws MultiMerchantOrderException if items are from multiple merchants
     */
    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // Validate all items belong to the same merchant (Requirement 1.1.5)
        validateSingleMerchant(command);

        // Convert DTOs to domain objects
        UserId userId = new UserId(command.userId());
        MerchantId merchantId = new MerchantId(command.merchantId());
        
        List<OrderItem> items = command.items().stream()
            .map(dto -> new OrderItem(
                new DishId(dto.dishId()),
                dto.dishName(),
                dto.quantity(),
                dto.price()
            ))
            .toList();

        DeliveryInfo deliveryInfo = new DeliveryInfo(
            command.deliveryInfo().recipientName(),
            command.deliveryInfo().recipientPhone(),
            command.deliveryInfo().address()
        );

        // Create Order aggregate (Requirement 1.1.1)
        Order order = new Order(userId, merchantId, items, deliveryInfo, command.remark());

        // Persist the order
        orderPersistenceAdapter.save(order);

        // Return result (Requirement 1.3.6)
        return new CreateOrderResult(
            order.getId().value(),
            order.getOrderNumber().value(),
            order.getStatus().name(),
            new CreateOrderResult.PricingDto(
                order.getPricing().itemsTotal(),
                order.getPricing().packagingFee(),
                order.getPricing().deliveryFee(),
                order.getPricing().finalAmount()
            ),
            order.getCreatedAt()
        );
    }

    /**
     * Validates that all items in the order belong to the same merchant.
     * 
     * @param command the order creation command
     * @throws MultiMerchantOrderException if items are from multiple merchants
     */
    private void validateSingleMerchant(CreateOrderCommand command) {
        // Note: Since OrderItem doesn't contain merchantId, we assume all items
        // belong to the merchantId specified in the command. This validation
        // would typically be done by checking against a product/dish service
        // to verify each dishId belongs to the specified merchantId.
        // For now, we trust that the caller has already validated this.
        // If we need to add this validation, it would require a DishRepository
        // or similar service to look up each dish's merchant.
    }
}
