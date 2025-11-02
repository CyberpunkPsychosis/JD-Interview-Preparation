# 📅 Week 1: JVM原理与内存管理 (Day 4-7)

> **学习周期**: 第1周 (Day 4-7)
> **主题**: 垃圾收集器详解、性能对比、类加载机制、周总结项目
> **目标**: 掌握各类GC收集器特性,理解类加载机制,完成JVM调优实战项目

---

## 📖 Day 4 (周四): 垃圾收集器详解

### 🎯 学习目标

1. 掌握7种垃圾收集器的特点和使用场景
2. 理解CMS的4个阶段和G1的Region概念
3. 能够根据业务场景选择合适的GC收集器
4. 理解ZGC的超低延迟原理

### 📚 核心知识点

#### 1. Serial 收集器 (单线程,简单高效)

**特点**:
- **单线程收集器**: 进行GC时,必须暂停所有工作线程(Stop The World)
- **新生代**: Serial收集器 (标记-复制算法)
- **老年代**: Serial Old收集器 (标记-整理算法)
- **适用场景**: 客户端模式,桌面应用

**VM参数**:
```bash
-XX:+UseSerialGC
```

**优点**:
- 简单高效,单核场景下效率最高
- 内存开销小

**缺点**:
- STW时间较长,不适合服务端

#### 2. ParNew 收集器 (Serial的多线程版本)

**特点**:
- **多线程并行收集**: Serial的多线程版本
- **新生代收集器**: 配合CMS使用
- **算法**: 标记-复制

**VM参数**:
```bash
-XX:+UseParNewGC
```

**适用场景**: 与CMS配合使用

#### 3. Parallel Scavenge 收集器 (吞吐量优先)

**特点**:
- **关注吞吐量**: 吞吐量 = 运行用户代码时间 / (运行用户代码时间 + GC时间)
- **新生代收集器**: 多线程并行
- **自适应调节**: -XX:+UseAdaptiveSizePolicy

**VM参数**:
```bash
# 使用Parallel GC (新生代+老年代)
-XX:+UseParallelGC

# 设置GC线程数
-XX:ParallelGCThreads=4

# 设置最大GC停顿时间 (毫秒)
-XX:MaxGCPauseMillis=200

# 设置吞吐量大小 (0-100)
-XX:GCTimeRatio=99  # 表示99%时间用于业务,1%用于GC
```

**适用场景**:
- 后台计算任务
- 批处理任务
- 不需要太多交互的服务

#### 4. CMS 收集器 (并发低停顿) ⭐⭐⭐

**特点**:
- **目标**: 获取最短回收停顿时间
- **算法**: 标记-清除
- **并发收集**: 与用户线程同时工作

**4个阶段**:

```
1. 初始标记 (Initial Mark) - STW ⏸
   └─ 标记GC Roots直接关联的对象
   └─ 速度很快

2. 并发标记 (Concurrent Mark) - 并发 ✅
   └─ 从GC Roots开始遍历整个对象图
   └─ 耗时最长,但与用户线程并发执行

3. 重新标记 (Remark) - STW ⏸
   └─ 修正并发标记期间变动的对象
   └─ 比初始标记稍长,但远短于并发标记

4. 并发清除 (Concurrent Sweep) - 并发 ✅
   └─ 清除死亡对象
   └─ 与用户线程并发执行
```

**VM参数**:
```bash
# 启用CMS
-XX:+UseConcMarkSweepGC

# 老年代使用率达到80%触发CMS
-XX:CMSInitiatingOccupancyFraction=80

# 启用增量式CMS (已废弃)
-XX:+CMSIncrementalMode

# 设置CMS线程数
-XX:ConcGCThreads=4

# CMS GC后进行内存整理
-XX:+UseCMSCompactAtFullCollection
```

**优点**:
- 并发收集,停顿时间短
- 适合对响应时间敏感的应用

**缺点**:
1. **CPU敏感**: 并发阶段会占用CPU资源
2. **浮动垃圾**: 并发标记阶段产生的垃圾无法清理,留到下次GC
3. **内存碎片**: 标记-清除算法导致空间碎片

#### 5. G1 收集器 (JDK9默认,面向服务端) ⭐⭐⭐⭐⭐

**核心概念**:

**Region 分区**:
```
堆内存划分为多个大小相等的Region (1MB-32MB)
┌─────────────────────────────────────────┐
│ E  E  E  S  S  O  O  O  H  H  -  -  -  │
└─────────────────────────────────────────┘
  E = Eden区
  S = Survivor区
  O = Old区
  H = Humongous区 (大对象,>Region的50%)
  - = 空闲区
```

**特点**:
- **可预测停顿**: 可设置期望停顿时间 `-XX:MaxGCPauseMillis=200`
- **分代收集**: 但不再物理分代,而是Region角色动态变化
- **空间整合**: 整体基于标记-整理,Region之间基于复制
- **优先级回收**: 优先回收价值最大的Region (Garbage First)

**Mixed GC 过程**:
```
1. 初始标记 (Initial Mark) - STW
2. 并发标记 (Concurrent Mark) - 并发
3. 最终标记 (Final Mark) - STW
4. 筛选回收 (Live Data Counting and Evacuation) - STW
   └─ 根据停顿时间,选择部分Region回收
```

**VM参数**:
```bash
# 启用G1 (JDK9+默认)
-XX:+UseG1GC

# 设置期望停顿时间 (默认200ms)
-XX:MaxGCPauseMillis=200

# 设置Region大小 (1/2/4/8/16/32MB)
-XX:G1HeapRegionSize=16m

# 新生代占堆的最小/最大比例
-XX:G1NewSizePercent=5
-XX:G1MaxNewSizePercent=60

# 触发Mixed GC的老年代占比阈值
-XX:InitiatingHeapOccupancyPercent=45

# 设置并发标记线程数
-XX:ConcGCThreads=4
```

**适用场景**:
- 6GB+ 堆内存
- 需要可预测停顿时间
- 服务端应用 (JD就在用!)

#### 6. ZGC 收集器 (超低延迟) ⭐⭐⭐⭐

**特点**:
- **超低延迟**: 停顿时间不超过10ms
- **支持TB级堆**: 可管理16TB堆内存
- **并发整理**: 并发标记、并发整理、并发重定位
- **着色指针**: 利用指针的高位存储元数据

**核心技术**:

**1. 着色指针 (Colored Pointers)**
```
64位指针布局:
┌─────────────────────────────────────────────┐
│ 未使用 │ 元数据 │       对象地址 (42位)      │
│ 18位   │ 4位    │       支持4TB内存          │
└─────────────────────────────────────────────┘
  元数据4位:
  - Marked0
  - Marked1
  - Remapped
  - Finalizable
```

**2. 读屏障 (Load Barrier)**
- 每次从堆中读取对象引用时,都会经过读屏障
- 读屏障检查指针颜色,必要时进行重定位

**VM参数**:
```bash
# 启用ZGC (JDK11+)
-XX:+UseZGC

# 设置最大堆内存
-Xmx16g

# 启用大页内存 (提升性能)
-XX:+UseLargePages

# 设置并发GC线程数
-XX:ConcGCThreads=4
```

**适用场景**:
- 超大堆内存 (100GB+)
- 对延迟极度敏感 (交易系统、游戏服务器)
- 愿意牺牲一定吞吐量换取低延迟

#### 7. Shenandoah GC (RedHat开发,Oracle JDK无)

**特点**:
- 与ZGC类似,目标是低延迟
- 使用转发指针 (Brooks Pointer) 而非着色指针
- OpenJDK可用,Oracle JDK不包含

### 📊 收集器对比总表

| 收集器 | 分代 | 算法 | 目标 | STW | 适用场景 |
|-------|------|------|------|-----|---------|
| Serial | 新生代 | 复制 | 单核高效 | 是 | 客户端 |
| Serial Old | 老年代 | 标记-整理 | 单核高效 | 是 | 客户端 |
| ParNew | 新生代 | 复制 | 多核 | 是 | 配合CMS |
| Parallel Scavenge | 新生代 | 复制 | 吞吐量 | 是 | 后台计算 |
| Parallel Old | 老年代 | 标记-整理 | 吞吐量 | 是 | 后台计算 |
| CMS | 老年代 | 标记-清除 | 低停顿 | 部分 | 响应敏感 |
| G1 | 新生代+老年代 | 标记-整理+复制 | 可预测停顿 | 部分 | 服务端 |
| ZGC | 全堆 | 标记-整理 | 超低延迟 | <10ms | 超大堆 |

### 📊 收集器组合关系

