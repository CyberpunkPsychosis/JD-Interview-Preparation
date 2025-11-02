# 如何运行JVM项目

## 问题：为什么 `java -cp target/classes` 找不到类？

### 原因分析

你的命令：
```bash
java -cp target/classes com.jd.jvm.day02.ObjectCreationDemo
```

**问题所在**：
- `-cp target/classes` 只包含了你自己编译的 `.class` 文件
- **但是缺少了项目依赖的jar包**（如 `jol-core-0.17.jar`）
- ObjectCreationDemo 使用了 `org.openjdk.jol.*` 包，JVM找不到这些类，所以报错

类比理解：
```
你的程序 = 你的代码 + 第三方库
java -cp target/classes  ← 只有你的代码，没有第三方库 ❌
java -cp "target/classes:lib/*"  ← 有你的代码 + 第三方库 ✅
```

---

## 解决方案

### 方案1：使用Maven运行（强烈推荐）⭐

Maven会自动处理所有依赖：

```bash
cd 01-java-basic/jvm-tuning

# 编译并运行
mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day02.ObjectCreationDemo"
```

**优点**：
- ✅ 自动下载和管理依赖
- ✅ 不需要手动配置classpath
- ✅ 适合团队协作

---

### 方案2：使用java -cp（需要手动处理依赖）

#### 步骤1: 复制依赖jar包到本地

```bash
cd 01-java-basic/jvm-tuning

# 让Maven把所有依赖jar包复制到 target/lib 目录
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
```

#### 步骤2: 运行时包含所有jar包

```bash
# macOS/Linux
java -cp "target/classes:target/lib/*" com.jd.jvm.day02.ObjectCreationDemo

# Windows
java -cp "target/classes;target/lib/*" com.jd.jvm.day02.ObjectCreationDemo
```

**说明**：
- `target/classes` - 你的编译后的类
- `target/lib/*` - 所有依赖的jar包
- `:` (Linux/Mac) 或 `;` (Windows) - classpath分隔符

---

### 方案3：使用脚本运行（最方便）

我已经为你创建了 `run.sh` 脚本：

```bash
cd 01-java-basic/jvm-tuning
./run.sh
```

---

## classpath 详解

### 什么是classpath？

classpath 告诉JVM去哪里找 `.class` 文件和jar包。

### classpath的组成：

```
-cp "path1:path2:path3"
     ├─ path1: 你的编译输出目录（target/classes）
     ├─ path2: 第三方jar包目录（target/lib/*）
     └─ path3: 其他需要的路径
```

### 常见错误示例：

```bash
# ❌ 错误1: 只包含编译输出，缺少依赖
java -cp target/classes com.jd.jvm.day02.ObjectCreationDemo
# Error: NoClassDefFoundError: org/openjdk/jol/info/ClassLayout

# ❌ 错误2: 忘记引号，通配符失效
java -cp target/classes:target/lib/* com.jd.jvm.day02.ObjectCreationDemo
# 可能只会加载一个jar

# ✅ 正确: 包含所有路径，使用引号
java -cp "target/classes:target/lib/*" com.jd.jvm.day02.ObjectCreationDemo
```

---

## 运行项目中的其他程序

### Day01 - MemoryStructureDemo

```bash
# 使用Maven
mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day01.MemoryStructureDemo"

# 或者（不需要依赖）
javac -encoding UTF-8 -d target/classes src/main/java/com/jd/jvm/day01/MemoryStructureDemo.java
java -cp target/classes com.jd.jvm.day01.MemoryStructureDemo
```

**注意**：MemoryStructureDemo **不依赖第三方库**，所以可以直接用 `java -cp target/classes` 运行。

### Day01 - MemoryOverheadDemo

```bash
# 不需要第三方依赖
java -Xmx24m -Xms24m -cp target/classes com.jd.jvm.day01.MemoryOverheadDemo
```

### Day02 - ObjectCreationDemo

```bash
# 需要JOL依赖，必须用Maven或包含lib/*
mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day02.ObjectCreationDemo"
```

---

## 快速参考

| 程序 | 是否需要依赖 | 推荐运行方式 |
|------|------------|-------------|
| MemoryStructureDemo | ❌ | `java -cp target/classes com.jd.jvm.day01.MemoryStructureDemo` |
| MemoryOverheadDemo | ❌ | `java -Xmx24m -cp target/classes com.jd.jvm.day01.MemoryOverheadDemo` |
| ObjectCreationDemo | ✅ | `mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day02.ObjectCreationDemo"` |

---

## 常用Maven命令

```bash
# 清理
mvn clean

# 编译
mvn compile

# 运行
mvn exec:java -Dexec.mainClass="完整类名"

# 复制依赖
mvn dependency:copy-dependencies

# 打包
mvn package
```

---

## 总结

**你的原始问题**：
```bash
java -cp target/classes com.jd.jvm.day02.ObjectCreationDemo
```

**为什么报错**：缺少 JOL 依赖的jar包

**快速解决**：
```bash
mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day02.ObjectCreationDemo"
```

**记住**：
- 有第三方依赖 → 用Maven运行
- 没有第三方依赖 → 可以直接用java命令
