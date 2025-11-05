package com.example.demo.adapter.web.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreateOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = createValidOrderRequest();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("订单创建成功"))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.pricing.itemsTotal").value(50.00))
                .andExpect(jsonPath("$.data.pricing.packagingFee").value(1.00))
                .andExpect(jsonPath("$.data.pricing.deliveryFee").value(3.00))
                .andExpect(jsonPath("$.data.pricing.finalAmount").value(54.00))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn();

        // Verify order number format (yyyyMMddHHmmss + 6 digits)
        String responseBody = result.getResponse().getContentAsString();
        String orderNumber = objectMapper
                .readTree(responseBody)
                .path("data")
                .path("orderNumber")
                .asText();
        assertThat(orderNumber).matches("\\d{20}");
    }

    @Test
    void shouldRejectOrderWithMissingMerchantId() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                null, // missing merchantId
                List.of(createValidOrderItem()),
                createValidDeliveryInfo(),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithMissingItems() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                null, // missing items
                createValidDeliveryInfo(),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithEmptyItems() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(), // empty items
                createValidDeliveryInfo(),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithMissingDeliveryInfo() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(createValidOrderItem()),
                null, // missing deliveryInfo
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithInvalidPhoneNumber() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(createValidOrderItem()),
                new CreateOrderController.CreateOrderRequest.DeliveryInfoRequest(
                        "张三",
                        "12345678901", // invalid phone format
                        "北京市朝阳区xxx街道xxx号"),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithTooLongRemark() throws Exception {
        // Given
        String longRemark = "a".repeat(201); // 201 characters
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001", List.of(createValidOrderItem()), createValidDeliveryInfo(), longRemark);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithTooLongAddress() throws Exception {
        // Given
        String longAddress = "a".repeat(501); // 501 characters
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(createValidOrderItem()),
                new CreateOrderController.CreateOrderRequest.DeliveryInfoRequest("张三", "13800138000", longAddress),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(new CreateOrderController.CreateOrderRequest.OrderItemRequest(
                        "dish-001",
                        "宫保鸡丁",
                        0, // invalid quantity
                        new BigDecimal("25.00"))),
                createValidDeliveryInfo(),
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("ValidationError"));
    }

    @Test
    void shouldCalculatePriceCorrectlyWithMultipleItems() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001",
                List.of(
                        new CreateOrderController.CreateOrderRequest.OrderItemRequest(
                                "dish-001", "宫保鸡丁", 2, new BigDecimal("25.00")),
                        new CreateOrderController.CreateOrderRequest.OrderItemRequest(
                                "dish-001", "鱼香肉丝", 1, new BigDecimal("30.00"))),
                createValidDeliveryInfo(),
                null);

        // When & Then
        // itemsTotal = 2 * 25.00 + 1 * 30.00 = 80.00
        // packagingFee = 1.00
        // deliveryFee = 3.00
        // finalAmount = 80.00 + 1.00 + 3.00 = 84.00
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.pricing.itemsTotal").value(80.00))
                .andExpect(jsonPath("$.data.pricing.packagingFee").value(1.00))
                .andExpect(jsonPath("$.data.pricing.deliveryFee").value(3.00))
                .andExpect(jsonPath("$.data.pricing.finalAmount").value(84.00));
    }

    @Test
    void shouldCreateOrderWithRemark() throws Exception {
        // Given
        CreateOrderController.CreateOrderRequest request = new CreateOrderController.CreateOrderRequest(
                "merchant-001", List.of(createValidOrderItem()), createValidDeliveryInfo(), "少辣");

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user-id")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    private CreateOrderController.CreateOrderRequest createValidOrderRequest() {
        return new CreateOrderController.CreateOrderRequest(
                "merchant-001", List.of(createValidOrderItem()), createValidDeliveryInfo(), null);
    }

    private CreateOrderController.CreateOrderRequest.OrderItemRequest createValidOrderItem() {
        return new CreateOrderController.CreateOrderRequest.OrderItemRequest(
                "dish-001", "宫保鸡丁", 2, new BigDecimal("25.00"));
    }

    private CreateOrderController.CreateOrderRequest.DeliveryInfoRequest createValidDeliveryInfo() {
        return new CreateOrderController.CreateOrderRequest.DeliveryInfoRequest("张三", "13800138000", "北京市朝阳区xxx街道xxx号");
    }
}
