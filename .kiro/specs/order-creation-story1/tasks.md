# Implementation Plan

- [x] 1. 清理旧代码和数据库迁移文件
  - 删除与 Story 1 无关的示例代码文件
  - 删除旧的数据库迁移文件
  - 清理相关的测试文件
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. 创建领域层核心值对象
  - [ ] 2.1 创建 MerchantId 值对象
    - 实现 record 类型的 MerchantId
    - _Requirements: 1.1_
  
  - [ ] 2.2 创建 DishId 值对象
    - 实现 record 类型的 DishId
    - _Requirements: 1.1_
  
  - [ ] 2.3 创建 OrderNumber 值对象
    - 实现订单号生成逻辑（yyyyMMddHHmmss + 6位随机数）
    - 使用 ThreadLocalRandom 保证线程安全
    - _Requirements: 1.1.2_
  
  - [ ] 2.4 创建 DeliveryInfo 值对象
    - 包含 recipientName、recipientPhone、address 字段
    - 实现手机号格式验证逻辑
    - 实现地址长度验证
    - _Requirements: 1.1.8, 1.1.10, 1.3.2_
  
  - [ ] 2.5 创建 OrderItem 值对象
    - 包含 dishId、dishName、quantity、price 字段
    - 实现 subtotal() 方法计算小计
    - 实现数量和价格验证
    - _Requirements: 1.1.7, 1.2.1, 1.3.1_
  
  - [ ] 2.6 创建 Pricing 值对象
    - 包含 itemsTotal、packagingFee、deliveryFee、finalAmount 字段
    - 定义打包费和配送费常量
    - 实现静态工厂方法 calculate(List<OrderItem>)
    - _Requirements: 1.2.1, 1.2.2, 1.2.3, 1.2.4_

- [ ] 3. 重构 Order 领域模型
  - [ ] 3.1 更新 Order 类字段
    - 添加 orderNumber、merchantId、items、deliveryInfo、remark、pricing 字段
    - 移除 productId、quantity、price 字段
    - _Requirements: 1.3.1, 1.3.2, 1.3.3_
  
  - [ ] 3.2 实现 Order 构造方法
    - 创建接受 userId、merchantId、items、deliveryInfo、remark 的构造方法
    - 自动生成 OrderNumber
    - 设置状态为 PENDING_PAYMENT
    - 调用 Pricing.calculate() 计算价格
    - 验证 items 至少包含一项
    - 验证所有 items 属于同一商家
    - _Requirements: 1.1.1, 1.1.2, 1.1.3, 1.1.5, 1.1.6, 1.2.4_
  
  - [ ] 3.3 更新 OrderStatus 枚举
    - 添加 PENDING_PAYMENT 状态
    - 保留 PAID、CANCELLED 状态
    - _Requirements: 1.1.3_

- [ ] 4. 创建应用层服务和异常
  - [ ] 4.1 创建 MultiMerchantOrderException
    - 定义异常类和错误消息
    - _Requirements: 1.1.5_
  
  - [ ] 4.2 创建 CreateOrderService
    - 定义 CreateOrderCommand record（包含嵌套的 OrderItemDto 和 DeliveryInfoDto）
    - 定义 CreateOrderResult record（包含嵌套的 PricingDto）
    - 实现 createOrder(CreateOrderCommand) 方法
    - 验证所有 items 的 merchantId 一致
    - 将 DTO 转换为领域对象
    - 创建 Order 并持久化
    - 返回 CreateOrderResult
    - _Requirements: 1.1.1, 1.1.4, 1.1.5, 1.3.6_

- [ ] 5. 创建数据库迁移和持久化层
  - [ ] 5.1 创建数据库迁移脚本
    - 创建 V1__Create_orders_and_order_items_tables.sql
    - 定义 orders 表结构（包含所有必需字段和索引）
    - 定义 order_items 表结构（包含外键约束）
    - _Requirements: 1.3.4, 1.3.5_
  
  - [ ] 5.2 创建 OrderItemEntity
    - 定义 JPA 实体类
    - 配置与 orders 表的关联关系
    - _Requirements: 1.3.1_
  
  - [ ] 5.3 创建 DeliveryInfoEmbeddable
    - 定义 @Embeddable 类
    - _Requirements: 1.3.2_
  
  - [ ] 5.4 创建 PricingEmbeddable
    - 定义 @Embeddable 类
    - 配置字段映射
    - _Requirements: 1.2.5, 1.3.5_
  
  - [ ] 5.5 重构 OrderEntity
    - 更新字段映射新的 Order 模型
    - 配置 @OneToMany 关系到 OrderItemEntity
    - 配置 @Embedded 关系到 DeliveryInfoEmbeddable 和 PricingEmbeddable
    - _Requirements: 1.3.1, 1.3.2, 1.3.3, 1.3.4, 1.3.5_
  
  - [ ] 5.6 重构 OrderPersistenceAdapter
    - 更新 save() 方法的实体转换逻辑
    - 更新 findById() 方法的领域对象转换逻辑
    - 实现 toEntity(Order) 私有方法
    - 实现 toDomain(OrderEntity) 私有方法
    - _Requirements: 1.3.1, 1.3.2, 1.3.3, 1.3.4, 1.3.5_

- [ ] 6. 创建 Web 适配器层
  - [ ] 6.1 创建 CreateOrderController
    - 定义 CreateOrderRequest record（包含嵌套的 OrderItemRequest 和 DeliveryInfoRequest）
    - 添加 Bean Validation 注解
    - 实现 POST /api/v1/orders 端点
    - 从 @AuthenticationPrincipal 获取用户信息
    - 返回 201 Created 状态码
    - _Requirements: 1.1.4, 1.1.7, 1.1.8, 1.1.9, 1.1.10, 1.3.6_
  
  - [ ] 6.2 创建 CreateOrderAdapter
    - 定义 CreateOrderResponse record（包含嵌套的 OrderData 和 PricingData）
    - 实现 createOrder(CreateOrderRequest, User) 方法
    - 将 CreateOrderRequest 转换为 CreateOrderCommand
    - 调用 CreateOrderService
    - 将 CreateOrderResult 转换为 CreateOrderResponse
    - _Requirements: 1.3.6_
  
  - [ ] 6.3 更新 WebExceptionHandler
    - 添加 MultiMerchantOrderException 的处理（返回 400）
    - 确保 MethodArgumentNotValidException 正确处理
    - 返回统一的错误响应格式
    - _Requirements: 1.1.4, 1.1.5, 1.3.7_

- [ ] 7. 集成测试
  - [ ] 7.1 编写 CreateOrderControllerTest
    - 测试成功创建订单场景
    - 测试缺少必填字段的验证
    - 测试多商家订单拒绝
    - 测试手机号格式验证
    - 测试备注长度验证
    - 测试地址长度验证
    - 测试数量验证
    - 验证返回的价格计算正确
    - 验证返回的订单号格式正确
    - _Requirements: 1.1.1, 1.1.2, 1.1.3, 1.1.4, 1.1.5, 1.1.6, 1.1.7, 1.1.8, 1.1.9, 1.1.10, 1.2.1, 1.2.2, 1.2.3, 1.2.4, 1.3.6_
