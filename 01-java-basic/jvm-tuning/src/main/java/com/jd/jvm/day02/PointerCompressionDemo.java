/**
 * 文件: PointerCompressionDemo.java
 *
 * 对比开启/关闭指针压缩的对象大小差异
 */

package com.jd.jvm.day02;

import org.openjdk.jol.info.ClassLayout;

/**
 * 指针压缩实验
 *
 * 运行参数:
 * 1. 开启压缩(默认): 无需参数
 * 2. 关闭压缩: -XX:-UseCompressedOops
 *
 * @author Your Name
 * @date 2024-11-02
 */
public class PointerCompressionDemo {

    public static void main(String[] args) {
        System.out.println("=== 指针压缩实验 ===\n");

        // 分析带引用字段的对象
        Person person = new Person("张三", 25);
        System.out.println(ClassLayout.parseInstance(person).toPrintable());

        /**
         * 对比结果:
         *
         * 1. 开启压缩指针(默认):
         *    - Class Pointer: 4字节
         *    - 引用字段: 4字节
         *    - Person对象: 24字节
         *
         * 2. 关闭压缩指针(-XX:-UseCompressedOops):
         *    - Class Pointer: 8字节
         *    - 引用字段: 8字节
         *    - Person对象: 32字节
         *
         * 结论:
         * - 压缩指针可节省50%的指针空间
         * - 堆内存<32GB时建议开启
         * - 堆内存>32GB时自动关闭
         */
    }
}

class Person {
    private String name;  // 引用: 压缩4字节 vs 未压缩8字节
    private int age;      // 基本类型: 始终4字节

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}