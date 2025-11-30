# JDK17 新特性

## [恢复始终严格模式（Always-Strict）的浮点语义（JEP306）](https://openjdk.org/jeps/306)

### 背景
在老版本的JDK时，JVM 为提高运算效率，允许在部分情况下使用更高精度的浮点寄存器（尤其在 x87 FP 上），导致可能不严格遵守IEEE 754标准，即
某些 CPU 浮点寄存器精度更高，导致结果与标准不同。这违反了JAVA的跨平台一致性。

而`strictfp`关键字的作用就是使得JVM强制遵守IEEE 754的标准，达到跨平台的一致性。
即在`non-strict`模式下浮点运算可能仍保留扩展精度 (80-bit)，但是`strictfp`会在每一步舍入到 (64-bit)

对比代码如[JEP306](../JDK11/src/main/java/com/misitetong/jdk17/JEP306.java)所示，但是需要在老的机器上运行才能看出差异。

因此现在取消`strictfp`关键字是因为现代JVM/CPU已普遍支持IEEE 754指令集（如 SSE2、AVX），无需依赖高精度寄存器优化。

## [强化伪随机数生成器（JEP356）](https://openjdk.org/jeps/356)

### 存在的问题
1. 算法有限： 
   - `java.util.Random` 使用线性同余生成器（LCG），周期有限，统计质量一般。
   - `ThreadLocalRandom` 在多线程下性能更好，但算法仍比较简单。
2. 可组合性差：
   如果想组合不同随机数生成策略，现有 API 不够灵活。
3. 缺乏现代算法支持：
   随机数生成领域已经有很多高质量算法（如 LXM、Xoshiro256 等），Java 旧 API 没法直接使用。

### 新的引入

1. `java.util.random.RandomGenerator`定义了标准的随机数生成行为，所有新的生成器都实现它。

2. `java.util.random.RandomGeneratorFactory`可以通过工厂获取所有的随机数生成器
   ```java
   RandomGenerator rng = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create();
   ```
3. 随机数生成支持流式处理
   ```java
   RandomGenerator rng3 = RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create();
   System.out.println("\nXoroshiro128PlusPlus: ");
   rng3.ints().limit(5).forEach(System.out::println);
   ```
4. 生成器拆分（Generator Splitting）：是指将一个随机数生成器（RNG）分成多个子生成器，以便在多个计算任务或线程中独立地生成随机数，而不会干扰彼此。拆分的目的是提高并行计算的效率和避免重复或冲突的随机数生成。
   优点：
      - 避免共享随机源： 如果多个线程或任务共享一个随机数生成器，可能会导致生成相同或相关的随机数。拆分生成器后，每个线程拥有自己独立的生成器，从而避免了冲突。
      - 提高并行性能： 拆分生成器可以减少锁的使用。传统的做法是将随机数生成器封装在一个线程安全的对象中，而这通常需要加锁，影响性能。拆分生成器无需加锁，可以显著提高并行计算的效率。
      - 保证随机性： 拆分的子生成器依然能够保持一定的随机性分布，因为它们继承了父生成器的状态，并且不会相互干扰。这样确保了并行计算的结果仍然具有统计学上的随机性。

## [新的macos渲染管线（JEP382）](https://openjdk.org/jeps/382)

### 背景
在macOS上，Java GUI应用程序的图形渲染原本依赖于OpenGL。
macOS自大约2018年开始逐渐淘汰OpenGL，转而推行Metal图形框架。
在JDK17中，支持使用Metal进行渲染，这一变化主要涉及JavaFX 和 Swing等图形界面的渲染过程，
改进了Java在macOS上的用户界面响应速度和渲染质量。

## [原生支持arm版本的macos（JEP391）](https://openjdk.org/jeps/391)

