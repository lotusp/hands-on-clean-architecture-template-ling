package com.example.demo.adapter.web;

import com.example.demo.application.service.CreateOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

public abstract class OrdersBase extends ContractTestBase {

    @BeforeEach
    public void setup() {
        super.setup();

        when(createOrderService.createOrder(any()))
                .thenAnswer(invocation -> {
                    CreateOrderService.CreateOrderCommand command = invocation.getArgument(0);

                    BigDecimal itemsTotal = command.items().stream()
                            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal packagingFee = new BigDecimal("1.00");
                    BigDecimal deliveryFee = new BigDecimal("3.00");
                    BigDecimal finalAmount = itemsTotal.add(packagingFee).add(deliveryFee);

                    return new CreateOrderService.CreateOrderResult(
                            "order-id-1",
                            "20251105102730996280",
                            "PENDING_PAYMENT",
                            new CreateOrderService.CreateOrderResult.PricingDto(
                                    itemsTotal,
                                    packagingFee,
                                    deliveryFee,
                                    finalAmount
                            ),
                            Instant.parse("2025-11-05T02:27:30.745152Z")
                    );
                });
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (testInfo.getTestMethod()
                .filter(method -> method.getName().equals("validate_create_a_new_order"))
                .isPresent()) {
            verify(createOrderService)
                    .createOrder(
                            assertArg(command -> {
                                assertThat(command.userId()).isEqualTo("user-token");
                                assertThat(command.merchantId()).isEqualTo("merchant-001");
                                assertThat(command.items()).hasSize(1);
                                assertThat(command.items().get(0).dishId()).isEqualTo("dish-001");
                                assertThat(command.items().get(0).dishName()).isEqualTo("宫保鸡丁");
                                assertThat(command.items().get(0).quantity()).isEqualTo(2);
                                assertThat(command.items().get(0).price()).isEqualTo(new BigDecimal("25.00"));
                                assertThat(command.deliveryInfo().recipientName()).isEqualTo("张三");
                                assertThat(command.deliveryInfo().recipientPhone()).isEqualTo("13800138000");
                                assertThat(command.deliveryInfo().address()).isEqualTo("北京市朝阳区xxx街道xxx号");
                            }));
        }
    }
}