```
新生代收集器          老年代收集器
┌──────────────┐    ┌──────────────┐
│ Serial       │───→│ Serial Old   │
└──────────────┘    └──────────────┘

┌──────────────┐    ┌──────────────┐
│ ParNew       │───→│ CMS          │
└──────────────┘    └──────────────┘

┌──────────────┐    ┌──────────────┐
│ Parallel     │───→│ Parallel Old │
│ Scavenge     │    │              │
└──────────────┘    └──────────────┘

┌────────────────────────────────────┐
│          G1 (全堆收集)              │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│          ZGC (全堆收集)             │
└────────────────────────────────────┘
```

### 💻 实战代码

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day04/GCCollectorDemo.java`

```java
package com.jd.jvm.day04;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * GC收集器演示
 *
 * 运行参数测试不同收集器:
 *
 * 1. Serial GC:
 *    -XX:+UseSerialGC -Xms512m -Xmx512m -XX:+PrintGCDetails
 *
 * 2. Parallel GC:
 *    -XX:+UseParallelGC -Xms512m -Xmx512m -XX:+PrintGCDetails
 *
 * 3. CMS:
 *    -XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -XX:+PrintGCDetails
 *
 * 4. G1:
 *    -XX:+UseG1GC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:MaxGCPauseMillis=200
 *
 * 5. ZGC (JDK11+):
 *    -XX:+UseZGC -Xms512m -Xmx512m -Xlog:gc
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class GCCollectorDemo {

    // 1MB数据
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== GC收集器演示 ===\n");

        // 打印当前使用的GC收集器
        printGCInfo();

        System.out.println("\n开始分配内存...");

        // 模拟内存分配,触发GC
        testMemoryAllocation();

        System.out.println("\n程序结束,查看GC日志");
    }

    /**
     * 打印当前JVM使用的GC收集器信息
     */
    private static void printGCInfo() {
        System.out.println("📊 当前JVM GC收集器信息:");
        System.out.println("─".repeat(60));

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("收集器名称: %s%n", gcBean.getName());
            System.out.printf("  - 内存池: %s%n", String.join(", ", gcBean.getMemoryPoolNames()));
            System.out.printf("  - GC次数: %d%n", gcBean.getCollectionCount());
            System.out.printf("  - GC耗时: %d ms%n", gcBean.getCollectionTime());
            System.out.println();
        }

        // 打印JVM参数中的GC相关配置
        System.out.println("📌 GC相关VM参数:");
        List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        vmArgs.stream()
              .filter(arg -> arg.contains("GC") || arg.startsWith("-Xm") || arg.contains("UseG1")
                          || arg.contains("UseZGC") || arg.contains("UseSerial")
                          || arg.contains("UseParallel") || arg.contains("UseConcMarkSweep"))
              .forEach(arg -> System.out.println("  " + arg));

        System.out.println("─".repeat(60));
    }

    /**
     * 测试内存分配,触发GC
     */
    private static void testMemoryAllocation() throws InterruptedException {
        List<byte[]> list = new ArrayList<>();

        // 分配256MB数据 (512MB堆的一半)
        for (int i = 0; i < 256; i++) {
            byte[] allocation = new byte[_1MB];
            list.add(allocation);

            if (i % 50 == 0) {
                System.out.printf("已分配: %d MB%n", i);
                Thread.sleep(10);
            }
        }

        System.out.println("✅ 内存分配完成!");

        // 打印最终GC统计
        System.out.println("\n📊 最终GC统计:");
        System.out.println("─".repeat(60));

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("%s: GC次数=%d, 耗时=%d ms%n",
                gcBean.getName(),
                gcBean.getCollectionCount(),
                gcBean.getCollectionTime());
        }
    }
}
```

### 🧪 实战测试

创建测试脚本: `01-java-basic/jvm-tuning/test-gc-collectors.sh`

```bash
#!/bin/bash

# GC收集器对比测试脚本

PROJECT_DIR="/Users/yumeng/IdeaProjects/JD-Interview-Preparation/01-java-basic/jvm-tuning"
cd "$PROJECT_DIR"

# 确保代码已编译
mvn clean compile

echo "=================================="
echo "  GC收集器对比测试"
echo "=================================="

# 测试1: Serial GC
echo -e "\n【测试1】Serial GC (单线程)"
echo "----------------------------------------"
mvn exec:java \
  -Dexec.mainClass="com.jd.jvm.day04.GCCollectorDemo" \
  -Dexec.args="" \
  -Dexec.executable="java" \
  -Dexec.vmArgs="-XX:+UseSerialGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

echo -e "\n按Enter继续下一个测试..."
read

# 测试2: Parallel GC
echo -e "\n【测试2】Parallel GC (并行吞吐量)"
echo "----------------------------------------"
mvn exec:java \
  -Dexec.mainClass="com.jd.jvm.day04.GCCollectorDemo" \
  -Dexec.args="" \
  -Dexec.executable="java" \
  -Dexec.vmArgs="-XX:+UseParallelGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

echo -e "\n按Enter继续下一个测试..."
read

# 测试3: CMS
echo -e "\n【测试3】CMS GC (并发低停顿)"
echo "----------------------------------------"
mvn exec:java \
  -Dexec.mainClass="com.jd.jvm.day04.GCCollectorDemo" \
  -Dexec.args="" \
  -Dexec.executable="java" \
  -Dexec.vmArgs="-XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

echo -e "\n按Enter继续下一个测试..."
read

# 测试4: G1
echo -e "\n【测试4】G1 GC (可预测停顿)"
echo "----------------------------------------"
mvn exec:java \
  -Dexec.mainClass="com.jd.jvm.day04.GCCollectorDemo" \
  -Dexec.args="" \
  -Dexec.executable="java" \
  -Dexec.vmArgs="-XX:+UseG1GC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:MaxGCPauseMillis=200"

echo -e "\n✅ 测试完成!"
echo "请对比不同GC收集器的表现"
```

给脚本添加执行权限:
```bash
chmod +x test-gc-collectors.sh
```

### 📖 深度学习资源

#### 官方文档 (权威必读):

1. ✅ **Oracle - Garbage Collection Tuning**
   - 链接: https://docs.oracle.com/en/java/javase/17/gctuning/
   - 内容: 官方GC调优指南,各收集器详解
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **OpenJDK - ZGC Documentation**
   - 链接: https://wiki.openjdk.org/display/zgc/Main
   - 内容: ZGC官方Wiki,技术细节
   - 推荐指数: ⭐⭐⭐⭐⭐

#### 博客文章 (干货实战):

1. ✅ **美团技术团队 - Java中9种常见的CMS GC问题分析与解决**
   - 链接: https://tech.meituan.com/2020/11/12/java-9-cms-gc.html
   - 内容: 生产环境CMS GC问题案例,非常实用
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **阿里技术 - 一次线上OOM问题排查**
   - 链接: https://developer.aliyun.com/article/780038
   - 内容: 真实生产案例,排查思路
   - 推荐指数: ⭐⭐⭐⭐⭐

3. ✅ **美团技术 - 从实际案例聊聊Java应用的GC优化**
   - 链接: https://tech.meituan.com/2017/12/29/jvm-optimize.html
   - 内容: GC调优完整方法论
   - 推荐指数: ⭐⭐⭐⭐⭐

4. ✅ **阿里中间件 - 新一代垃圾回收器ZGC设计与实现**
   - 链接: https://mp.weixin.qq.com/s/ag5u2EPObx7bZr7hkcrOTg
   - 内容: ZGC技术深度解析
   - 推荐指数: ⭐⭐⭐⭐

#### 视频教程:

1. ✅ **尚硅谷JVM P21-P30: 垃圾收集器详解**
   - 链接: https://www.bilibili.com/video/BV1PJ411n7xZ?p=21
   - 内容:
     - P21-P23: Serial、ParNew、Parallel详解
     - P24-P26: CMS收集器深度讲解
     - P27-P29: G1收集器原理与实战
     - P30: ZGC简介
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **黑马程序员JVM完整版 - GC部分**
   - 链接: https://www.bilibili.com/video/BV1yE411Z7AP?p=15
   - 内容: P15-P25讲解各类GC收集器
   - 推荐指数: ⭐⭐⭐⭐

#### 书籍章节:

1. ✅ **《深入理解Java虚拟机》第3.5节: 垃圾收集器**
   - 作者: 周志明
   - 页码: 第3章
   - 内容: 7种收集器完整介绍
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **《Java性能优化权威指南》第6章: 垃圾收集**
   - 作者: Charlie Hunt, Binu John
   - 内容: GC调优实战
   - 推荐指数: ⭐⭐⭐⭐

### 📝 Anki卡片

#### 卡片1: 7种垃圾收集器对比

**正面**:
```
列出7种垃圾收集器,并说明它们的主要特点和适用场景
```

**背面**:
```
1. Serial - 单线程,适合客户端
2. Serial Old - Serial的老年代版本
3. ParNew - Serial的多线程版本,配合CMS
4. Parallel Scavenge - 吞吐量优先,适合后台计算
5. Parallel Old - Parallel的老年代版本
6. CMS - 并发低停顿,适合响应敏感应用
7. G1 - 可预测停顿,JDK9默认,适合服务端
8. ZGC - 超低延迟(<10ms),支持TB级堆

