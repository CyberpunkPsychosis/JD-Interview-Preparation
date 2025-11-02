/**
 * 文件: GCRootsDemo.java
 */

package com.jd.jvm.day03;

/**
 * GC Roots演示
 *
 * GC Roots包括:
 * 1. 虚拟机栈中引用的对象(局部变量)
 * 2. 方法区中类静态属性引用的对象
 * 3. 方法区中常量引用的对象
 * 4. 本地方法栈中JNI引用的对象
 * 5. JVM内部引用(Class对象、异常对象、系统类加载器)
 * 6. 被同步锁(synchronized)持有的对象
 *
 * @author Your Name
 * @date 2024-11-03
 */
public class GCRootsDemo {

    // GC Root 2: 类静态属性引用
    private static Object staticObj = new Object();

    // GC Root 3: 常量引用
    private static final Object CONSTANT_OBJ = new Object();

    public static void main(String[] args) {
        System.out.println("=== GC Roots演示 ===\n");

        // GC Root 1: 栈中局部变量引用
        Object localVar = new Object();
        System.out.println("1. 栈中局部变量: " + localVar);

        System.out.println("2. 类静态属性: " + staticObj);
        System.out.println("3. 常量引用: " + CONSTANT_OBJ);

        // 演示对象可达性
        testObjectReachability();

        // 演示finalize方法
        testFinalize();
    }

    /**
     * 测试对象可达性
     */
    private static void testObjectReachability() {
        System.out.println("\n--- 对象可达性测试 ---");

        Object obj1 = new Object(); // 可达
        Object obj2 = new Object(); // 可达
        Object obj3 = obj1;         // obj3指向obj1,obj1仍可达

        System.out.println("obj1: " + obj1 + " (可达)");
        System.out.println("obj2: " + obj2 + " (可达)");
        System.out.println("obj3: " + obj3 + " (指向obj1)");

        // 断开引用
        obj1 = null;
        System.out.println("\nobj1 = null后:");
        System.out.println("obj1: null (不可达)");
        System.out.println("obj3: " + obj3 + " (仍可达,因为obj3还引用着)");

        obj3 = null;
        System.out.println("\nobj3 = null后:");
        System.out.println("原obj1对象现在完全不可达,等待GC回收");
    }

    /**
     * 测试finalize方法
     */
    private static void testFinalize() {
        System.out.println("\n--- finalize方法测试 ---");

        FinalizableObject obj = new FinalizableObject("test-object");
        System.out.println("创建对象: " + obj.name);

        obj = null;
        System.out.println("断开引用,等待GC...");

        // 触发GC
        System.gc();

        // 等待finalize执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /**
         * 注意:
         * 1. finalize方法只会被调用一次
         * 2. finalize方法执行时间不确定
         * 3. 不建议使用finalize,应该用try-with-resources
         */
    }
}

/**
 * 可被finalize的对象
 */
class FinalizableObject {
    String name;

    public FinalizableObject(String name) {
        this.name = name;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("对象 " + name + " 被回收了(finalize被调用)");
    }
}