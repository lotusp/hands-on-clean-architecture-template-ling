# Design Document

## Overview

本设计文档描述"要吃饱"订餐系统 Story 1 的技术实现方案。基于现有的 HoCATLing 整洁架构框架，我们将实现完整的订单创建功能，包括多餐品订单、配送信息、价格计算和数据持久化。

设计遵循现有架构模式：
- Domain 层：纯业务逻辑，不依赖框架
- Application 层：用例编排
- Adapter 层：外部接口适配（Web、Persistence）
- Configuration 层：依赖注入配置

## Architecture

### 层次依赖关系

```
Configuration → Adapter → Application → Domain
```

### 核心组件交互流程

```
HTTP Request 
  → CreateOrderController (Web Adapter)
  → CreateOrderAdapter (Web Adapter)
  → CreateOrderService (Application)
  → Order (Domain)
  → OrderPersistenceAdapter (Persistence Adapter)
  → Database
```

## Components and Interfaces

### 1. Domain Layer (领域层)

#### 1.1 Order (订单聚合根)

**职责**: 封装订单核心业务逻辑和不变量

**新增字段**:
- `OrderNumber orderNumber` - 订单号值对象
- `MerchantId merchantId` - 商家ID值对象
- `List<OrderItem> items` - 订单项列表
- `DeliveryInfo deliveryInfo` - 配送信息值对象
- `String remark` - 备注（可选）
- `Pricing pricing` - 价格信息值对象

**移除字段**:
- `ProductId productId` - 替换为 items 列表
- `int quantity` - 移到 OrderItem 中
- `BigDecimal price` - 替换为 pricing 对象

**构造方法**:
```java
public Order(
    UserId userId,
    MerchantId merchantId,
    List<OrderItem> items,
    DeliveryInfo deliveryInfo,
    String remark
)
```

**业务规则**:
- 订单必须至少包含一个 OrderItem
- 所有 OrderItem 必须属于同一个商家
- 订单创建时自动生成 OrderNumber
- 订单创建时状态为 PENDING_PAYMENT
- 订单创建时自动计算 Pricing

#### 1.2 OrderNumber (订单号值对象)

**职责**: 生成和封装订单号

**格式**: yyyyMMddHHmmss + 6位随机数

**实现**:
```java
public record OrderNumber(String value) {
    public OrderNumber() {
        this(generateOrderNumber());
    }
    
    private static String generateOrderNumber() {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", 
            ThreadLocalRandom.current().nextInt(1000000));
        return timestamp + random;
    }
}
```

#### 1.3 OrderItem (订单项值对象)

**职责**: 封装单个餐品的订单信息

**字段**:
- `DishId dishId` - 餐品ID
- `String dishName` - 餐品名称
- `int quantity` - 数量
- `BigDecimal price` - 单价

**业务规则**:
- quantity 必须大于 0
- price 必须大于等于 0
- dishName 不能为空

**方法**:
```java
public BigDecimal subtotal() {
    return price.multiply(BigDecimal.valueOf(quantity));
}
```

#### 1.4 DeliveryInfo (配送信息值对象)

**职责**: 封装配送相关信息

**字段**:
- `String recipientName` - 收货人姓名
- `String recipientPhone` - 收货人手机号
- `String address` - 收货地址

**验证规则**:
- recipientName 不能为空
- recipientPhone 必须符合格式 ^1[3-9]\\d{9}$
- address 不能为空且长度不超过 500

#### 1.5 Pricing (价格信息值对象)

**职责**: 封装订单价格计算逻辑

**字段**:
- `BigDecimal itemsTotal` - 餐品总价
- `BigDecimal packagingFee` - 打包费（固定1.00元）
- `BigDecimal deliveryFee` - 配送费（固定3.00元）
- `BigDecimal finalAmount` - 最终金额

**常量**:
```java
public static final BigDecimal PACKAGING_FEE = new BigDecimal("1.00");
public static final BigDecimal DELIVERY_FEE = new BigDecimal("3.00");
```