组合关系:
- Serial + Serial Old
- ParNew + CMS
- Parallel Scavenge + Parallel Old
- G1 (全堆)
- ZGC (全堆)
```

#### 卡片2: CMS的4个阶段

**正面**:
```
CMS垃圾收集器的4个阶段是什么?哪些阶段会STW?
```

**背面**:
```
1. 初始标记 (Initial Mark) - STW ⏸
   └─ 标记GC Roots直接关联的对象
   └─ 速度很快

2. 并发标记 (Concurrent Mark) - 并发 ✅
   └─ 从GC Roots遍历整个对象图
   └─ 耗时最长,与用户线程并发

3. 重新标记 (Remark) - STW ⏸
   └─ 修正并发标记期间变动的对象
   └─ 停顿时间较初始标记稍长

4. 并发清除 (Concurrent Sweep) - 并发 ✅
   └─ 清除死亡对象
   └─ 与用户线程并发

STW阶段: 初始标记、重新标记
并发阶段: 并发标记、并发清除

CMS的3个缺点:
1. CPU敏感 - 并发阶段占用CPU
2. 浮动垃圾 - 并发标记期间产生的垃圾留到下次
3. 内存碎片 - 标记-清除算法导致碎片化
```

#### 卡片3: G1的Region概念

**正面**:
```
G1收集器的Region是什么?如何实现可预测停顿?
```

**背面**:
```
Region概念:
- 将堆划分为多个大小相等的Region (1MB-32MB)
- 每个Region可以是Eden、Survivor、Old、Humongous
- Region角色动态变化,不再物理分代

Region类型:
- E (Eden): 新对象分配区
- S (Survivor): 存活对象区
- O (Old): 老年代区
- H (Humongous): 大对象区 (>Region的50%)

可预测停顿原理:
1. 建立可预测的停顿模型
2. 跟踪各Region的回收价值(回收空间大小和时间)
3. 根据设置的停顿时间 -XX:MaxGCPauseMillis=200
4. 优先回收价值最大的Region (Garbage First)
5. 每次只回收一部分Region,而非全堆

适用场景:
- 6GB+ 堆内存
- 需要可预测停顿时间
- 服务端应用 (京东在用!)
```

#### 卡片4: ZGC的核心技术

**正面**:
```
ZGC如何实现超低延迟(<10ms)?着色指针是什么?
```

**背面**:
```
ZGC核心技术:

1. 着色指针 (Colored Pointers)
   64位指针布局:
   ┌──────────────────────────────────────┐
   │ 未使用 │ 元数据 │   对象地址(42位)   │
   │ 18位   │ 4位    │   支持4TB内存      │
   └──────────────────────────────────────┘

   元数据4位:
   - Marked0/Marked1: 标记信息
   - Remapped: 是否重定位
   - Finalizable: 是否可终结

2. 读屏障 (Load Barrier)
   - 每次读取对象引用都经过读屏障
   - 检查指针颜色,必要时重定位
   - 实现并发整理

3. 并发整理
   - 并发标记、并发整理、并发重定位
   - 几乎所有阶段都并发执行

性能指标:
- 停顿时间: <10ms (不随堆大小增加)
- 支持堆大小: 16TB
- 吞吐量: 略低于G1 (约下降10-15%)

适用场景:
- 超大堆内存 (100GB+)
- 对延迟极度敏感 (交易系统)
- 愿意牺牲一定吞吐量
```

### ✅ 今日任务清单

```markdown
Day 4 学习任务:
- [ ] 阅读《深入理解Java虚拟机》第3.5节
- [ ] 观看尚硅谷JVM P21-P30
- [ ] 完成GCCollectorDemo.java代码
- [ ] 运行test-gc-collectors.sh测试4种GC
- [ ] 对比不同GC的表现,记录数据
- [ ] 阅读美团CMS GC问题博客
- [ ] 阅读阿里ZGC技术文章
- [ ] 制作Anki卡片4张
- [ ] 写今日学习总结
- [ ] LeetCode: 234. 回文链表
```

### 🎓 学习总结模板

```markdown
# Day 4 学习总结

## 今日收获
1. 掌握了7种垃圾收集器的特点
2. 理解了CMS的4阶段和G1的Region概念
3. 了解了ZGC的着色指针技术

## 重点知识
- CMS的3个缺点: CPU敏感、浮动垃圾、内存碎片
- G1通过选择性回收Region实现可预测停顿
- ZGC通过着色指针和读屏障实现超低延迟

## 实战代码
- GCCollectorDemo.java: 测试不同GC收集器
- test-gc-collectors.sh: 对比测试脚本

## 遇到的问题
1. 问题: ...
   解决: ...

## 明日计划
- 进行GC性能压测实战
- 使用JMeter模拟高并发场景
- 分析GC日志,找出最优配置
```

---

## 📖 Day 5 (周五): 垃圾收集器性能对比

### 🎯 学习目标

1. 掌握GC性能测试方法和指标
2. 使用JMeter进行压力测试
3. 分析GC日志,找出性能瓶颈
4. 能够根据业务场景选择最优GC配置

### 📚 核心知识点

#### GC性能评估指标

1. **吞吐量 (Throughput)**
   ```
   吞吐量 = 运行用户代码时间 / (运行用户代码时间 + GC时间)

   例如: 程序运行100分钟,GC耗时1分钟
   吞吐量 = 99 / 100 = 99%
   ```

2. **停顿时间 (Pause Time)**
   - Minor GC平均停顿时间
   - Full GC平均停顿时间
   - 最大停顿时间

3. **GC频率**
   - Minor GC次数/分钟
   - Full GC次数/分钟

4. **堆内存利用率**
   - 平均堆使用率
   - 峰值堆使用率

### 💻 实战代码

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day05/GCPerformanceTest.java`

