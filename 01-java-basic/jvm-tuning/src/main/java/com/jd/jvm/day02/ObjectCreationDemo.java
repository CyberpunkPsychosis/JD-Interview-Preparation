/**
 * 文件位置: 01-java-basic/jvm-tuning/src/main/java/com/jd/jvm/day02/ObjectCreationDemo.java
 *
 * 前置准备:
 * 1. 在pom.xml中添加JOL依赖
 * 2. 编写以下代码
 * 3. 运行并分析结果
 */

package com.jd.jvm.day02;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

/**
 * 对象创建与内存布局分析
 *
 * 使用JOL(Java Object Layout)工具分析对象内存布局
 *
 * @author Your Name
 * @date 2024-11-02
 */
public class ObjectCreationDemo {

    public static void main(String[] args) {
        System.out.println("=== JVM信息 ===");
        System.out.println(VM.current().details());
        System.out.println();

        // 1. 分析Object对象
        analyzeObject();

        // 2. 分析Integer对象
        analyzeInteger();

        // 3. 分析User对象
        analyzeUser();

        // 4. 分析数组对象
        analyzeArray();

        // 5. 对象大小计算
        calculateObjectSize();
    }

    /**
     * 分析Object对象
     * 最小对象: 只有对象头
     */
    private static void analyzeObject() {
        System.out.println("=== 1. Object对象内存布局 ===");
        Object obj = new Object();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        /**
         * 预期输出(64位JVM,开启指针压缩):
         *
         * java.lang.Object object internals:
         * OFFSET  SIZE   TYPE DESCRIPTION               VALUE
         *      0     4        (object header)           01 00 00 00  # Mark Word(前4字节)
         *      4     4        (object header)           00 00 00 00  # Mark Word(后4字节)
         *      8     4        (object header)           e5 01 00 f8  # Class Pointer(压缩后4字节)
         *     12     4        (loss due to alignment)                # 对齐填充
         * Instance size: 16 bytes
         *
         * 结论: Object对象占16字节
         * - 对象头: 12字节(Mark Word 8字节 + Class Pointer 4字节)
         * - 对齐填充: 4字节(补齐到8的倍数)
         */
    }

    /**
     * 分析Integer对象
     */
    private static void analyzeInteger() {
        System.out.println("=== 2. Integer对象内存布局 ===");
        Integer num = 123;
        System.out.println(ClassLayout.parseInstance(num).toPrintable());

        /**
         * 预期输出:
         * java.lang.Integer object internals:
         * OFFSET  SIZE   TYPE DESCRIPTION               VALUE
         *      0     4        (object header)           01 00 00 00
         *      4     4        (object header)           00 00 00 00
         *      8     4        (object header)           e5 01 00 f8
         *     12     4    int Integer.value             123      # int字段4字节
         * Instance size: 16 bytes
         *
         * 结论: Integer对象占16字节
         * - 对象头: 12字节
         * - int value: 4字节
         * - 对齐填充: 0字节(已对齐)
         */
    }

    /**
     * 分析自定义User对象
     */
    private static void analyzeUser() {
        System.out.println("=== 3. User对象内存布局 ===");
        User user = new User("张三", 25, true);
        System.out.println(ClassLayout.parseInstance(user).toPrintable());

        /**
         * 预期输出:
         * com.jd.jvm.day02.User object internals:
         * OFFSET  SIZE      TYPE DESCRIPTION               VALUE
         *      0     4           (object header)           01 00 00 00
         *      4     4           (object header)           00 00 00 00
         *      8     4           (object header)           43 c1 00 f8
         *     12     4       int User.age                  25         # int 4字节
         *     16     1   boolean User.active               true       # boolean 1字节
         *     17     3           (alignment/padding gap)              # 间隙3字节
         *     20     4    String User.name                 (object)   # 引用4字节(压缩)
         * Instance size: 24 bytes
         *
         * 结论: User对象占24字节
         * - 对象头: 12字节
         * - int age: 4字节
         * - boolean active: 1字节
         * - String name引用: 4字节
         * - 对齐填充: 3字节
         *
         * 注意: String对象本身另外占用内存,这里只是引用
         */
    }

    /**
     * 分析数组对象
     */
    private static void analyzeArray() {
        System.out.println("=== 4. 数组对象内存布局 ===");
        int[] arr = new int[5];
        System.out.println(ClassLayout.parseInstance(arr).toPrintable());

        /**
         * 预期输出:
         * [I object internals:
         * OFFSET  SIZE   TYPE DESCRIPTION               VALUE
         *      0     4        (object header)           01 00 00 00
         *      4     4        (object header)           00 00 00 00
         *      8     4        (object header)           6d 01 00 f8
         *     12     4        (object header)           05 00 00 00  # 数组长度
         *     16    20    int [I.<elements>             N/A          # 5个int,每个4字节
         * Instance size: 36 bytes
         *
         * 结论: int[5]数组占36字节
         * - 对象头: 16字节(多了4字节存数组长度)
         * - 数组数据: 20字节(5个int)
         * - 对齐填充: 0字节
         */
    }

    /**
     * 对象大小计算练习
     */
    private static void calculateObjectSize() {
        System.out.println("=== 5. 对象大小计算练习 ===\n");

        // 练习1: 计算Product对象大小
        Product product = new Product("iPhone", 5999.0, 100);
        System.out.println("Product对象:");
        System.out.println(ClassLayout.parseInstance(product).toPrintable());

        /**
         * 分析过程:
         * 1. 对象头: 12字节
         * 2. 字段:
         *    - String name (引用): 4字节
         *    - double price: 8字节
         *    - int stock: 4字节
         * 3. 总计: 12 + 4 + 8 + 4 = 28字节
         * 4. 对齐: 补齐到32字节
         */

        // 练习2: 空对象
        EmptyObject empty = new EmptyObject();
        System.out.println("EmptyObject对象:");
        System.out.println(ClassLayout.parseInstance(empty).toPrintable());

        // 练习3: 继承关系
        Student student = new Student("李四", 20, true, "S001", 90.5);
        System.out.println("Student对象(继承User):");
        System.out.println(ClassLayout.parseInstance(student).toPrintable());
    }
}

/**
 * 用户类
 */
class User {
    private String name;     // 引用类型: 4字节(压缩指针)
    private int age;         // int: 4字节
    private boolean active;  // boolean: 1字节

    public User(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
}

/**
 * 商品类
 */
class Product {
    private String name;     // 4字节
    private double price;    // 8字节
    private int stock;       // 4字节

    public Product(String name, double price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}

/**
 * 空对象
 */
class EmptyObject {
    // 没有字段,只有对象头
}

/**
 * 学生类(继承User)
 */
class Student extends User {
    private String studentId;  // 4字节
    private double score;      // 8字节

    public Student(String name, int age, boolean active,
                   String studentId, double score) {
        super(name, age, active);
        this.studentId = studentId;
        this.score = score;
    }
}