### 背景
Apple 在 2020 年推出了基于 ARM 架构的 Apple Silicon 处理器（例如 M1 和 M1 Pro/Max），并逐步淘汰了 Intel x86 架构的处理器。由于 ARM 架构与传统的 x86 架构在硬件设计和指令集上的差异，Java 需要对其进行适配，以确保其能够在这些新的设备上顺利运行。
此前，JVM 主要支持基于 x86 架构的 macOS 系统，而对 macOS/AArch64 的支持较为有限。这导致在 Apple Silicon 设备上运行 Java 时，需要通过 Rosetta 2 等兼容层进行转换和模拟，虽然能够运行，但效率较低且存在性能损失。

## [废弃Applet的API（JEP398）](https://openjdk.org/jeps/398)
### 背景
Applet 是一种可以在网页浏览器中运行的小型 Java 程序，曾经广泛用于 web 应用程序的图形用户界面和交互功能。但随着技术的发展，Applet 的使用逐渐减少，许多现代浏览器已不再支持它，因此 JEP 398 提出了将 Applet API 废弃的计划。

### 废弃的接口
```java
java.beans.Beans
javax.swing.RepaintManager
javax.naming.Context
java.applet.Applet
java.applet.AppletStub
java.applet.AppletContext
java.applet.AudioClip
javax.swing.JApplet
java.beans.AppletInitializer
```
## [加强JDK内部API的封装（JEP403）](https://openjdk.org/jeps/403)
### 背景
1. 自 Java 9 引入模块系统（JPMS）以来，JDK 开始对内部 API 实施强封装（strong encapsulation）。
2. 在 JDK 9 到 JDK 15 中，默认采用宽松的强封装（relaxed strong encapsulation），允许通过反射访问 JDK 8 时代就存在的内部 API（例如 sun.*、com.sun.* 等包中的非 public 成员）。
3. JDK 16（JEP 396）默认启用强封装，但仍可通过 --illegal-access 参数手动切回宽松模式。
4. JEP 403 彻底废除了 --illegal-access 参数的功能，意味着不再允许用户绕过强封装。
5. 不会移除或封装那些尚无标准替代方案的“关键内部 API”，比如 sun.misc.Unsafe（尽管不鼓励使用，但暂时仍可访问）。

### 影响的代码
下面的代码不可访问时会抛出`InaccessibleObjectException`
```java
// 1. 通过反射访问 sun.misc.Unsafe（某些框架会这样做）
Field f = Unsafe.class.getDeclaredField("theUnsafe");
f.setAccessible(true); // JDK 17+ 可能失败（除非模块开放）

// 2. 使用内部 XML 处理器（JDK 内嵌 Xerces）
org.apache.xerces... // 如果依赖 JDK 内部 Xerces，会失败

// 3. 使用内部 ASM 字节码库
org.objectweb.asm... // JDK 内部 ASM 不再可访问

// 4. 操作时区内部类
sun.util.calendar.ZoneInfo // 应改用 java.time.ZoneId
```

### 检测代码依赖
可以通过`jdeps`工具来检测代码是否依赖内部的API
```shell
jdeps --jdk-internals app.jar
```
## [为switch支持模式匹配（JEP406）](https://openjdk.org/jeps/406)

### 背景
Java 的 switch 只能用于精准匹配：
1. 基本类型int, char, byte, short
2. 包装类 String, enum

这项特性后来在 JDK 18（JEP 420）和 JDK 19（JEP 427）中经历了第二、第三次预览，并最终在 JDK 21 中正式发布（作为标准特性）

[JEP 420: Pattern Matching for switch (Second Preview)](https://openjdk.org/jeps/420)

[JEP 427: Pattern Matching for switch (Third Preview)](https://openjdk.org/jeps/427)

### 新特性
下面所有的特性代码示例详见[JEP406](./src/main/java/com/misitetong/jdk17/JEP406.java)
1. 模式用作case标签
2. 支持 null 作为 case 标签， 甚至可以合并
   ```java
    case null, String s -> System.out.println("String or null: " + s);
   ```
3. 守卫模式：用于在匹配类型后，进一步加条件过滤
4. 支持括号用于解决语法歧义
5. 增强的完整性检查，尤其结合 密封类（Sealed Classes, JEP 409）
6. 模式变量的作用域：模式的变量只在匹配成功后的作用域内有效