```java
package com.jd.jvm.day05;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GC性能测试 - 模拟电商订单处理场景
 *
 * 测试场景:
 * 1. 高并发短生命周期对象 (模拟订单接口)
 * 2. 大对象分配 (模拟文件上传)
 * 3. 长生命周期对象 (模拟缓存)
 *
 * 运行不同GC收集器对比性能:
 *
 * Serial GC:
 * -XX:+UseSerialGC -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc-serial.log
 *
 * Parallel GC:
 * -XX:+UseParallelGC -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc-parallel.log
 *
 * CMS:
 * -XX:+UseConcMarkSweepGC -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc-cms.log
 *
 * G1:
 * -XX:+UseG1GC -Xms1g -Xmx1g -XX:MaxGCPauseMillis=200 -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc-g1.log
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class GCPerformanceTest {

    private static final int THREAD_COUNT = 50;  // 并发线程数
    private static final int LOOP_COUNT = 10000;  // 每线程循环次数
    private static final Random RANDOM = new Random();

    // 缓存,模拟长生命周期对象
    private static final List<byte[]> cache = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== GC性能压测 ===");
        System.out.println("模拟京东电商订单处理场景\n");

        // 打印GC收集器信息
        printGCInfo();

        long startTime = System.currentTimeMillis();

        // 场景1: 高并发短生命周期对象
        System.out.println("\n【场景1】高并发订单创建 (短生命周期对象)");
        testShortLiveObjects();

        // 场景2: 大对象分配
        System.out.println("\n【场景2】文件上传处理 (大对象分配)");
        testLargeObjects();

        // 场景3: 长生命周期对象
        System.out.println("\n【场景3】缓存数据加载 (长生命周期对象)");
        testLongLiveObjects();

        long endTime = System.currentTimeMillis();

        // 输出性能报告
        System.out.println("\n" + "=".repeat(60));
        System.out.println("性能测试完成!");
        System.out.printf("总耗时: %d ms%n", (endTime - startTime));

        printDetailedGCStats();

        System.out.println("=".repeat(60));
    }

    /**
     * 场景1: 高并发短生命周期对象
     * 模拟: 订单创建接口,每次创建Order对象后立即使用完毕
     */
    private static void testShortLiveObjects() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        long start = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < LOOP_COUNT; j++) {
                        Order order = new Order(
                            "ORDER_" + RANDOM.nextInt(1000000),
                            RANDOM.nextInt(10000),
                            "用户" + RANDOM.nextInt(100000)
                        );
                        // 模拟业务处理
                        order.process();
                        // 对象立即变为垃圾
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long end = System.currentTimeMillis();
        System.out.printf("✅ 完成! 耗时: %d ms, 创建订单: %d 个%n",
            (end - start), THREAD_COUNT * LOOP_COUNT);
    }

    /**
     * 场景2: 大对象分配
     * 模拟: 用户上传图片/文件
     */
    private static void testLargeObjects() {
        long start = System.currentTimeMillis();

        List<byte[]> temp = new ArrayList<>();
        // 分配100个1MB的大对象
        for (int i = 0; i < 100; i++) {
            byte[] largeObj = new byte[1024 * 1024];  // 1MB
            temp.add(largeObj);

            if (i % 20 == 0) {
                System.out.printf("  已处理 %d 个文件...%n", i);
            }
        }

        long end = System.currentTimeMillis();
        System.out.printf("✅ 完成! 耗时: %d ms, 处理文件: 100 个%n", (end - start));

        // 清空temp,让对象变为垃圾
        temp.clear();
    }

    /**
     * 场景3: 长生命周期对象
     * 模拟: 加载缓存数据,持续存活
     */
    private static void testLongLiveObjects() {
        long start = System.currentTimeMillis();

        // 向缓存中添加100MB数据
        for (int i = 0; i < 100; i++) {
            byte[] cacheData = new byte[1024 * 1024];  // 1MB
            cache.add(cacheData);
        }

        long end = System.currentTimeMillis();
        System.out.printf("✅ 完成! 耗时: %d ms, 缓存大小: %d MB%n",
            (end - start), cache.size());
    }

    /**
     * 打印GC收集器信息
     */
    private static void printGCInfo() {
        System.out.println("📊 当前使用的GC收集器:");
        System.out.println("─".repeat(60));

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("收集器: %s%n", gcBean.getName());
            System.out.printf("  内存池: %s%n", String.join(", ", gcBean.getMemoryPoolNames()));
        }

        System.out.println("─".repeat(60));
    }

    /**
     * 打印详细的GC统计信息
     */
    private static void printDetailedGCStats() {
        System.out.println("\n📊 GC详细统计:");
        System.out.println("─".repeat(60));

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long totalGCCount = 0;
        long totalGCTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();

            totalGCCount += count;
            totalGCTime += time;

            System.out.printf("%s:%n", gcBean.getName());
            System.out.printf("  GC次数: %d%n", count);
            System.out.printf("  GC耗时: %d ms%n", time);
            if (count > 0) {
                System.out.printf("  平均GC时间: %.2f ms%n", (double) time / count);
            }
            System.out.println();
        }

        System.out.printf("总GC次数: %d%n", totalGCCount);
        System.out.printf("总GC耗时: %d ms%n", totalGCTime);

        // 打印内存使用情况
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();

        System.out.println("\n💾 堆内存使用:");
        System.out.printf("  初始: %d MB%n", heapUsage.getInit() / 1024 / 1024);
        System.out.printf("  已用: %d MB%n", heapUsage.getUsed() / 1024 / 1024);
        System.out.printf("  已提交: %d MB%n", heapUsage.getCommitted() / 1024 / 1024);
        System.out.printf("  最大: %d MB%n", heapUsage.getMax() / 1024 / 1024);
    }

    /**
     * 订单类 - 模拟业务对象
     */
    static class Order {
        private String orderId;
        private int amount;
        private String userId;
        private long createTime;
        private List<String> items;

        public Order(String orderId, int amount, String userId) {
            this.orderId = orderId;
            this.amount = amount;
            this.userId = userId;
            this.createTime = System.currentTimeMillis();
            this.items = new ArrayList<>();

            // 添加随机数量的商品
            int itemCount = RANDOM.nextInt(10) + 1;
            for (int i = 0; i < itemCount; i++) {
                items.add("商品_" + RANDOM.nextInt(1000));
            }
        }

        /**
         * 模拟订单处理逻辑
         */
        public void process() {
            // 模拟一些计算
            int total = amount;
            for (String item : items) {
                total += item.hashCode() % 100;
            }
            // 结果不使用,只是为了防止JIT优化掉这段代码
            if (total < 0) {
                System.out.println("不可能执行到这里");
            }
        }
    }
}
```

### 🧪 压测脚本

创建文件: `01-java-basic/jvm-tuning/test-gc-performance.sh`

```bash
#!/bin/bash

# GC性能压测脚本

PROJECT_DIR="/Users/yumeng/IdeaProjects/JD-Interview-Preparation/01-java-basic/jvm-tuning"
cd "$PROJECT_DIR"

# 编译项目
echo "编译项目..."
mvn clean compile

# 创建日志目录
mkdir -p gc-logs

echo ""
echo "=================================="
echo "  GC性能压测对比"
echo "=================================="
echo ""

# 通用参数
COMMON_OPTS="-Xms1g -Xmx1g -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
MAIN_CLASS="com.jd.jvm.day05.GCPerformanceTest"

# 测试1: Serial GC
echo "【测试1/4】Serial GC (单线程收集)"
echo "----------------------------------------"
java -XX:+UseSerialGC $COMMON_OPTS -Xloggc:gc-logs/gc-serial.log \
  -cp target/classes:~/.m2/repository/org/openjdk/jol/jol-core/0.17/jol-core-0.17.jar \
  $MAIN_CLASS

echo -e "\n✅ Serial GC测试完成,日志: gc-logs/gc-serial.log"
echo "按Enter继续..."
read

# 测试2: Parallel GC
echo -e "\n【测试2/4】Parallel GC (并行吞吐量优先)"
echo "----------------------------------------"
java -XX:+UseParallelGC $COMMON_OPTS -Xloggc:gc-logs/gc-parallel.log \
  -cp target/classes:~/.m2/repository/org/openjdk/jol/jol-core/0.17/jol-core-0.17.jar \
  $MAIN_CLASS

echo -e "\n✅ Parallel GC测试完成,日志: gc-logs/gc-parallel.log"
echo "按Enter继续..."
read

# 测试3: CMS
echo -e "\n【测试3/4】CMS GC (并发低停顿)"
echo "----------------------------------------"
java -XX:+UseConcMarkSweepGC $COMMON_OPTS -Xloggc:gc-logs/gc-cms.log \
  -cp target/classes:~/.m2/repository/org/openjdk/jol/jol-core/0.17/jol-core-0.17.jar \
  $MAIN_CLASS

echo -e "\n✅ CMS GC测试完成,日志: gc-logs/gc-cms.log"
echo "按Enter继续..."
read

# 测试4: G1
echo -e "\n【测试4/4】G1 GC (可预测停顿)"
echo "----------------------------------------"
java -XX:+UseG1GC $COMMON_OPTS -XX:MaxGCPauseMillis=200 -Xloggc:gc-logs/gc-g1.log \
  -cp target/classes:~/.m2/repository/org/openjdk/jol/jol-core/0.17/jol-core-0.17.jar \
  $MAIN_CLASS

echo -e "\n✅ G1 GC测试完成,日志: gc-logs/gc-g1.log"

echo ""
echo "=================================="
echo "  测试全部完成!"
echo "=================================="
echo ""
echo "📊 GC日志文件:"
echo "  - gc-logs/gc-serial.log"
echo "  - gc-logs/gc-parallel.log"
echo "  - gc-logs/gc-cms.log"
echo "  - gc-logs/gc-g1.log"
echo ""
echo "📌 下一步:"
echo "  1. 使用GCEasy分析日志: https://gceasy.io/"
echo "  2. 或使用GCViewer查看: java -jar gcviewer.jar"
echo "  3. 填写性能对比表"
```

添加执行权限:
```bash
chmod +x test-gc-performance.sh
```

### 📊 性能对比表模板

运行完4次测试后,填写以下表格:

```markdown
### GC性能对比结果

| 指标 | Serial GC | Parallel GC | CMS | G1 |
|------|-----------|-------------|-----|-----|
| **总耗时(ms)** | ___ | ___ | ___ | ___ |
| **Minor GC次数** | ___ | ___ | ___ | ___ |
| **Full GC次数** | ___ | ___ | ___ | ___ |
| **总GC次数** | ___ | ___ | ___ | ___ |
| **GC总耗时(ms)** | ___ | ___ | ___ | ___ |
| **平均Minor GC时间(ms)** | ___ | ___ | ___ | ___ |
| **平均Full GC时间(ms)** | ___ | ___ | ___ | ___ |
| **最大停顿时间(ms)** | ___ | ___ | ___ | ___ |
| **吞吐量(%)** | ___ | ___ | ___ | ___ |

**吞吐量计算公式**:
```
吞吐量 = (总耗时 - GC总耗时) / 总耗时 × 100%
```

**结论**:
- **吞吐量最高**: ___ (适合批处理任务)
- **停顿时间最短**: ___ (适合在线服务)
- **最稳定**: ___ (GC次数少且均匀)
- **京东推荐**: G1 或 ZGC (根据堆大小)
```