**工厂方法**:
```java
public static Pricing calculate(List<OrderItem> items) {
    BigDecimal itemsTotal = items.stream()
        .map(OrderItem::subtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal finalAmount = itemsTotal
        .add(PACKAGING_FEE)
        .add(DELIVERY_FEE);
    
    return new Pricing(itemsTotal, PACKAGING_FEE, DELIVERY_FEE, finalAmount);
}
```

#### 1.6 新增值对象

- `MerchantId` - 商家ID值对象
- `DishId` - 餐品ID值对象

#### 1.7 OrderStatus 枚举更新

**新增状态**:
- `PENDING_PAYMENT` - 待支付（替换 CREATED）

**保留状态**:
- `PAID` - 已支付
- `CANCELLED` - 已取消

### 2. Application Layer (应用层)

#### 2.1 CreateOrderService

**职责**: 编排订单创建用例

**依赖**:
- `OrderPersistenceAdapter` - 订单持久化

**方法**:
```java
public CreateOrderResult createOrder(CreateOrderCommand command)
```

**Command 定义**:
```java
public record CreateOrderCommand(
    @NotNull String userId,
    @NotNull String merchantId,
    @NotNull @Size(min = 1) List<OrderItemDto> items,
    @NotNull DeliveryInfoDto deliveryInfo,
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
```

**Result 定义**:
```java
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
```

**业务逻辑**:
1. 验证所有 items 属于同一个 merchantId
2. 将 DTO 转换为领域对象
3. 创建 Order 聚合根
4. 通过 OrderPersistenceAdapter 持久化
5. 返回 CreateOrderResult

**异常处理**:
- `MultiMerchantOrderException` - 订单包含多个商家的餐品
- `ValidationException` - 输入验证失败

### 3. Adapter Layer (适配器层)

#### 3.1 Web Adapter

##### CreateOrderController

**职责**: 处理 HTTP 请求

**Endpoint**: `POST /api/v1/orders`

**Request**:
```java
public record CreateOrderRequest(
    @NotNull String merchantId,
    @NotNull @Size(min = 1) List<OrderItemRequest> items,
    @NotNull DeliveryInfoRequest deliveryInfo,
    @Size(max = 200) String remark
) {
    public record OrderItemRequest(
        @NotNull String dishId,
        @NotNull String dishName,
        @NotNull @Min(1) Integer quantity,
        @NotNull BigDecimal price
    ) {}
    
    public record DeliveryInfoRequest(
        @NotNull String recipientName,
        @NotNull @Pattern(regexp = "^1[3-9]\\d{9}$") String recipientPhone,
        @NotNull @Size(max = 500) String address
    ) {}
}
```

**Response**:
```java
public record CreateOrderResponse(
    int code,
    String message,
    OrderData data
) {
    public record OrderData(
        String orderId,
        String orderNumber,
        String status,
        PricingData pricing,
        String createdAt
    ) {}
    
    public record PricingData(
        BigDecimal itemsTotal,
        BigDecimal packagingFee,
        BigDecimal deliveryFee,
        BigDecimal finalAmount
    ) {}
}
```

**实现**:
```java
@PostMapping("/orders")
@ResponseStatus(HttpStatus.CREATED)
public CreateOrderResponse createOrder(
    @RequestBody @Valid CreateOrderRequest request,
    @AuthenticationPrincipal User user
) {
    return createOrderAdapter.createOrder(request, user);
}
```

##### CreateOrderAdapter

**职责**: 转换 Web 请求/响应与应用层 Command/Result

**方法**:
```java
public CreateOrderResponse createOrder(
    CreateOrderRequest request, 
    User user
)
```

**转换逻辑**:
1. 从 User 提取 userId
2. 将 CreateOrderRequest 转换为 CreateOrderCommand
3. 调用 CreateOrderService
4. 将 CreateOrderResult 转换为 CreateOrderResponse

##### WebExceptionHandler (更新)

