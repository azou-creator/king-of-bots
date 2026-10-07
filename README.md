# King of Bots

一个实时贪吃蛇对战平台：两名玩家在同一张随机生成的地图上同时控制自己的蛇，可以选择**亲自出战**（键盘 WASD / 方向键），也可以**编写自己的 Java Bot** 由 AI 代为出战。内置匹配系统、天梯排行与完整对局回放。

## 功能特性

- **匹配系统**：独立的匹配服务，按天梯分数与等待时长自动配对，支持取消匹配与断线自动移出队列
- **实时对战**：WebSocket 驱动，双方每回合同步移动；每回合 5 秒倒计时，超时判负；头顶/身体/边界碰撞判定与尾巴增长规则全部由服务端裁判
- **Bot 对战**：用户在网页编辑器中编写 Java Bot，运行时内存编译并沙箱执行（支持嵌套类，编译错误输出到日志），内置两套示例算法：
  - `AlphaBetaSnake`：迭代加深 + Alpha-Beta 剪枝，Voronoi 领地评估
  - `MinimaxSnake`：纯 Minimax 全展开，生存空间贪心评估
- **对局回放**：每局记录地图与双方全部步数，可完整回放
- **天梯排行**：对局结果实时更新分数
- **个人中心**：Bot 的增删改查、头像上传

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3、Vite、Vuex、Vue Router、Ant Design Vue、Canvas |
| 后端 | Spring Boot 3.1、Spring Security + JWT、Spring Data JPA、WebSocket |
| 服务间通信 | HTTP REST（内网 IP 白名单） |
| 数据库 | MySQL 8（UTF-8MB4） |
| Bot 运行时 | `javax.tools` 内存编译器 + 独立线程执行（超时 2s） |

## 架构

后端由三个独立部署的 Spring Boot 服务组成：

```
浏览器 ──WebSocket──▶ backend (8080) ◀──HTTP── botruningsystem (8082)
                        │  ▲
              HTTP ▼    │  │ HTTP
              matchingsystem (8081)
                        │
                      MySQL
```

1. 玩家在匹配页发起请求，`backend` 转发给 `matchingsystem`；
2. 匹配成功后回调 `backend` 的 `/startGame/start`，创建对局线程；
3. 每回合对局把地图与双方历史步数发给 `botruningsystem`，运行时编译并执行用户 Bot，结果回传 `/receiveBotMove/move`；
4. 每步 5 秒窗口内未交步的玩家判负，对局结束保存 `Record`，前端按记录回放。

服务间接口不使用 JWT，而是通过 `IpAddressAuthorizationManager` 限制仅本机（127.0.0.1）调用。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 16+ 与 pnpm
- MySQL 8

### 1. 准备数据库

可以用 Docker 一键启动（账号密码与 `application.yml` 默认配置一致）：

```bash
docker run -d --name kob-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=kob \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

业务表（`user` / `bot` / `record`）由 JPA 在服务首次启动时自动创建。

### 2. 启动后端（三个服务）

在 IDEA 中分别运行三个启动类，或使用命令行：

```bash
cd backendcloud

# 主服务：REST API / WebSocket / 对局引擎，端口 8080
mvn -pl backend spring-boot:run

# 匹配系统，端口 8081
mvn -pl matchingsystem spring-boot:run

# Bot 运行系统，端口 8082
mvn -pl botruningsystem spring-boot:run
```

> 三个服务必须同时运行，才能完成一次完整的匹配与对局。

### 3. 启动前端

```bash
cd web
pnpm install
pnpm dev
```

访问 `http://localhost:5173`，注册两个账号即可开始匹配对战。

## 编写你自己的 Bot

Bot 是一段实现固定协议的 Java 代码，完整示例见 [`backendcloud/bots/`](backendcloud/bots/)（Alpha-Beta 与 Minimax 两种实现）。协议要点：

```java
package com.kob.botruningsystem.reflect;

import java.util.Scanner;
import java.util.function.Supplier;

public class Bot implements Supplier<Integer> {
    @Override
    public Integer get() {
        // 从 input.txt 读取局面, 返回 0~3 表示移动方向
        Scanner scanner = new Scanner(new File("input.txt"));
        return nextMove(scanner.nextLine());
    }
}
```

- 输入格式：`地图#我的起点x#我的起点y#(我的历史步)#对方起点x#对方起点y#(对方历史步)`，地图为 13×14 的墙字符串，蛇身需由起点与步数回放得出
- 返回值：`0` 上、`1` 右、`2` 下、`3` 左（`dx={-1,0,1,0}, dy={0,1,0,-1}`）
- 裁判规则：双方同时移动，按移动后身体判碰撞；头对头不判死；前 10 步每步增长，之后每 3 步增长一次
- 每步总预算 2 秒（含编译），超时判负
- 类名必须为 `Bot` 且实现 `Supplier<Integer>`（运行时会自动改写类名避免并发冲突）

## 项目结构

```
├── backendcloud/               # 后端（Maven 多模块）
│   ├── backend/                # 主服务: REST / WebSocket / 对局引擎 / 静态头像
│   ├── matchingsystem/         # 匹配系统微服务
│   ├── botruningsystem/        # Bot 编译运行微服务
│   └── bots/                   # 内置示例 Bot 源码
├── web/                        # 前端 (Vue 3 + Vite)
└── AGENTS.md                   # 面向 AI 辅助开发的项目约定与已知坑
```

## 界面预览

### 登录页
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/f3b0f6c6-d8de-4087-bc95-6d6058280702)
### 匹配页
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/7d5e18ef-93d2-4863-b859-beec4418fa01)
### 对战页面
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/de3afbf8-fe06-4f5e-bd29-45e9723bd348)
### 对局列表
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/18f695b2-d963-43af-b693-7344f487d0c6)
### 排行榜
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/df959502-4cbb-4d4c-ae96-7de313025645)
### 个人中心
![image](https://github.com/haonanaJava/king-of-bots/assets/57832817/26bb4a14-d6a9-4234-8e18-b2e8af8f2589)

## License

[MIT](LICENSE)
