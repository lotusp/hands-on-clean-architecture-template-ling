# Requirements Document

## Introduction

本需求文档定义"要吃饱"订餐系统中用户下订单功能的 Story 1 实现规格。该功能实现基本的订单创建能力，允许已登录用户选择商家餐品、填写配送信息并创建订单，系统自动计算订单价格并生成订单记录。

## Glossary

- **System**: 要吃饱订餐平台系统
- **User**: 已注册并登录的平台用户
- **Order**: 用户提交的餐品购买订单实体
- **Merchant**: 提供餐品的商家店铺
- **Dish**: 商家提供的菜品
- **OrderItem**: 订单中的单个餐品项，包含餐品信息和数量
- **DeliveryInfo**: 配送信息，包含收货人和地址
- **OrderNumber**: 订单号，格式为 yyyyMMddHHmmss + 6位随机数
- **OrderStatus**: 订单状态枚举值
- **Pricing**: 订单价格信息，包含各项费用明细

## Requirements

### Requirement 1: 用户创建基本订单

**User Story:** 作为一个已登录用户，我想要选择商家餐品并提交订单，以便购买我喜欢的餐品。

#### Acceptance Criteria

1. WHEN User提交包含merchantId、items列表、deliveryInfo和可选remark的订单请求，THE System SHALL创建一个具有唯一orderId的新Order
2. WHEN Order创建时，THE System SHALL生成格式为"yyyyMMddHHmmss + 6位随机数"的orderNumber
3. WHEN Order创建时，THE System SHALL将Order的status设置为"PENDING_PAYMENT"
4. IF User提交的订单请求缺少必填字段（merchantId、items、deliveryInfo中的任一字段），THEN THE System SHALL拒绝该请求并返回包含字段名称的验证错误信息
5. IF User选择的items来自多个不同的Merchant，THEN THE System SHALL拒绝该订单并返回错误信息"订单只能包含同一商家的餐品"
6. WHEN User提交订单请求，THE System SHALL验证items列表至少包含一个OrderItem
7. WHEN User提交订单请求，THE System SHALL验证每个OrderItem的quantity大于0
8. WHEN User提交订单请求，THE System SHALL验证deliveryInfo中的recipientPhone符合中国手机号格式（^1[3-9]\\d{9}$）
9. WHEN User提交订单请求且remark字段存在，THE System SHALL验证remark长度不超过200字符
10. WHEN User提交订单请求，THE System SHALL验证deliveryInfo中的address长度不超过500字符

### Requirement 2: 基本价格计算

**User Story:** 作为用户，我想要看到订单的总价格，以便了解需要支付的金额。

#### Acceptance Criteria

1. WHEN Order创建时，THE System SHALL计算itemsTotal为所有OrderItem的price乘以quantity之和
2. WHEN Order创建时，THE System SHALL设置packagingFee为1.00元
3. WHEN Order创建时，THE System SHALL设置deliveryFee为3.00元
4. WHEN Order创建时，THE System SHALL计算finalAmount为itemsTotal加packagingFee加deliveryFee
5. WHEN Order存储到数据库时，THE System SHALL确保所有金额字段（itemsTotal、packagingFee、deliveryFee、finalAmount、OrderItem的price）保留2位小数精度

### Requirement 3: 订单数据完整性

**User Story:** 作为系统管理员，我需要确保订单数据完整准确，以便进行订单追踪和问题排查。

#### Acceptance Criteria

1. WHEN Order创建时，THE System SHALL记录userId、merchantId和items列表（每个OrderItem包含dishId、dishName、quantity、price）
2. WHEN Order创建时，THE System SHALL记录deliveryInfo中的recipientName、recipientPhone和address
3. WHEN Order创建时且remark字段存在，THE System SHALL记录remark文本
4. WHEN Order创建时，THE System SHALL记录createdAt时间戳为当前UTC时间
5. WHEN Order创建时，THE System SHALL持久化packagingFee为1.00元和deliveryFee为3.00元
6. WHEN Order创建成功，THE System SHALL返回HTTP状态码201和包含orderId、orderNumber、status、pricing明细和createdAt的响应体
7. WHEN Order创建失败由于验证错误，THE System SHALL返回HTTP状态码400和包含错误详情的响应体