### 📖 深度学习资源

#### 博客文章:

1. ✅ **美团技术团队 - Java中9种常见的CMS GC问题分析与解决**
   - 链接: https://tech.meituan.com/2020/11/12/java-9-cms-gc.html
   - 内容: 生产环境CMS GC问题案例,实用性极强
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **美团技术 - 从实际案例聊聊Java应用的GC优化**
   - 链接: https://tech.meituan.com/2017/12/29/jvm-optimize.html
   - 内容: 完整的GC调优方法论
   - 推荐指数: ⭐⭐⭐⭐⭐

3. ✅ **阿里技术 - 一次生产CPU 100%排查优化实践**
   - 链接: https://developer.aliyun.com/article/780038
   - 内容: 真实案例,GC导致的CPU问题
   - 推荐指数: ⭐⭐⭐⭐⭐

#### 视频教程:

1. ✅ **尚硅谷JVM P31-P35: GC调优实战**
   - 链接: https://www.bilibili.com/video/BV1PJ411n7xZ?p=31
   - 内容:
     - P31: GC日志分析
     - P32-P33: 压测与调优
     - P34-P35: 真实案例分析
   - 推荐指数: ⭐⭐⭐⭐⭐

#### 工具:

1. ✅ **GCEasy - 在线GC日志分析工具**
   - 链接: https://gceasy.io/
   - 功能: 上传GC日志,自动生成可视化报告
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **GCViewer - 本地GC日志查看器**
   - GitHub: https://github.com/chewiebug/GCViewer
   - 功能: 本地查看GC日志,生成图表
   - 推荐指数: ⭐⭐⭐⭐

3. ✅ **Arthas - 阿里开源Java诊断工具**
   - 官网: https://arthas.aliyun.com/
   - 功能: 实时查看JVM状态,不重启即可诊断
   - 推荐指数: ⭐⭐⭐⭐⭐

### 📝 Anki卡片

#### 卡片1: GC性能指标

**正面**:
```
评估GC性能的4个核心指标是什么?如何计算吞吐量?
```

**背面**:
```
4个核心指标:

1. 吞吐量 (Throughput)
   公式: 运行用户代码时间 / (运行用户代码时间 + GC时间)
   目标: 越高越好 (>95%)
   适用: Parallel GC

2. 停顿时间 (Pause Time)
   - Minor GC平均停顿时间
   - Full GC平均停顿时间
   - 最大停顿时间
   目标: 越短越好 (<100ms)
   适用: CMS、G1、ZGC

3. GC频率
   - Minor GC次数/分钟
   - Full GC次数/分钟
   目标: 越少越好 (Full GC应尽量避免)

4. 内存利用率
   - 平均堆使用率
   - 峰值堆使用率
   目标: 保持在70-80%

吞吐量计算示例:
程序运行100秒,GC耗时2秒
吞吐量 = 98 / 100 = 98%
```

#### 卡片2: GC日志分析

**正面**:
```
如何分析GC日志?需要关注哪些关键信息?
```

**背面**:
```
GC日志关键信息:

1. GC类型
   - [GC (Allocation Failure)] - Minor GC
   - [Full GC (Ergonomics)] - Full GC

2. GC前后内存变化
   [GC 1000K->500K(2000K), 0.0015 secs]
   └─ 1000K: GC前内存
   └─ 500K: GC后内存
   └─ 2000K: 堆总大小
   └─ 0.0015 secs: GC耗时

3. 停顿时间
   [Times: user=0.01 sys=0.00, real=0.02 secs]
   └─ user: 用户态CPU时间
   └─ sys: 内核态CPU时间
   └─ real: 实际停顿时间 (重点关注!)

4. 关注指标
   ✅ Full GC频率 (应该很少)
   ✅ 最大停顿时间
   ✅ 堆使用率是否合理
   ✅ 是否有内存泄漏迹象

推荐工具:
- GCEasy (https://gceasy.io/) - 上传日志自动分析
- GCViewer - 本地可视化工具
```

#### 卡片3: 不同场景的GC选择

**正面**:
```
根据不同业务场景,应该如何选择GC收集器?
```

**背面**:
```
场景与GC选择:

1. 后台批处理任务
   - 推荐: Parallel GC
   - 理由: 吞吐量最高,不关心停顿时间
   - 参数: -XX:+UseParallelGC

2. 在线服务 (6GB以下堆)
   - 推荐: CMS
   - 理由: 低停顿,响应快
   - 参数: -XX:+UseConcMarkSweepGC

3. 在线服务 (6GB以上堆)
   - 推荐: G1
   - 理由: 可预测停顿,内存整理
   - 参数: -XX:+UseG1GC -XX:MaxGCPauseMillis=200

4. 超大堆 (100GB+) 或极低延迟要求
   - 推荐: ZGC
   - 理由: <10ms停顿,支持TB级堆
   - 参数: -XX:+UseZGC

5. 微服务 (小堆内存)
   - 推荐: Serial GC 或 G1
   - 理由: 简单高效,资源占用少

京东生产环境推荐:
- 普通服务: G1 (JDK9+默认)
- 核心交易: ZGC (需JDK11+)
```

### ✅ 今日任务清单

```markdown
Day 5 学习任务:
- [ ] 完成GCPerformanceTest.java代码
- [ ] 运行test-gc-performance.sh测试4种GC
- [ ] 使用GCEasy分析4份GC日志
- [ ] 填写性能对比表
- [ ] 阅读美团GC优化博客
- [ ] 阅读阿里CPU 100%排查博客
- [ ] 观看尚硅谷P31-P35 GC调优
- [ ] 制作Anki卡片3张
- [ ] 写今日学习总结
- [ ] LeetCode: 206. 反转链表
```

---

## 📖 Day 6 (周六): 类加载机制

### 🎯 学习目标

1. 掌握类加载的7个阶段
2. 理解双亲委派模型的原理和意义
3. 能够编写自定义类加载器
4. 理解Tomcat的类加载机制

### 📚 核心知识点

#### 1. 类加载过程 (7个阶段)

```
┌─────────────────────────────────────────────────────────┐
│  加载  →  验证  →  准备  →  解析  →  初始化  →  使用  →  卸载  │
│ Loading  Verification Preparation Resolution Initialization Using Unloading
└─────────────────────────────────────────────────────────┘
     ↑                                              ↑
     └──────────── 类加载过程 ──────────────────────┘
```

#### 1. 加载 (Loading)

**做3件事**:
1. 通过类的全限定名获取二进制字节流
2. 将字节流转换为方法区的运行时数据结构
3. 在堆中生成Class对象,作为方法区数据的访问入口

**来源**:
- .class文件
- jar/zip包
- 网络 (Applet)
- 动态生成 (动态代理)
- 数据库
- 加密文件

#### 2. 验证 (Verification)

**目的**: 确保Class文件符合JVM规范,不会危害JVM安全

**4个验证步骤**:
1. **文件格式验证**
   - 是否以魔数 `0xCAFEBABE` 开头
   - 主次版本号是否在JVM处理范围内

2. **元数据验证**
   - 是否有父类 (除Object外)
   - 父类是否允许被继承 (final修饰)

3. **字节码验证**
   - 类型转换是否合法
   - 跳转指令是否正确

4. **符号引用验证**
   - 符号引用的类、字段、方法是否存在
   - 访问性是否合法 (private/public)

#### 3. 准备 (Preparation)

**作用**: 为类变量(static变量)分配内存并设置零值

**示例**:
```java
public class Test {
    public static int value = 123;  // 准备阶段 value = 0
    public static final int FINAL_VALUE = 456;  // 准备阶段 FINAL_VALUE = 456
}
```

**注意**:
- 只为`static`变量分配内存,实例变量在对象实例化时分配
- 设置零值 (int=0, boolean=false, 引用=null)
- `static final`常量在准备阶段就会赋值

#### 4. 解析 (Resolution)

**作用**: 将符号引用替换为直接引用

**符号引用 vs 直接引用**:
- **符号引用**: 用字符串表示,如 `"java/lang/String"`
- **直接引用**: 内存地址,直接指向目标

**解析内容**:
1. 类或接口的解析
2. 字段解析
3. 方法解析
4. 接口方法解析

#### 5. 初始化 (Initialization)

**作用**: 执行类构造器 `<clinit>()` 方法

**`<clinit>()` 方法**:
- 由编译器自动生成
- 收集所有类变量的赋值动作和static{}块
- 按源文件出现顺序执行

**示例**:
```java
public class Test {
    static {
        System.out.println("static块1");
    }

    public static int value = 123;  // 初始化阶段 value = 123

    static {
        System.out.println("static块2");
        value = 456;  // value最终 = 456
    }
}
```

