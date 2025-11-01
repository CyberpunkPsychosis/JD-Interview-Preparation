/**
 * 项目位置: 01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day01
 * 文件名: MemoryStructureDemo.java
 *
 * Day1 实战任务:
 * 1. 创建Maven项目 jvm-tuning
 * 2. 编写以下代码
 * 3. 运行并观察结果
 */

package com.jd.jvm.day01;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM内存结构演示
 *
 * VM参数: -Xmx20m -Xms20m -XX:+PrintGCDetails
 *
 * @author Your Name
 * @date 2024-11-01
 */
public class MemoryStructureDemo {

    // 方法区: 类信息、常量池、静态变量
    private static final String CONSTANT = "京东JD";  // 常量
    private static int staticVar = 100;               // 静态变量

    public static void main(String[] args) {
        System.out.println("=== JVM内存结构演示 ===\n");

        // 1. 栈内存演示
        testStack();

        // 2. 堆内存演示
        testHeap();

        // 3. 方法区演示
        testMethodArea();

        // 4. 堆内存溢出演示 (注释掉,需要时再打开)
         testHeapOOM();
    }

    /**
     * 栈内存演示
     * 栈: 存储局部变量、方法调用
     */
    private static void testStack() {
        System.out.println("1. 栈内存演示:");

        // 局部变量存储在栈中
        int localVar = 1;
        String str = "Hello";

        System.out.println("  局部变量 localVar = " + localVar);
        System.out.println("  局部变量 str = " + str);
        System.out.println("  栈特点: 方法执行完自动释放\n");
    }

    /**
     * 堆内存演示
     * 堆: 存储对象实例
     */
    private static void testHeap() {
        System.out.println("2. 堆内存演示:");

        // 对象实例存储在堆中
        User user = new User("张三", 25);
        Product product = new Product("iPhone 15", 5999.0);

        System.out.println("  对象: " + user);
        System.out.println("  对象: " + product);
        System.out.println("  堆特点: 需要GC回收\n");
    }

    /**
     * 方法区演示
     * 方法区: 存储类信息、常量、静态变量
     */
    private static void testMethodArea() {
        System.out.println("3. 方法区演示:");

        System.out.println("  常量: " + CONSTANT);
        System.out.println("  静态变量: " + staticVar);
        System.out.println("  类信息: " + User.class.getName());
        System.out.println("  方法区特点: 类加载时创建\n");
    }

    /**
     * 堆内存溢出演示
     * 预期异常: java.lang.OutOfMemoryError: Java heap space
     *
     * 运行前确保VM参数: -Xmx20m -Xms20m -XX:+PrintGCDetails
     */
    private static void testHeapOOM() {
        System.out.println("4. 堆内存溢出演示:");

        List<byte[]> list = new ArrayList<>();
        int count = 0;

        try {
            while (true) {
                // 每次分配1MB
                list.add(new byte[1024 * 1024]);
                count++;
                System.out.println("  已分配: " + count + "MB");
            }
        } catch (OutOfMemoryError e) {
            System.err.println("\n  ❌ 堆内存溢出!");
            System.err.println("  已分配: " + count + "MB");
            System.err.println("  异常信息: " + e.getMessage());

            // 打印内存信息
            printMemoryInfo();
        }
    }

    /**
     * 打印内存信息
     */
    private static void printMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        System.out.println("\n=== 内存信息 ===");
        System.out.println("最大内存: " + maxMemory + "MB");
        System.out.println("总内存: " + totalMemory + "MB");
        System.out.println("空闲内存: " + freeMemory + "MB");
        System.out.println("已用内存: " + usedMemory + "MB");
    }
}

/**
 * 用户类
 */
class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', age=" + age + "}";
    }
}

/**
 * 商品类
 */
class Product {
    private String name;
    private double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public String toString() {
        return "Product{name='" + name + "', price=" + price + "}";
    }
}
