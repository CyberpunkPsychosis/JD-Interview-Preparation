/**
 * 内存开销详细演示
 *
 * VM参数: -Xmx24m -Xms24m -XX:+PrintGCDetails
 *
 * 说明为什么分配10MB就OOM，而最大内存是24MB
 */
package com.jd.jvm.day01;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public class MemoryOverheadDemo {

    public static void main(String[] args) {
        System.out.println("=== 内存开销详细分析 ===\n");

        // 1. 打印初始内存状态
        printMemoryInfo("程序启动时");

        // 2. 计算对象开销
        demonstrateObjectOverhead();

        // 3. 详细的OOM测试
        detailedOOMTest();
    }

    /**
     * 演示对象开销
     */
    private static void demonstrateObjectOverhead() {
        System.out.println("\n=== 对象开销分析 ===");

        // 理论上1MB的byte数组
        int arraySize = 1024 * 1024; // 1MB

        System.out.println("1. byte数组声明大小: " + arraySize + " bytes = 1MB");
        System.out.println("\n2. 实际内存占用包括:");
        System.out.println("   - 对象头(Mark Word + Class Pointer): ~12-16 bytes");
        System.out.println("   - 数组长度字段: 4 bytes");
        System.out.println("   - 实际数据: " + arraySize + " bytes");
        System.out.println("   - 对齐填充: 0-7 bytes (总大小必须是8的倍数)");

        // 估算实际大小
        int objectHeader = 16;
        int arrayLength = 4;
        int actualSize = objectHeader + arrayLength + arraySize;
        int aligned = ((actualSize + 7) / 8) * 8; // 向上对齐到8的倍数

        System.out.println("\n3. 估算实际占用: " + aligned + " bytes");
        System.out.println("   ≈ " + (aligned / 1024.0 / 1024.0) + " MB");
        System.out.println("\n4. 开销比例: " +
            String.format("%.4f%%", (aligned - arraySize) * 100.0 / arraySize));
    }

    /**
     * 详细的OOM测试
     */
    private static void detailedOOMTest() {
        System.out.println("\n=== 详细OOM测试 ===\n");

        Runtime runtime = Runtime.getRuntime();

        // 获取初始内存
        long initialUsed = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("初始已用内存: " + (initialUsed / 1024 / 1024) + "MB");

        List<byte[]> list = new ArrayList<>();
        int count = 0;
        long lastUsed = initialUsed;

        try {
            while (true) {
                // 每次分配1MB
                byte[] bytes = new byte[1024 * 1024];
                list.add(bytes);
                count++;

                // 计算实际增长的内存
                long currentUsed = runtime.totalMemory() - runtime.freeMemory();
                long increment = currentUsed - lastUsed;

                System.out.printf("分配第%2d个1MB数组 -> 实际增长: %.2fMB, 当前已用: %dMB, 剩余: %dMB\n",
                    count,
                    increment / 1024.0 / 1024.0,
                    currentUsed / 1024 / 1024,
                    runtime.freeMemory() / 1024 / 1024);

                lastUsed = currentUsed;

                // 避免无限循环
                if (count >= 30) {
                    System.out.println("\n达到安全限制，停止分配");
                    break;
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("\n❌ 发生OOM异常!");
            System.err.println("成功分配的1MB数组个数: " + count);
            System.err.println("理论分配: " + count + "MB");

            printMemoryInfo("OOM发生时");

            // 分析为什么
            analyzeOOM(count);
        }
    }

    /**
     * 分析OOM原因
     */
    private static void analyzeOOM(int allocatedArrays) {
        System.out.println("\n=== OOM原因分析 ===");

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        System.out.println("\n1. 堆最大内存: " + maxMemory + "MB");
        System.out.println("\n2. 实际只能分配: " + allocatedArrays + "MB的byte数组");

        System.out.println("\n3. 内存去向分析:");
        System.out.println("   ├─ JVM启动占用: ~2-3MB (System类、基础库等)");
        System.out.println("   ├─ 对象头开销: ~" +
            String.format("%.2f", allocatedArrays * 20.0 / 1024.0) + "MB");
        System.out.println("   │  (每个数组~20字节对象头 × " + allocatedArrays + "个)");
        System.out.println("   ├─ ArrayList开销: 包括内部数组和引用");
        System.out.println("   ├─ GC元数据: 垃圾回收器的管理结构");
        System.out.println("   └─ 可用数据空间: ~" + allocatedArrays + "MB");

        System.out.println("\n4. 计算:");
        double overhead = maxMemory - allocatedArrays;
        System.out.println("   总开销 ≈ " + String.format("%.1f", overhead) + "MB");
        System.out.println("   开销占比 = " +
            String.format("%.1f%%", overhead * 100.0 / maxMemory));

        System.out.println("\n5. 结论:");
        System.out.println("   虽然-Xmx设置了" + maxMemory + "MB，但由于:");
        System.out.println("   • 每个对象都有对象头开销");
        System.out.println("   • ArrayList等容器也需要内存");
        System.out.println("   • JVM内部数据结构占用");
        System.out.println("   • GC需要预留一些空间");
        System.out.println("   因此实际可用于存储数据的空间 < " + maxMemory + "MB");
    }

    /**
     * 打印内存信息
     */
    private static void printMemoryInfo(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        System.out.println("\n【" + stage + "的内存状态】");
        System.out.println("最大内存(Xmx): " + maxMemory + "MB");
        System.out.println("当前总内存:    " + totalMemory + "MB");
        System.out.println("空闲内存:      " + freeMemory + "MB");
        System.out.println("已用内存:      " + usedMemory + "MB");
        System.out.println("可用内存:      " + (maxMemory - usedMemory) + "MB");
    }
}