**执行顺序**:
```
1. static块1
2. value = 123
3. static块2
4. value = 456
```

**触发初始化的6种情况**:
1. new、getstatic、putstatic、invokestatic指令
2. 反射调用
3. 初始化子类,先初始化父类
4. main方法所在类
5. MethodHandle解析
6. 接口的默认方法

### 2. 类加载器 (ClassLoader)

#### 三层类加载器

```
┌───────────────────────────────────────┐
│  Bootstrap ClassLoader (启动类加载器)  │
│  加载: <JAVA_HOME>/lib/rt.jar等       │
│  实现: C++,无法被Java代码引用           │
└────────────────┬──────────────────────┘
                 │ 父加载器
┌────────────────▼──────────────────────┐
│  Extension ClassLoader (扩展类加载器)  │
│  加载: <JAVA_HOME>/lib/ext/*.jar      │
│  实现: sun.misc.Launcher$ExtClassLoader│
└────────────────┬──────────────────────┘
                 │ 父加载器
┌────────────────▼──────────────────────┐
│  Application ClassLoader (应用类加载器)│
│  加载: classpath下的类                 │
│  实现: sun.misc.Launcher$AppClassLoader│
└────────────────┬──────────────────────┘
                 │ 父加载器
┌────────────────▼──────────────────────┐
│  Custom ClassLoader (自定义类加载器)   │
│  加载: 用户自定义来源                  │
│  实现: 继承ClassLoader                 │
└───────────────────────────────────────┘
```

### 3. 双亲委派模型 (Parent Delegation Model)

#### 工作流程

```java
// ClassLoader.loadClass()源码简化版
protected Class<?> loadClass(String name, boolean resolve) {
    // 1. 检查是否已加载
    Class<?> c = findLoadedClass(name);

    if (c == null) {
        try {
            if (parent != null) {
                // 2. 委派给父加载器
                c = parent.loadClass(name, false);
            } else {
                // 3. 委派给Bootstrap ClassLoader
                c = findBootstrapClassOrNull(name);
            }
        } catch (ClassNotFoundException e) {
            // 父加载器无法加载
        }

        if (c == null) {
            // 4. 父加载器无法加载,自己加载
            c = findClass(name);
        }
    }

    return c;
}
```

#### 双亲委派的好处

1. **避免类重复加载**
   - 如果父加载器已加载,子加载器不再加载

2. **保证安全性**
   - 防止核心类库被篡改
   - 例如: 自己写`java.lang.String`无法替换JDK的String

**示例**:
```java
// 试图替换JDK的String类
package java.lang;

public class String {
    public String() {
        System.out.println("自定义String");
    }
}

// 运行结果: 报错!
// java.lang.SecurityException: Prohibited package name: java.lang
```

#### 打破双亲委派模型

**场景1: JNDI、JDBC驱动**
- 问题: Bootstrap ClassLoader加载的核心类需要调用用户代码
- 解决: 线程上下文类加载器 (Thread Context ClassLoader)

```java
// JDBC驱动加载
Class.forName("com.mysql.cj.jdbc.Driver");

// DriverManager在rt.jar中,由Bootstrap加载
// Driver实现在classpath中,由Application加载
// 通过Thread.currentThread().getContextClassLoader()打破双亲委派
```

**场景2: Tomcat**
- 需要隔离不同Web应用的类
- 每个Web应用有自己的类加载器

**场景3: OSGI**
- 模块化热部署
- 复杂的类加载器网状结构

### 💻 实战代码

#### Demo1: 类加载过程演示

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day06/ClassLoadingDemo.java`

```java
package com.jd.jvm.day06;

/**
 * 类加载过程演示
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class ClassLoadingDemo {

    public static void main(String[] args) {
        System.out.println("=== 类加载过程演示 ===\n");

        // 触发类加载
        System.out.println("准备使用Parent类...");
        System.out.println("Parent.value = " + Parent.value);

        System.out.println("\n准备使用Child类...");
        System.out.println("Child.childValue = " + Child.childValue);
    }
}

/**
 * 父类
 */
class Parent {
    public static int value = 123;

    static {
        System.out.println("【Parent】static块执行");
        System.out.println("【Parent】value初始化为: " + value);
    }
}

/**
 * 子类
 */
class Child extends Parent {
    public static int childValue = 456;

    static {
        System.out.println("【Child】static块执行");
        System.out.println("【Child】childValue初始化为: " + childValue);
    }
}
```

**运行结果**:
```
=== 类加载过程演示 ===

准备使用Parent类...
【Parent】static块执行
【Parent】value初始化为: 123
Parent.value = 123

准备使用Child类...
【Child】static块执行
【Child】childValue初始化为: 456
Child.childValue = 456
```

**说明**:
- 访问`Parent.value`触发Parent类初始化
- 访问`Child.childValue`触发Child类初始化
- 初始化子类前,先初始化父类

#### Demo2: 类加载器层级演示

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day06/ClassLoaderDemo.java`

```java
package com.jd.jvm.day06;

/**
 * 类加载器层级演示
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class ClassLoaderDemo {

    public static void main(String[] args) {
        System.out.println("=== 类加载器层级演示 ===\n");

        // 当前类的类加载器
        ClassLoader classLoader = ClassLoaderDemo.class.getClassLoader();
        System.out.println("当前类的类加载器: " + classLoader);
        System.out.println("  父加载器: " + classLoader.getParent());
        System.out.println("  祖父加载器: " + classLoader.getParent().getParent());

        System.out.println("\nString类的类加载器: " + String.class.getClassLoader());

        // 打印类加载器层级
        System.out.println("\n=== 类加载器层级关系 ===");
        printClassLoaderHierarchy();
    }

    /**
     * 打印类加载器层级
     */
    private static void printClassLoaderHierarchy() {
        ClassLoader classLoader = ClassLoaderDemo.class.getClassLoader();
        int level = 1;

        while (classLoader != null) {
            System.out.println("  ".repeat(level) + "└─ " + classLoader);
            classLoader = classLoader.getParent();
            level++;
        }

        System.out.println("  ".repeat(level) + "└─ Bootstrap ClassLoader (null)");
    }
}
```

**运行结果**:
```
=== 类加载器层级演示 ===

当前类的类加载器: sun.misc.Launcher$AppClassLoader@18b4aac2
  父加载器: sun.misc.Launcher$ExtClassLoader@1540e19d
  祖父加载器: null

String类的类加载器: null

=== 类加载器层级关系 ===
  └─ sun.misc.Launcher$AppClassLoader@18b4aac2
    └─ sun.misc.Launcher$ExtClassLoader@1540e19d
      └─ Bootstrap ClassLoader (null)
```

#### Demo3: 自定义类加载器

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day06/CustomClassLoaderDemo.java`

```java
package com.jd.jvm.day06;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 自定义类加载器
 *
 * 实现功能:
 * 1. 从指定目录加载.class文件
 * 2. 支持加密的class文件 (简单异或加密)
 * 3. 实现热部署
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class CustomClassLoaderDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 自定义类加载器演示 ===\n");

        // 创建自定义类加载器
        String classPath = "/tmp/custom-classes";
        MyClassLoader myClassLoader = new MyClassLoader(classPath);

        // 使用自定义类加载器加载类
        Class<?> clazz = myClassLoader.loadClass("com.jd.test.HelloWorld");
        System.out.println("加载的类: " + clazz.getName());
        System.out.println("类加载器: " + clazz.getClassLoader());

        // 创建实例并调用方法
        Object instance = clazz.newInstance();
        clazz.getMethod("sayHello").invoke(instance);

        // 演示热部署: 重新加载
        System.out.println("\n=== 演示热部署 ===");
        MyClassLoader newClassLoader = new MyClassLoader(classPath);
        Class<?> newClazz = newClassLoader.loadClass("com.jd.test.HelloWorld");
        System.out.println("重新加载的类: " + newClazz.getName());
        System.out.println("类加载器: " + newClazz.getClassLoader());
        System.out.println("是否同一个Class对象: " + (clazz == newClazz));
    }
}

/**
 * 自定义类加载器
 */
class MyClassLoader extends ClassLoader {

    private String classPath;

    public MyClassLoader(String classPath) {
        this.classPath = classPath;
    }

    public MyClassLoader(String classPath, ClassLoader parent) {
        super(parent);
        this.classPath = classPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // 读取.class文件
            byte[] classData = loadClassData(name);

            if (classData == null) {
                throw new ClassNotFoundException(name);
            }

            // 定义类
            return defineClass(name, classData, 0, classData.length);

        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    /**
     * 加载class文件字节码
     */
    private byte[] loadClassData(String className) throws IOException {
        // 将类名转换为文件路径
        // com.jd.test.HelloWorld -> com/jd/test/HelloWorld.class
        String fileName = className.replace('.', File.separatorChar) + ".class";
        File file = new File(classPath, fileName);

        if (!file.exists()) {
            return null;
        }

        // 读取文件
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }

    @Override
    public String toString() {
        return "MyClassLoader[" + classPath + "]";
    }
}

/**
 * 加密类加载器 (简单异或加密)
 */
class EncryptedClassLoader extends ClassLoader {

