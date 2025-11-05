package com.example.demo.adapter.persistence.order.adapter;

import com.example.demo.adapter.persistence.order.DeliveryInfoEmbeddable;
import com.example.demo.adapter.persistence.order.OrderEntity;
import com.example.demo.adapter.persistence.order.OrderEntityRepository;
import com.example.demo.adapter.persistence.order.OrderItemEntity;
import com.example.demo.adapter.persistence.order.PricingEmbeddable;
import com.example.demo.domain.dish.DishId;
import com.example.demo.domain.merchant.MerchantId;
import com.example.demo.domain.order.DeliveryInfo;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderId;
import com.example.demo.domain.order.OrderItem;
import com.example.demo.domain.order.OrderNumber;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.order.Pricing;
import com.example.demo.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceAdapterTest {

    @Mock
    private OrderEntityRepository orderEntityRepository;

    @InjectMocks
    private OrderPersistenceAdapter orderPersistenceAdapter;

    @Test
    void save_should_persist_order_with_all_fields() {
        Instant now = Instant.now();

        // Create order items
        OrderItem item1 = new OrderItem(new DishId("dish-001"), "宫保鸡丁", 2, new BigDecimal("25.00"));
        OrderItem item2 = new OrderItem(new DishId("dish-002"), "鱼香肉丝", 1, new BigDecimal("30.00"));

        // Create delivery info
        DeliveryInfo deliveryInfo = new DeliveryInfo("张三", "13800138000", "北京市朝阳区某某街道123号");

        // Create pricing
        Pricing pricing = Pricing.calculate(List.of(item1, item2));

        // Create order using reconstitution constructor
        Order order = new Order(
                new OrderId("order-001"),
                new OrderNumber("20251105102730996280"),
                new UserId("user-001"),
                new MerchantId("merchant-001"),
                List.of(item1, item2),
                deliveryInfo,
                "少辣",
                OrderStatus.PENDING_PAYMENT,
                pricing,
                now,
                now);

        orderPersistenceAdapter.save(order);

        verify(orderEntityRepository).save(assertArg(orderEntity -> {
            assertThat(orderEntity)
                    .returns("order-001", from(OrderEntity::getId))
                    .returns("20251105102730996280", from(OrderEntity::getOrderNumber))
                    .returns("user-001", from(OrderEntity::getUserId))
                    .returns("merchant-001", from(OrderEntity::getMerchantId))
                    .returns("少辣", from(OrderEntity::getRemark))
                    .returns(OrderStatus.PENDING_PAYMENT, from(OrderEntity::getStatus))
                    .returns(now, from(OrderEntity::getCreatedAt))
                    .returns(now, from(OrderEntity::getUpdatedAt));

            // Verify items
            assertThat(orderEntity.getItems()).hasSize(2);
            assertThat(orderEntity.getItems().get(0))
                    .returns("order-001", from(OrderItemEntity::getOrderId))
                    .returns("dish-001", from(OrderItemEntity::getDishId))
                    .returns("宫保鸡丁", from(OrderItemEntity::getDishName))
                    .returns(2, from(OrderItemEntity::getQuantity))
                    .returns(new BigDecimal("25.00"), from(OrderItemEntity::getPrice));
            assertThat(orderEntity.getItems().get(1))
                    .returns("order-001", from(OrderItemEntity::getOrderId))
                    .returns("dish-002", from(OrderItemEntity::getDishId))
                    .returns("鱼香肉丝", from(OrderItemEntity::getDishName))
                    .returns(1, from(OrderItemEntity::getQuantity))
                    .returns(new BigDecimal("30.00"), from(OrderItemEntity::getPrice));

            // Verify delivery info
            assertThat(orderEntity.getDeliveryInfo())
                    .returns("张三", from(DeliveryInfoEmbeddable::getRecipientName))
                    .returns("13800138000", from(DeliveryInfoEmbeddable::getRecipientPhone))
                    .returns("北京市朝阳区某某街道123号", from(DeliveryInfoEmbeddable::getAddress));

            // Verify pricing
            assertThat(orderEntity.getPricing())
                    .returns(new BigDecimal("80.00"), from(PricingEmbeddable::getItemsTotal))
                    .returns(new BigDecimal("1.00"), from(PricingEmbeddable::getPackagingFee))
                    .returns(new BigDecimal("3.00"), from(PricingEmbeddable::getDeliveryFee))
                    .returns(new BigDecimal("84.00"), from(PricingEmbeddable::getFinalAmount));
        }));
    }

    @Test
    void find_by_id_should_return_order_with_all_fields() {
        Instant now = Instant.now();

        // Create order item entities
        OrderItemEntity itemEntity1 = new OrderItemEntity();
        itemEntity1.setOrderId("order-001");
        itemEntity1.setDishId("dish-001");
        itemEntity1.setDishName("宫保鸡丁");
        itemEntity1.setQuantity(2);
        itemEntity1.setPrice(new BigDecimal("25.00"));

        OrderItemEntity itemEntity2 = new OrderItemEntity();
        itemEntity2.setOrderId("order-001");
        itemEntity2.setDishId("dish-002");
        itemEntity2.setDishName("鱼香肉丝");
        itemEntity2.setQuantity(1);
        itemEntity2.setPrice(new BigDecimal("30.00"));

        // Create delivery info embeddable
        DeliveryInfoEmbeddable deliveryInfoEmbeddable =
                new DeliveryInfoEmbeddable("张三", "13800138000", "北京市朝阳区某某街道123号");

        // Create pricing embeddable
        PricingEmbeddable pricingEmbeddable =
                new PricingEmbeddable(
                        new BigDecimal("80.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("84.00"));

        // Create order entity
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId("order-001");
        orderEntity.setOrderNumber("20251105102730996280");
        orderEntity.setUserId("user-001");
        orderEntity.setMerchantId("merchant-001");
        orderEntity.setItems(List.of(itemEntity1, itemEntity2));
        orderEntity.setDeliveryInfo(deliveryInfoEmbeddable);
        orderEntity.setRemark("少辣");
        orderEntity.setStatus(OrderStatus.PENDING_PAYMENT);
        orderEntity.setPricing(pricingEmbeddable);
        orderEntity.setCreatedAt(now);
        orderEntity.setUpdatedAt(now);

        when(orderEntityRepository.findById("order-001")).thenReturn(Optional.of(orderEntity));

        Optional<Order> result = orderPersistenceAdapter.findById(new OrderId("order-001"));

        assertThat(result).isPresent();
        Order order = result.get();

        // Verify basic fields
        assertThat(order.getId().value()).isEqualTo("order-001");
        assertThat(order.getOrderNumber().value()).isEqualTo("20251105102730996280");
        assertThat(order.getUserId().value()).isEqualTo("user-001");
        assertThat(order.getMerchantId().value()).isEqualTo("merchant-001");
        assertThat(order.getRemark()).isEqualTo("少辣");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getCreatedAt()).isEqualTo(now);
        assertThat(order.getUpdatedAt()).isEqualTo(now);

        // Verify items
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0))
                .returns(new DishId("dish-001"), from(OrderItem::dishId))
                .returns("宫保鸡丁", from(OrderItem::dishName))
                .returns(2, from(OrderItem::quantity))
                .returns(new BigDecimal("25.00"), from(OrderItem::price));
        assertThat(order.getItems().get(1))
                .returns(new DishId("dish-002"), from(OrderItem::dishId))
                .returns("鱼香肉丝", from(OrderItem::dishName))
                .returns(1, from(OrderItem::quantity))
                .returns(new BigDecimal("30.00"), from(OrderItem::price));

        // Verify delivery info
        assertThat(order.getDeliveryInfo())
                .returns("张三", from(DeliveryInfo::recipientName))
                .returns("13800138000", from(DeliveryInfo::recipientPhone))
                .returns("北京市朝阳区某某街道123号", from(DeliveryInfo::address));

        // Verify pricing
        assertThat(order.getPricing())
                .returns(new BigDecimal("80.00"), from(Pricing::itemsTotal))
                .returns(new BigDecimal("1.00"), from(Pricing::packagingFee))
                .returns(new BigDecimal("3.00"), from(Pricing::deliveryFee))
                .returns(new BigDecimal("84.00"), from(Pricing::finalAmount));
    }
}
