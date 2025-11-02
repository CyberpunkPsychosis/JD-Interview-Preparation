/**
 * 文件位置: 01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day03/GCDemo.java
 */

package com.jd.jvm.day03;

import java.util.ArrayList;
import java.util.List;

/**
 * GC演示
 *
 * VM参数: -XX:+PrintGCDetails -Xms20M -Xmx20M -Xmn10M -XX:SurvivorRatio=8
 *
 * @author Your Name
 * @date 2024-11-03
 */
public class GCDemo {

    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        System.out.println("=== GC演示 ===\n");

        // 1. 演示Minor GC
        System.out.println("1. Minor GC演示:");
        testMinorGC();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 演示Full GC
        System.out.println("2. Full GC演示:");
        testFullGC();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 演示大对象直接进入老年代
        System.out.println("3. 大对象直接进入老年代:");
        testBigObjectToOld();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 演示对象晋升到老年代
        System.out.println("4. 对象晋升演示:");
        testObjectPromotion();
    }

    /**
     * 测试Minor GC
     *
     * 原理:
     * - 新生代(10MB) = Eden(8MB) + Survivor0(1MB) + Survivor1(1MB)
     * - Eden满时触发Minor GC
     */
    private static void testMinorGC() {
        byte[] allocation1, allocation2, allocation3, allocation4;

        // Eden已使用: 2MB
        allocation1 = new byte[2 * _1MB];
        System.out.println("allocation1: 2MB");

        // Eden已使用: 4MB
        allocation2 = new byte[2 * _1MB];
        System.out.println("allocation2: 2MB");

        // Eden已使用: 6MB
        allocation3 = new byte[2 * _1MB];
        System.out.println("allocation3: 2MB");

        // Eden已使用: 8MB (即将满)
        // 此时分配4MB,Eden空间不足
        // 触发Minor GC: allocation1、2、3仍存活,但Survivor放不下(1MB)
        // 所以会通过担保机制直接进入老年代
        allocation4 = new byte[4 * _1MB];
        System.out.println("allocation4: 4MB (触发Minor GC)");

        /**
         * 预期GC日志:
         * [GC (Allocation Failure) [PSYoungGen: 6144K->808K(9216K)] 6144K->4904K(19456K), 0.0035639 secs]
         *
         * 解读:
         * - PSYoungGen: 新生代GC
         * - 6144K->808K(9216K): 新生代从6MB降到808KB(总大小9MB)
         * - 6144K->4904K(19456K): 堆从6MB降到4.9MB(总大小19MB)
         * - 4904-808=4096KB: 有4MB对象进入老年代
         */
    }

    /**
     * 测试Full GC
     *
     * 触发条件:
     * 1. 老年代空间不足
     * 2. 永久代空间不足(JDK7及以前)
     * 3. System.gc()
     * 4. CMS GC出现promotion failed、concurrent mode failure
     */
    private static void testFullGC() {
        byte[] allocation1 = new byte[2 * _1MB];
        allocation1 = null; // 断开引用

        // 手动触发Full GC
        System.gc();

        /**
         * 预期GC日志:
         * [Full GC (System.gc()) [PSYoungGen: 1024K->0K(9216K)] [ParOldGen: 4096K->4902K(10240K)] 5120K->4902K(19456K)]
         *
         * 解读:
         * - Full GC: 全堆回收
         * - System.gc(): 由System.gc()触发
         * - ParOldGen: 老年代
         */
    }

    /**
     * 大对象直接进入老年代
     *
     * 参数: -XX:PretenureSizeThreshold=3145728 (3MB)
     * (注意: 只对Serial和ParNew收集器有效)
     */
    private static void testBigObjectToOld() {
        // 分配4MB大对象
        byte[] bigObj = new byte[4 * _1MB];
        System.out.println("分配4MB大对象,直接进入老年代");

        /**
         * 目的:
         * - 避免大对象在Eden和Survivor之间来回复制
         * - 减少内存碎片
         */
    }

    /**
     * 长期存活对象进入老年代
     *
     * 参数:
     * - -XX:MaxTenuringThreshold=15 (默认15)
     * - -XX:+PrintTenuringDistribution (打印年龄分布)
     */
    private static void testObjectPromotion() {
        // 创建一些对象并让它们经历多次GC
        List<byte[]> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            list.add(new byte[_1MB]);
            System.out.println("第" + (i + 1) + "次分配1MB");

            // 每隔3次触发一次GC
            if (i % 3 == 2) {
                System.gc();
                System.out.println("  -> 触发GC");
            }
        }

        /**
         * 观察对象年龄增长:
         * - Desired survivor size xxx bytes, new threshold 15 (max 15)
         * - age 1: xxx bytes, xxx total
         * - age 2: xxx bytes, xxx total
         * ...
         */
    }
}