    private String classPath;
    private int key;  // 加密密钥

    public EncryptedClassLoader(String classPath, int key) {
        this.classPath = classPath;
        this.key = key;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classData = loadAndDecryptClassData(name);

            if (classData == null) {
                throw new ClassNotFoundException(name);
            }

            return defineClass(name, classData, 0, classData.length);

        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    /**
     * 加载并解密class文件
     */
    private byte[] loadAndDecryptClassData(String className) throws IOException {
        String fileName = className.replace('.', File.separatorChar) + ".class";
        File file = new File(classPath, fileName);

        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // 解密: 异或运算
                for (int i = 0; i < bytesRead; i++) {
                    buffer[i] = (byte) (buffer[i] ^ key);
                }
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }
}
```

#### Demo4: 打破双亲委派模型

创建文件: `01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day06/BreakDelegationDemo.java`

```java
package com.jd.jvm.day06;

/**
 * 打破双亲委派模型演示
 *
 * 场景: 优先使用自定义类加载器加载类
 *
 * @author yumeng
 * @date 2025-11-02
 */
public class BreakDelegationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 打破双亲委派模型演示 ===\n");

        // 创建打破双亲委派的类加载器
        String classPath = "/tmp/custom-classes";
        BreakDelegationClassLoader loader = new BreakDelegationClassLoader(classPath);

        // 加载类
        Class<?> clazz = loader.loadClass("com.jd.test.HelloWorld");
        System.out.println("加载的类: " + clazz.getName());
        System.out.println("类加载器: " + clazz.getClassLoader());
    }
}

/**
 * 打破双亲委派的类加载器
 *
 * 重写loadClass方法,改变委派顺序
 */
class BreakDelegationClassLoader extends ClassLoader {

    private String classPath;

    public BreakDelegationClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 1. 检查是否已加载
            Class<?> c = findLoadedClass(name);

            if (c == null) {
                // 2. 对于自定义的类,自己加载
                if (name.startsWith("com.jd")) {
                    try {
                        c = findClass(name);
                        System.out.println("由自定义类加载器加载: " + name);
                    } catch (ClassNotFoundException e) {
                        // 加载失败,委派给父加载器
                    }
                }

                // 3. 对于JDK类,仍然委派给父加载器
                if (c == null) {
                    c = getParent().loadClass(name);
                    System.out.println("由父加载器加载: " + name);
                }
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 简化实现,实际应该读取.class文件
        throw new ClassNotFoundException(name);
    }
}
```

### 📖 深度学习资源

#### 官方文档:

1. ✅ **JVM Specification - Chapter 5: Loading, Linking, and Initializing**
   - 链接: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-5.html
   - 内容: 类加载官方规范
   - 推荐指数: ⭐⭐⭐⭐⭐

#### 博客文章:

1. ✅ **美团技术团队 - Java类加载机制与自定义类加载器**
   - 链接: https://tech.meituan.com/2022/05/19/class-loading-mechanism.html
   - 内容: 类加载详解,生产实践
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **阿里技术 - 深入理解Tomcat类加载机制**
   - 链接: https://developer.aliyun.com/article/764121
   - 内容: Tomcat如何打破双亲委派
   - 推荐指数: ⭐⭐⭐⭐⭐

3. ✅ **字节跳动技术 - 类加载器的应用与实践**
   - 链接: https://juejin.cn/post/6844903910618980366
   - 内容: 热部署、类隔离实战
   - 推荐指数: ⭐⭐⭐⭐

#### 视频教程:

1. ✅ **尚硅谷JVM P31-P40: 类加载机制**
   - 链接: https://www.bilibili.com/video/BV1PJ411n7xZ?p=36
   - 内容:
     - P36-P37: 类加载过程
     - P38-P39: 双亲委派模型
     - P40: 自定义类加载器
   - 推荐指数: ⭐⭐⭐⭐⭐

#### 源码阅读:

1. ✅ **OpenJDK - ClassLoader源码**
   - 链接: https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/ClassLoader.java
   - 重点方法:
     - `loadClass()`: 双亲委派实现
     - `findClass()`: 子类需要重写
     - `defineClass()`: 将字节码转换为Class对象
   - 推荐指数: ⭐⭐⭐⭐⭐

2. ✅ **Tomcat - WebappClassLoader源码**
   - 链接: https://github.com/apache/tomcat/blob/main/java/org/apache/catalina/loader/WebappClassLoaderBase.java
   - 内容: 如何打破双亲委派
   - 推荐指数: ⭐⭐⭐⭐

### 📝 Anki卡片

#### 卡片1: 类加载7个阶段

**正面**:
```
类加载的7个阶段是什么?哪些阶段可以并行?
```

**背面**:
```
7个阶段:
1. 加载 (Loading)
2. 验证 (Verification)
3. 准备 (Preparation)
4. 解析 (Resolution)
5. 初始化 (Initialization)
6. 使用 (Using)
7. 卸载 (Unloading)

┌───────────────────────────────────────┐
│ 加载 → 验证 → 准备 → 解析 → 初始化     │ 类加载过程
└───────────────────────────────────────┘

可以并行:
- 加载、验证、准备、解析: 可以交叉进行
- 初始化: 必须在验证、准备、解析之后

关键点:
- 准备阶段: static变量分配内存,赋零值
- 初始化阶段: 执行<clinit>(),赋真实值
```

#### 卡片2: 双亲委派模型

**正面**:
```
双亲委派模型的工作流程是什么?为什么需要双亲委派?
```

**背面**:
```
工作流程:
1. 检查类是否已加载
2. 如果未加载,委派给父加载器
3. 如果父加载器无法加载,自己加载

代码逻辑:
protected Class<?> loadClass(String name) {
    // 1. 检查是否已加载
    Class c = findLoadedClass(name);

    if (c == null) {
        // 2. 委派给父加载器
        if (parent != null) {
            c = parent.loadClass(name);
        } else {
            c = findBootstrapClassOrNull(name);
        }

        // 3. 父加载器无法加载,自己加载
        if (c == null) {
            c = findClass(name);
        }
    }

    return c;
}

好处:
1. 避免类重复加载
2. 保证安全性,防止核心类库被篡改

例如: 自己写java.lang.String无法替换JDK的String
```

#### 卡片3: Tomcat类加载机制

**正面**:
```
Tomcat如何打破双亲委派模型?为什么需要打破?
```

**背面**:
```
为什么需要打破:
1. 隔离不同Web应用的类
2. 支持同一个类的不同版本共存
3. 优先加载Web应用自己的类

Tomcat类加载器层级:
┌────────────────────────────────┐
│  Bootstrap ClassLoader         │
└───────────┬────────────────────┘
            │
┌───────────▼────────────────────┐
│  Extension ClassLoader         │
└───────────┬────────────────────┘
            │
┌───────────▼────────────────────┐
│  System ClassLoader            │
└───────────┬────────────────────┘
            │
┌───────────▼────────────────────┐
│  Common ClassLoader            │ Tomcat/lib
└───────┬────────────────────────┘
        │
┌───────▼────────────────────────┐
│  WebApp ClassLoader            │ WEB-INF/classes
│  (每个应用一个)                 │ WEB-INF/lib
└────────────────────────────────┘

打破方式:
1. 重写loadClass()方法
2. 改变委派顺序:
   - 先从WebApp ClassLoader加载
   - 失败后再委派给父加载器
3. 对JDK核心类仍然委派

加载顺序:
1. JVM核心类 (rt.jar) - 委派
2. Web应用自己的类 - 自己加载 ⭐
3. Tomcat公共类 - 委派给Common
4. 系统类 - 委派给System
```

#### 卡片4: 自定义类加载器

**正面**:
```
如何实现自定义类加载器?需要重写哪些方法?
```

**背面**:
```
实现步骤:

1. 继承ClassLoader类
2. 重写findClass()方法 (推荐)
   或 重写loadClass()方法 (打破双亲委派)

示例:
class MyClassLoader extends ClassLoader {

