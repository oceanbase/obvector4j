# Contributing to obvector4j

感谢您对 obvector4j 的关注！本文档介绍如何参与项目贡献。

## 开发环境

- JDK 8
- Maven 3.6+
- Docker（集成测试需要）

## 快速开始

```bash
git clone https://github.com/oceanbase/obvector4j.git
cd obvector4j
make build          # 编译打包
make unit-test      # 单元测试（无需 Docker）
make test           # 集成测试（需要 Docker）
make format-check   # 代码风格检查
```

## 代码规范

### 许可证头

所有 Java 源文件必须包含 Mulan PSL v2 许可证头：

```java
/*
 * Copyright (c) 2024 OceanBase. All rights reserved.
 *
 * obvector4j is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 *     http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 */
```

### 代码风格

项目使用 [Checkstyle](config/checkstyle.xml) 进行代码风格检查，主要规则：

- 禁止 Tab 缩进，统一使用 4 空格
- 行尾不能有多余空格
- 单行长度不超过 150 字符
- 禁止未使用的 import 和 `*` 通配符 import
- `if`/`else`/`for`/`while` 语句必须使用大括号
- `catch` 块不能为空（至少要有注释）
- `equals()` 和 `hashCode()` 必须成对出现

提交前运行 `make format-check` 确保通过。

### 异常处理

- 捕获具体异常（如 `SQLException`），不要捕获 `Throwable`
- 不要使用 `e.printStackTrace()`，使用 SLF4J 日志或 JUnit 断言
- 测试方法抛出异常优于 try-catch

## 提交规范

### Commit Message

使用清晰的英文描述：

```
Add license headers to all Java source files
Fix NPE in ObVecClient.createCollection
Refactor hybrid search DSL builder
```

### PR 流程

1. Fork 仓库并创建分支：`git checkout -b fix/your-feature`
2. 确保通过所有检查：`make format-check && make unit-test`
3. 提交 PR，描述改动内容和动机

## 测试

### 单元测试

放在 `src/test/java/.../unit/` 下，不需要外部依赖：

```bash
make unit-test
```

### 集成测试

放在 `src/test/java/.../integration/container/` 下，使用 Testcontainers：

```bash
make test
```

### 远程测试

连接真实 OceanBase 集群：

```bash
export OCEANBASE_REMOTE_IT=1
export OCEANBASE_HOST=<host>
export OCEANBASE_PORT=<port>
export OCEANBASE_USER=<user>
export OCEANBASE_PASSWORD=<password>
export OCEANBASE_DATABASE=<database>
mvn test -Premote-it
```

## 项目结构

```
src/main/java/com/oceanbase/obvector4j/
├── ObVecClient.java          # 主入口
├── schema/                   # 表结构与索引
├── model/                    # JDBC 值类型
├── hybrid/                   # 混合搜索
│   └── core/                 # 4.6.0+ HYBRID_SEARCH DSL
├── filter/                   # 过滤条件 DSL
├── version/                  # 版本检测
├── json_table/               # JSON 虚拟表
└── util/                     # 工具类
```

## 许可证

贡献的代码将遵循 [Mulan PSL v2](LICENSE) 许可证。