**新增异常处理**:
- `MultiMerchantOrderException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request (已存在)

#### 3.2 Persistence Adapter

##### OrderEntity (更新)

**新增字段**:
```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    
    private String orderNumber;
    private String userId;
    private String merchantId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items;
    
    @Embedded
    private DeliveryInfoEmbeddable deliveryInfo;
    
    private String remark;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "itemsTotal", column = @Column(name = "items_total")),
        @AttributeOverride(name = "packagingFee", column = @Column(name = "packaging_fee")),
        @AttributeOverride(name = "deliveryFee", column = @Column(name = "delivery_fee")),
        @AttributeOverride(name = "finalAmount", column = @Column(name = "final_amount"))
    })
    private PricingEmbeddable pricing;
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

##### OrderItemEntity (新增)

```java
@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderId;
    private String dishId;
    private String dishName;
    private int quantity;
    private BigDecimal price;
}
```

##### DeliveryInfoEmbeddable (新增)

```java
@Embeddable
public class DeliveryInfoEmbeddable {
    private String recipientName;
    private String recipientPhone;
    private String address;
}
```

##### PricingEmbeddable (新增)

```java
@Embeddable
public class PricingEmbeddable {
    private BigDecimal itemsTotal;
    private BigDecimal packagingFee;
    private BigDecimal deliveryFee;
    private BigDecimal finalAmount;
}
```

##### OrderPersistenceAdapter (更新)

**更新方法**:
```java
public void save(Order order) {
    OrderEntity entity = toEntity(order);
    orderEntityRepository.save(entity);
}

public Optional<Order> findById(OrderId orderId) {
    return orderEntityRepository.findById(orderId.value())
        .map(this::toDomain);
}
```

**转换方法**:
- `toEntity(Order)` - 领域对象转实体
- `toDomain(OrderEntity)` - 实体转领域对象

## Data Models

### Database Schema

#### orders 表 (更新)

```sql
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    
    -- 配送信息
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(11) NOT NULL,
    address VARCHAR(500) NOT NULL,
    
    -- 备注
    remark VARCHAR(200),
    
    -- 状态
    status VARCHAR(20) NOT NULL,
    
    -- 价格信息
    items_total DECIMAL(10, 2) NOT NULL,
    packaging_fee DECIMAL(10, 2) NOT NULL,
    delivery_fee DECIMAL(10, 2) NOT NULL,
    final_amount DECIMAL(10, 2) NOT NULL,
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_number (order_number),
    INDEX idx_created_at (created_at)
);
```

#### order_items 表 (新增)

```sql
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    dish_id VARCHAR(36) NOT NULL,
    dish_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
);
```

### 数据迁移策略

由于现有表结构与新需求差异较大，建议：

**选项 1: 重建表** (推荐用于开发环境)
- 删除现有 orders 表
- 创建新的 orders 和 order_items 表

**选项 2: 增量迁移** (推荐用于生产环境)
- 添加新字段
- 创建 order_items 表
- 数据迁移脚本
- 删除旧字段

本次实现采用选项 1，创建新的迁移脚本。

## Error Handling

### 异常类型

#### MultiMerchantOrderException

```java
public class MultiMerchantOrderException extends RuntimeException {
    public MultiMerchantOrderException() {
        super("订单只能包含同一商家的餐品");
    }
}
```

#### InvalidDeliveryInfoException

```java
public class InvalidDeliveryInfoException extends RuntimeException {
    public InvalidDeliveryInfoException(String message) {
        super(message);
    }
}
```

### 错误响应格式

```json
{
  "code": 400,
  "message": "请求参数验证失败",
  "errors": [
    {
      "field": "items[0].quantity",
      "message": "数量必须大于0"
    }
  ]
}
```

### HTTP 状态码映射

- 201 Created - 订单创建成功
- 400 Bad Request - 验证失败、业务规则违反
- 401 Unauthorized - 未认证
- 500 Internal Server Error - 系统错误

## Testing Strategy

### 单元测试