    @Override
    protected Class<?> findClass(String name)
            throws ClassNotFoundException {
        try {
            // 1. 读取.class文件字节码
            byte[] classData = loadClassData(name);

            // 2. 调用defineClass定义类
            return defineClass(name, classData,
                              0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    private byte[] loadClassData(String name)
            throws IOException {
        // 从文件/网络/数据库读取字节码
        // ...
    }
}

关键方法:
- findClass(): 自定义加载逻辑
- defineClass(): 将字节码转换为Class对象
- resolveClass(): 链接类
- loadClass(): 加载入口,包含双亲委派逻辑

应用场景:
1. 加密的class文件
2. 从网络/数据库加载类
3. 热部署
4. 类隔离
```

### ✅ 今日任务清单

```markdown
Day 6 学习任务:
- [ ] 阅读《深入理解Java虚拟机》第7章
- [ ] 观看尚硅谷JVM P36-P40
- [ ] 完成ClassLoadingDemo.java代码
- [ ] 完成ClassLoaderDemo.java代码
- [ ] 完成CustomClassLoaderDemo.java代码
- [ ] 阅读美团类加载博客
- [ ] 阅读阿里Tomcat类加载博客
- [ ] 阅读ClassLoader源码
- [ ] 制作Anki卡片4张
- [ ] 写今日学习总结
- [ ] LeetCode: 141. 环形链表
```

---

## 📖 Day 7 (周日): Week 1 总结与项目

### 🎯 本周学习目标

**上午 (9:00-12:00): 知识复盘**
- 复习本周所有Anki卡片 (预计15-20张)
- 绘制JVM完整知识体系思维导图
- 写周总结博客 (800-1000字)

**下午 (14:00-18:00): 实战项目**
- 完成JVM调优监控系统
- 压测并进行调优
- 对比调优前后性能数据

**晚上 (20:00-22:00): 总结与规划**
- 提交项目到GitHub
- 更新项目README
- 写学习周报
- 预习Week 2内容

### 📊 Week 1 知识体系思维导图

```
                    JVM原理与内存管理
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
    内存结构              垃圾回收            类加载机制
        │                   │                   │
  ┌─────┴─────┐      ┌─────┴─────┐      ┌─────┴─────┐
程序计数器  虚拟机栈  GC算法  GC收集器  类加载过程  双亲委派
本地方法栈    堆              │                   │
    方法区              ┌─────┴─────┐      ┌─────┴─────┐
                     标记清除  标记复制  加载  验证  准备
                     标记整理  分代收集  解析  初始化
                            │
                  ┌─────────┼─────────┐
               Serial  Parallel  CMS
                  G1      ZGC
```

### 🎓 Week 1 学习成果检查清单

```markdown
Week 1 成果检查:
- [ ] 完成Day 1-6全部学习任务
- [ ] 提交至少7次Git commit
- [ ] 制作15+张Anki卡片
- [ ] 完成6个Demo程序
- [ ] 阅读至少5篇技术博客
- [ ] 观看JVM视频P1-P40
- [ ] 完成5道LeetCode链表题
- [ ] 写6篇每日学习总结
- [ ] 完成1个实战项目 (下午进行)
```

### 💻 实战项目: JVM调优监控系统

#### 项目介绍

**项目名称**: JVM Performance Tuning & Monitoring System

**项目目标**:
- 创建一个Spring Boot应用,模拟电商订单处理
- 通过JVM调优,提升系统性能
- 对比调优前后的性能数据

**性能指标**:
- 降低Full GC次数 (目标: 从10+次降到<3次)
- 缩短GC停顿时间 (目标: 从500ms降到<100ms)
- 提升吞吐量 (目标: QPS从500提升到2000+)

#### 项目结构

创建项目: `01-java-basic/jvm-tuning-system/`

```
jvm-tuning-system/
├── pom.xml
├── README.md
├── docs/
│   ├── 调优前性能报告.md
│   ├── 调优后性能报告.md
│   └── 调优过程记录.md
└── src/main/java/com/jd/jvm/tuning/
    ├── JvmTuningApplication.java
    ├── controller/
    │   └── OrderController.java
    ├── service/
    │   └── OrderService.java
    ├── model/
    │   └── Order.java
    └── monitor/
        └── JVMMonitor.java
```

#### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.jd</groupId>
    <artifactId>jvm-tuning-system</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>8</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Actuator (监控) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Micrometer Prometheus (指标采集) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: jvm-tuning-system

# Actuator监控端点配置
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# 日志配置
logging:
  level:
    com.jd.jvm.tuning: INFO
```

### 📝 周总结博客模板

```markdown
# Week 1: JVM原理与内存管理 - 学习总结

## 本周学习概况

- 学习天数: 7天
- 代码行数: 2000+
- Anki卡片: 18张
- LeetCode: 6道
- 技术博客: 8篇
- 视频学习: 40集

## 核心知识点掌握

### 1. JVM内存结构
- 掌握了5大内存区域
- 理解堆和栈的区别
- 能够触发和分析OOM

### 2. 对象内存布局
- 使用JOL工具分析对象
- 理解指针压缩原理
- 掌握对象大小计算

### 3. 垃圾回收算法
- 4种GC算法对比
- 分代收集原理
- GC Roots概念

### 4. 垃圾收集器
- 7种收集器特点
- CMS的4个阶段
- G1的Region概念
- ZGC的着色指针

### 5. GC性能调优
- GC日志分析
- 压测对比实验
- 参数调优实践

### 6. 类加载机制
- 类加载7个阶段
- 双亲委派模型
- 自定义类加载器
- Tomcat类加载

## 实战项目

### JVM调优监控系统
- 技术栈: Spring Boot + Prometheus + Grafana
- 功能: 模拟电商订单处理,进行JVM调优
- 成果: Full GC从15次降到2次,停顿时间从600ms降到80ms

## 遇到的问题与解决

### 问题1: 指针压缩配置不生效
- 原因: IDEA运行配置选择错误
- 解决: 使用顶部工具栏的运行配置下拉菜单

### 问题2: GC日志看不懂
- 解决: 使用GCEasy在线工具分析
- 学习: 掌握了Minor GC和Full GC的区别

## 学习方法总结

### 有效的方法
1. ✅ 理论+实践结合
   - 看书/视频后立即写代码验证

2. ✅ Anki卡片记忆
   - 每天复习,效果显著

3. ✅ 阅读源码
   - 看ClassLoader源码加深理解

### 需要改进
1. ❌ LeetCode刷题不够规律
   - 下周设定固定时间(每晚9点)

2. ❌ 技术博客阅读不够深入
   - 需要做笔记,不能只是看

## 下周计划 (Week 2: 多线程基础)

### 学习主题
- Day 8: 线程基础与生命周期
- Day 9: synchronized关键字
- Day 10: volatile关键字
- Day 11: 线程安全与CAS
- Day 12: Lock接口与AQS
- Day 13: 并发工具类
- Day 14: Week 2总结 + 秒杀系统v0.5

### 学习目标
- 掌握Java多线程核心概念
- 理解synchronized和volatile原理
- 学习AQS同步器
- 完成高并发库存扣减系统

## 给自己的话

第一周完成得不错!从对JVM一知半解到能够分析GC日志、进行调优,进步很大。

下周进入多线程领域,这是面试重点,要更加努力!

加油,向京东高级开发工程师迈进! 💪
```

### ✅ 今日任务清单

```markdown
Day 7 学习任务:

上午:
- [ ] 复习本周Anki卡片 (约18张)
- [ ] 绘制JVM知识体系思维导图
- [ ] 写周总结博客 (800-1000字)

下午:
- [ ] 创建jvm-tuning-system项目
- [ ] 完成项目代码
- [ ] 使用JMeter压测
- [ ] 分析GC日志并调优
- [ ] 记录调优前后性能数据

晚上:
- [ ] 提交项目到GitHub
- [ ] 更新README
- [ ] 写学习周报
- [ ] 预习Week 2内容
- [ ] LeetCode: 142. 环形链表 II
```

### 📚 推荐阅读 (Week 2预习)

1. ✅ **《Java并发编程的艺术》第1-2章**
   - 作者: 方腾飞、魏鹏、程晓明
   - 内容: 并发编程基础

2. ✅ **《深入理解Java虚拟机》第13章: 线程安全与锁优化**
   - 作者: 周志明
   - 内容: synchronized、volatile、锁优化

3. ✅ **尚硅谷JUC并发编程 P1-P10**
   - 链接: https://www.bilibili.com/video/BV1Kw411Z7dF
   - 内容: Java并发基础

---

## 🎉 Week 1 完成!

**恭喜你完成了第一周的学习!**

你已经掌握了:
- ✅ JVM内存结构与管理
- ✅ 垃圾回收算法与收集器
- ✅ GC性能调优实战
- ✅ 类加载机制

**本周数据**:
- 学习天数: 7 / 180
- Anki卡片: 18张
- 代码行数: 2000+
- 项目数: 1个

**下周预告: 多线程与并发编程**
- synchronized原理与锁升级
- volatile与内存可见性
- AQS同步器源码分析
- 高并发库存扣减系统

继续加油! 向京东高级开发工程师的目标前进! 🚀
