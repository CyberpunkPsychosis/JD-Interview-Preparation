/**
 * 文件: GCAlgorithmDemo.java
 *
 * 4种GC算法演示(概念演示,非实际GC)
 */

package com.jd.jvm.day03;

import java.util.*;

/**
 * GC算法模拟演示
 *
 * @author Your Name
 * @date 2024-11-03
 */
public class GCAlgorithmDemo {

    public static void main(String[] args) {
        System.out.println("=== 4种GC算法对比 ===\n");

        // 1. 标记-清除算法 (Mark-Sweep)
        demonstrateMarkSweep();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 标记-复制算法 (Mark-Copy)
        demonstrateMarkCopy();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 标记-整理算法 (Mark-Compact)
        demonstrateMarkCompact();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 分代收集算法 (Generational Collection)
        demonstrateGenerational();
    }

    /**
     * 1. 标记-清除算法
     *
     * 优点: 实现简单
     * 缺点:
     *   - 效率不高(需要扫描两次)
     *   - 产生内存碎片
     */
    private static void demonstrateMarkSweep() {
        System.out.println("1. 标记-清除算法 (Mark-Sweep)");
        System.out.println("   步骤:");
        System.out.println("   ① 标记: 标记出所有可达对象");
        System.out.println("   ② 清除: 回收所有未标记的对象");
        System.out.println();
        System.out.println("   内存布局:");
        System.out.println("   回收前: [对象A][对象B][对象C][对象D][对象E]");
        System.out.println("   标记后: [对象A*][对象B][对象C*][对象D][对象E*]  (* = 可达)");
        System.out.println("   回收后: [对象A][空闲][对象C][空闲][对象E]");
        System.out.println();
        System.out.println("   ❌ 问题: 产生内存碎片,可能导致大对象无法分配");
    }

    /**
     * 2. 标记-复制算法
     *
     * 优点:
     *   - 无内存碎片
     *   - 效率高(只需复制存活对象)
     * 缺点:
     *   - 浪费50%内存
     *   - 存活对象多时效率低
     */
    private static void demonstrateMarkCopy() {
        System.out.println("2. 标记-复制算法 (Mark-Copy)");
        System.out.println("   步骤:");
        System.out.println("   ① 将内存分为两块(From和To)");
        System.out.println("   ② 标记From区的存活对象");
        System.out.println("   ③ 复制存活对象到To区");
        System.out.println("   ④ 清空From区");
        System.out.println("   ⑤ 交换From和To");
        System.out.println();
        System.out.println("   内存布局:");
        System.out.println("   From区: [对象A][对象B][对象C][对象D][对象E]");
        System.out.println("   To区:   [空闲  ][空闲  ][空闲  ][空闲  ][空闲  ]");
        System.out.println();
        System.out.println("   复制后:");
        System.out.println("   From区: [空闲  ][空闲  ][空闲  ][空闲  ][空闲  ]");
        System.out.println("   To区:   [对象A][对象C][对象E][空闲  ][空闲  ]");
        System.out.println();
        System.out.println("   ✅ 优点: 无碎片,紧凑排列");
        System.out.println("   ❌ 缺点: 浪费50%空间");
        System.out.println();
        System.out.println("   改进方案(HotSpot): Eden(80%) + Survivor0(10%) + Survivor1(10%)");
        System.out.println("   - 每次使用Eden + 1个Survivor(90%)");
        System.out.println("   - Minor GC时复制到另一个Survivor");
        System.out.println("   - 只浪费10%空间");
    }

    /**
     * 3. 标记-整理算法
     *
     * 优点:
     *   - 无内存碎片
     *   - 不浪费空间
     * 缺点:
     *   - 效率比复制算法低(需要移动对象)
     */
    private static void demonstrateMarkCompact() {
        System.out.println("3. 标记-整理算法 (Mark-Compact)");
        System.out.println("   步骤:");
        System.out.println("   ① 标记: 标记出所有可达对象");
        System.out.println("   ② 整理: 将存活对象移到内存一端");
        System.out.println("   ③ 清除: 清理边界外的所有空间");
        System.out.println();
        System.out.println("   内存布局:");
        System.out.println("   回收前: [对象A][对象B][对象C][对象D][对象E]");
        System.out.println("   标记后: [对象A*][对象B][对象C*][对象D][对象E*]");
        System.out.println("   整理后: [对象A][对象C][对象E][空闲  ][空闲  ]");
        System.out.println();
        System.out.println("   ✅ 优点: 无碎片,不浪费空间");
        System.out.println("   ⚠️  注意: 移动对象需要STW(Stop The World)");
    }

    /**
     * 4. 分代收集算法
     *
     * 理论基础: 弱分代假说
     *   - 绝大多数对象都是朝生夕灭
     *   - 熬过越多次GC的对象越难消亡
     */
    private static void demonstrateGenerational() {
        System.out.println("4. 分代收集算法 (Generational Collection)");
        System.out.println();
        System.out.println("   分代结构:");
        System.out.println("   ┌─────────────────────────────────┐");
        System.out.println("   │         Young Generation        │ 新生代(1/3)");
        System.out.println("   ├──────────┬───────────┬──────────┤");
        System.out.println("   │  Eden    │ Survivor0 │Survivor1 │");
        System.out.println("   │  (80%)   │  (10%)    │  (10%)   │");
        System.out.println("   └──────────┴───────────┴──────────┘");
        System.out.println("   ┌─────────────────────────────────┐");
        System.out.println("   │          Old Generation         │ 老年代(2/3)");
        System.out.println("   └─────────────────────────────────┘");
        System.out.println();
        System.out.println("   GC类型:");
        System.out.println("   • Minor GC (Young GC):");
        System.out.println("     - 发生在新生代");
        System.out.println("     - 频率高,速度快");
        System.out.println("     - 使用: 标记-复制算法");
        System.out.println();
        System.out.println("   • Major GC (Old GC):");
        System.out.println("     - 发生在老年代");
        System.out.println("     - 频率低,速度慢(比Minor GC慢10倍)");
        System.out.println("     - 使用: 标记-清除 或 标记-整理");
        System.out.println();
        System.out.println("   • Full GC:");
        System.out.println("     - 清理整个堆 + 方法区");
        System.out.println("     - 最慢,要尽量避免");
        System.out.println();
        System.out.println("   对象晋升规则:");
        System.out.println("   1. 大对象直接进入老年代(-XX:PretenureSizeThreshold)");
        System.out.println("   2. 长期存活对象进入老年代(-XX:MaxTenuringThreshold=15)");
        System.out.println("   3. 动态年龄判定(Survivor空间中相同年龄对象总和>50%)");
        System.out.println("   4. 空间分配担保(老年代最大可用连续空间>新生代所有对象)");
    }
}