#### Domain Layer
- `OrderTest` - 测试订单创建、价格计算、业务规则
- `OrderNumberTest` - 测试订单号生成格式
- `OrderItemTest` - 测试订单项计算
- `PricingTest` - 测试价格计算逻辑
- `DeliveryInfoTest` - 测试配送信息验证

#### Application Layer
- `CreateOrderServiceTest` - 测试订单创建用例
  - 成功创建订单
  - 多商家订单拒绝
  - 验证失败场景

#### Adapter Layer
- `CreateOrderAdapterTest` - 测试请求/响应转换
- `OrderPersistenceAdapterTest` - 测试实体转换和持久化

### 集成测试

- `CreateOrderControllerTest` - 端到端API测试
  - 成功创建订单流程
  - 各种验证失败场景
  - 认证测试

### 测试数据

**有效订单请求**:
```json
{
  "merchantId": "merchant-001",
  "items": [
    {
      "dishId": "dish-001",
      "dishName": "宫保鸡丁",
      "quantity": 2,
      "price": 25.00
    }
  ],
  "deliveryInfo": {
    "recipientName": "张三",
    "recipientPhone": "13800138000",
    "address": "北京市朝阳区xxx街道xxx号"
  },
  "remark": "少辣"
}
```

**预期响应**:
```json
{
  "code": 0,
  "message": "订单创建成功",
  "data": {
    "orderId": "uuid",
    "orderNumber": "20250104120000123456",
    "status": "PENDING_PAYMENT",
    "pricing": {
      "itemsTotal": 50.00,
      "packagingFee": 1.00,
      "deliveryFee": 3.00,
      "finalAmount": 54.00
    },
    "createdAt": "2025-01-04T12:00:00Z"
  }
}
```

## Implementation Notes

### 代码清理策略

为保持代码整洁，需要删除与当前需求无关的示例代码：

**需要删除的文件**:
- `PlaceOrderService.java` - 旧的下单服务
- `PlaceOrderController.java` - 旧的下单控制器
- `PlaceOrderAdapter.java` - 旧的下单适配器
- `PayOrderService.java` - 支付服务（Story 1 不涉及）
- `PayOrderController.java` - 支付控制器（Story 1 不涉及）
- `GetOrderService.java` - 查询服务（Story 1 不涉及）
- `GetOrderController.java` - 查询控制器（Story 1 不涉及）
- `GetOrderAdapter.java` - 查询适配器（Story 1 不涉及）
- `DeductInventoryAdapter.java` - 库存扣减适配器（Story 1 不涉及）
- `DeductInventoryFailedException.java` - 库存异常（Story 1 不涉及）
- `InventoryConfig.java` - 库存配置（Story 1 不涉及）
- 所有相关的测试文件

**需要保留的文件**:
- `Order.java` - 需要重构
- `OrderId.java` - 保留
- `OrderStatus.java` - 需要更新枚举值
- `OrderEntity.java` - 需要重构
- `OrderEntityRepository.java` - 保留
- `OrderPersistenceAdapter.java` - 需要重构
- `UserId.java` - 保留
- `ProductId.java` - 保留（后续可能用到）
- `Identities.java` - 保留
- `SecurityConfig.java` - 保留
- `WebExceptionHandler.java` - 需要更新

**数据库迁移文件处理**:
- 删除 V1-V4 旧的迁移文件
- 创建新的 V1 迁移文件用于 Story 1 的表结构

### 性能考虑

- 订单号生成使用 `ThreadLocalRandom` 保证线程安全
- 使用 `@Transactional` 确保订单和订单项原子性保存
- 为常用查询字段添加数据库索引

### 安全考虑

- 用户只能为自己创建订单（从认证信息获取 userId）
- 所有金额使用 `BigDecimal` 避免精度问题
- 输入验证使用 Bean Validation 注解
- SQL 注入防护由 JPA 自动处理

### 扩展性考虑

- Pricing 设计为值对象，便于后续添加折扣、优惠券逻辑
- OrderItem 独立表，支持复杂餐品配置
- DeliveryInfo 封装，便于后续添加配送时间等字段
