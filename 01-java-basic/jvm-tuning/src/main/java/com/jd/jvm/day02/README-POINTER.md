# 如何在IDEA中运行指针压缩对比实验

## 你遇到的问题

> "我在IDEA配置了关闭指针压缩的VM参数，但是没有生效"

## 问题根源

你可能直接点击了文件旁边的**绿色箭头** ▶️，这样会使用IDEA的默认配置，不会应用你设置的VM参数！

---

## 正确的操作方法（超详细图文说明）

### 视觉定位指南

```
IDEA界面布局：
┌─────────────────────────────────────────────────────────────┐
│ File Edit View ... Help                                     │ ← 菜单栏
├─────────────────────────────────────────────────────────────┤
│ 🔍 [搜索框]  ┃  [运行配置下拉▼]  ▶️ 🐛 ⏸                    │ ← 工具栏（重点！）
│              ┃                                              │
│              ┃  ← 这里！选择运行配置！                       │
├─────────────────────────────────────────────────────────────┤
│  代码编辑区域                                                │
│                                                             │
│   20  public class PointerCompressionDemo {                 │
│   21      ▶️ ← 不要点这里！这是默认配置！                    │
│   22      public static void main(String[] args) {          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 第1步：找到顶部工具栏的运行配置下拉菜单

**位置**：搜索框右边，有一个下拉箭头▼的地方

**当前显示**：可能显示其他配置名称或"Add Configuration..."

### 第2步：点击下拉菜单，选择配置

你会看到这些选项：
```
┌────────────────────────────────┐
│ MemoryStructureDemo           │
│ MemoryOverheadDemo            │
│ ObjectCreationDemo            │
│ PointerCompressionDemo (ON)   │ ← 开启压缩
│ PointerCompressionDemo (OFF)  │ ← 关闭压缩（选这个！）
│ ─────────────────────────────│
│ Edit Configurations...        │
└────────────────────────────────┘
```

### 第3步：选择 `PointerCompressionDemo (OFF)`

点击后，下拉菜单会显示你选择的配置名称。

### 第4步：点击绿色运行按钮 ▶️

**位置**：在下拉菜单右边的绿色三角形。

**不是文件左侧的绿色箭头！**

---

## 两种配置的区别

### 配置1：PointerCompressionDemo (ON)
```
VM参数: -XX:+UseCompressedOops
        开启指针压缩（默认）
```

**运行结果**：
- Class Pointer: 4字节
- String引用: 4字节
- Person对象: **24字节**

### 配置2：PointerCompressionDemo (OFF)
```
VM参数: -XX:-UseCompressedOops
        关闭指针压缩
```

**运行结果**：
- Class Pointer: 8字节
- String引用: 8字节
- Person对象: **32字节**

---

## 对比实验完整流程

### 实验步骤：

**1️⃣ 第一次运行（开启压缩）**
```
顶部工具栏下拉菜单 → 选择 "PointerCompressionDemo (ON)" → 点击▶️
```

**观察输出**：
```
Instance size: 24 bytes
String 引用: 4字节
```

**2️⃣ 第二次运行（关闭压缩）**
```
顶部工具栏下拉菜单 → 选择 "PointerCompressionDemo (OFF)" → 点击▶️
```

**观察输出**：
```
Instance size: 32 bytes
String 引用: 8字节
```

**3️⃣ 对比结果**

| 对比项 | 开启压缩 | 关闭压缩 | 差异 |
|-------|---------|---------|-----|
| Class Pointer | 4字节 | 8字节 | +100% |
| String引用 | 4字节 | 8字节 | +100% |
| 对象大小 | 24字节 | 32字节 | +33% |

**结论**：指针压缩可以节省大量内存空间！

---

## 常见错误和解决方法

### ❌ 错误1：点击了文件左侧的绿色箭头

**症状**：
- 每次运行都是24字节
- VM参数没有生效

**原因**：
使用了IDEA自动创建的临时配置，没有VM参数。

**解决**：
必须使用**顶部工具栏**的运行配置下拉菜单！

---

### ❌ 错误2：配置了但选错了配置

**症状**：
- 明明想测试关闭压缩
- 结果还是24字节

**原因**：
选择了 `PointerCompressionDemo (ON)` 而不是 `(OFF)`

**解决**：
仔细看配置名称，选择 `(OFF)` 结尾的那个！

---

### ❌ 错误3：看不到运行配置

**症状**：
下拉菜单里没有 `PointerCompressionDemo (ON/OFF)`

**原因**：
IDEA没有加载配置文件

**解决**：
1. File → Invalidate Caches... → Invalidate and Restart
2. 或重新打开项目

---

## 如果还是不行，手动创建配置

### 手动创建步骤：

1. **Run → Edit Configurations...**

2. **点击左上角 + 号**

3. **选择 Application**

4. **填写配置**：
   ```
   Name: PointerCompressionDemo-关闭压缩
   Main class: com.jd.jvm.day02.PointerCompressionDemo
   VM options: -XX:-UseCompressedOops
   Module: jvm-tuning
   ```

5. **Apply → OK**

6. **重新选择这个配置并运行**

---

## 验证是否成功

### 成功的标志：

**关闭压缩配置运行后，看到：**
```
com.jd.jvm.day02.Person object internals:
OFF  SZ               TYPE DESCRIPTION               VALUE
  0   8                    (object header: mark)     0x0000000000000001
  8   8                    (object header: class)    0x0000000014c66038  ← 8字节
 16   4                int Person.age                25
 20   4                    (alignment/padding gap)
 24   8   java.lang.String Person.name               (object)  ← 8字节
Instance size: 32 bytes  ← 32字节表示压缩已关闭！
```

**关键信息**：
- ✅ Class pointer: 8字节（不是4）
- ✅ String引用: 8字节（不是4）
- ✅ 总大小: 32字节（不是24）

---

## 终极测试方法（命令行验证）

如果在IDEA中还是有问题，用命令行验证：

```bash
cd /Users/yumeng/IdeaProjects/JD-Interview-Preparation/01-java-basic/jvm-tuning

# 关闭压缩运行
java -XX:-UseCompressedOops -cp "target/classes:~/.m2/repository/org/openjdk/jol/jol-core/0.17/jol-core-0.17.jar" com.jd.jvm.day02.PointerCompressionDemo
```

如果命令行能看到32字节，说明：
- ✅ 代码没问题
- ✅ VM参数正确
- ❌ IDEA配置有问题

---

## 总结

**核心要点**：
1. ⭐ 必须使用**顶部工具栏**的运行配置下拉菜单
2. ⭐ 不要点击文件左侧的绿色箭头
3. ⭐ 选择 `PointerCompressionDemo (OFF)` 配置
4. ⭐ 观察输出中的对象大小：32字节=成功

**配置位置**：
- 配置文件：`.idea/runConfigurations/PointerCompressionDemo__OFF_.xml`
- VM参数：`-XX:-UseCompressedOops`（注意是减号-）

现在去试试吧！记住要用顶部工具栏的下拉菜单！🚀
