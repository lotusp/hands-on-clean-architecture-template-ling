# HoCATLing 🐾

HoCATLing，Hands-on Clean Architecture Template Ling，即可落地的整洁架构模板轻量级版本，基于 [HoCAT](https://github.com/macdao/hands-on-clean-architecture-template)，适用于小型项目。

## 📋 需求文档

本项目实现了"要吃饱"订餐系统的订单管理功能。详细需求规格请参考：

📄 [需求文档](docs/Requirements.md)

---

## 🏗️ 架构设计

### 设计原则

- **简化项目结构**：不拆分多个独立的组件，所有代码在单一模块中组织
- **简化依赖关系**：刻意不使用 DIP（依赖倒置原则），而是直接依赖实现
- **适用场景**：适合小型项目和快速原型开发

### 架构图

![HoCATLing Diagram](https://www.plantuml.com/plantuml/svg/ZPJFYXin3CRlVWezGFC29OJT3TrUsbAoXpqiIt76siJ48xcofcKeUVSsBidyyHZsbA3lztsIXPYxf5QqQuD99q_HYct1uPljWZuowJVR8ZnwiR1bXn_WAnEdQ1jq8tw7ZLew17nWSIXFsWTShn-u8sUbtsp0sNIiE6npEiY5t79WcRYUZrvnoNGPh6n2BAqDxsW2dyNs8sxBRMH2qZdtnH-EMeDlGy2UWpD7xn0cyoH5GTO-eZ5o7GMkm9JzOm2QQpBO68Dlh7gch00C_lj1UqBvvHiq06SpzJiR5UNZRzmN_YsJ2eU0CXEUSVzZXpyxJFhlDqMcX1aP7B3QB69bqlt_2Gf_3XYhYtcmNgsctDd0g91poaKiBo6Y99yKgS5Y6JkCdBooz3FX4wdNN1mnb-TdOKv_7rgCdrQMy-13O9qQ6kMdbs_DJ6bTNa4JF9AMfv3BYyNA9MJjieOwLMQDLpAfmEgML1ShRUfRcNMjS1juSTnQJw_bPGtAxM--o7BTSsBQ-coCUMcOtloXazp-DUVVFW5Wp1sIwzhm5m00)

<details>
<summary>查看 PlantUML 源码</summary>

```plantuml
@startuml
skinparam defaultFontName Fira Code, Monospaced
skinparam RectangleBorderStyle<<Boundary>> dashed
skinparam RectangleBackgroundColor<<Boundary>> White
skinparam RectangleFontStyle<<Boundary>> normal
skinparam RectangleBackgroundColor Gray
skinparam ComponentBackgroundColor LightGray
skinparam ComponentFontStyle bold
hide <<Boundary>> stereotype

rectangle Boundary <<Boundary>> {
  component application {
    rectangle ApplicationService
  }

  component adapter:web {
    rectangle Controller
    rectangle Web_Request_Response
    Controller -> Web_Request_Response
    rectangle WebAdapter
    Controller --> WebAdapter
    Web_Request_Response <-- WebAdapter
  }
  WebAdapter --> ApplicationService

  component adapter:persistence {
    rectangle Repository
    rectangle Entity
    Repository -> Entity
    rectangle PersistenceAdapter
    PersistenceAdapter --> Repository
    PersistenceAdapter --> Entity
  }
  ApplicationService --> PersistenceAdapter

  component adapter:client {
    rectangle Client
    rectangle Client_Request_Response
    Client -> Client_Request_Response
    rectangle ClientAdapter
    ClientAdapter --> Client
    ClientAdapter --> Client_Request_Response
  }
  ApplicationService --> ClientAdapter

  ApplicationService -> [domain]
  WebAdapter --> [domain]
  [domain] <-- PersistenceAdapter
  [domain] <-- ClientAdapter
}

[configuration] --> Boundary

@enduml
```

</details>

### 分层说明

| 层级 | 职责 | 示例组件 |
|------|------|---------|
| **adapter:web** | 处理 HTTP 请求和响应 | Controller, WebAdapter, Request/Response |
| **application** | 业务逻辑编排 | ApplicationService |
| **adapter:persistence** | 数据持久化 | Repository, Entity, PersistenceAdapter |
| **adapter:client** | 外部服务调用 | Client, ClientAdapter |
| **domain** | 领域模型和业务规则 | Domain Objects |
| **configuration** | 应用配置和依赖注入 | Spring Configuration |

---

## 🚀 快速开始

### 前置条件

#### 最低要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| **Java** | 21+ | 必需，用于编译和运行应用 |
| **Gradle** | 8.x+ | 自动包含（使用 Gradle Wrapper） |

#### 可选组件

| 组件 | 用途 | 何时需要 |
|------|------|---------|
| **Docker** | 运行 MySQL 容器 | 使用 MySQL 模式时 |
| **Docker Compose** | 管理容器编排 | 使用 MySQL 模式时 |

### 本地运行测试

#### 模式 1：H2 内存数据库（推荐）

**适用场景**：快速开发、功能测试、演示

```bash
# 启动应用
./gradlew bootRun

# 访问 H2 控制台（可选）
# 浏览器打开: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# User: sa, Password: (留空)
```

#### 模式 2：MySQL 数据库

**适用场景**：集成测试、生产环境模拟、数据持久化需求

```bash
# 启动应用（自动启动 MySQL 容器）
./gradlew bootRun --args='--spring.profiles.active=mysql'

# 查看 MySQL 数据（可选）
docker exec -it app-mysql-1 mysql -u root testdb
```

**Docker Compose 配置**：
- 配置文件：`app/compose.yaml`
- 镜像：`mysql:lts`
- 数据库：`testdb`
- 端口：动态映射

---

## 🔨 构建和打包

### 编译项目

```bash
# 编译 Java 代码
./gradlew compileJava

# 编译测试代码
./gradlew compileTestJava
```

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行契约测试
./gradlew contractTest

# 查看测试报告
open app/build/reports/tests/test/index.html
```

### 构建项目

```bash
# 完整构建（编译 + 测试 + 打包）
./gradlew build

# 跳过测试的构建
./gradlew build -x test
```

### 打包 Docker 镜像

```bash
# 使用 Spring Boot Gradle 插件构建镜像
./gradlew bootBuildImage

# 指定镜像名称和标签
./gradlew bootBuildImage --imageName=hocatling:latest

# 查看构建的镜像
docker images | grep hocatling
```
