package com.example.demo.adapter.persistence.order.adapter;

import com.example.demo.adapter.persistence.order.OrderEntity;
import com.example.demo.adapter.persistence.order.OrderEntityRepository;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderId;
import com.example.demo.domain.order.OrderNumber;
import com.example.demo.domain.merchant.MerchantId;
import com.example.demo.domain.user.UserId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter {

    private final OrderEntityRepository orderEntityRepository;

    // TODO: This is a temporary implementation. Will be properly implemented in task 5.6
    // Currently the OrderEntity structure doesn't match the new Order domain model
    public void save(Order order) {
        // Temporary stub - will be implemented in task 5
        throw new UnsupportedOperationException(
            "OrderPersistenceAdapter.save() needs to be implemented in task 5.6 after OrderEntity is updated"
        );
    }

    // TODO: This is a temporary implementation. Will be properly implemented in task 5.6
    public Optional<Order> findById(OrderId orderId) {
        // Temporary stub - will be implemented in task 5
        throw new UnsupportedOperationException(
            "OrderPersistenceAdapter.findById() needs to be implemented in task 5.6 after OrderEntity is updated"
        );
    }
}
