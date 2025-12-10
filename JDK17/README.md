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

## [移除RMI Activation（JEP407）](https://openjdk.org/jeps/407)

### 背景
RMI：是 Java 早期提供的分布式对象通信机制，允许一个 JVM 调用另一个 JVM 中的对象方法。
RMI Activation 自 Java 8 起就被标记为 deprecated（JEP 398 在 JDK 15 中将其标记为 "forRemoval = true"）
1. Unicast（单播）：远程对象常驻内存，客户端直接连接（主流用法）✅ 保留
2. Activation（激活）：远程对象按需启动（lazy activation），即客户端首次调用时才在服务端启动该对象 ❌ 被移除

移除的原因：
1. 现代分布式系统普遍采用： HTTP/REST（如 Spring Boot + Feign） 、gRPC 、WebSocket 、消息队列（Kafka、RabbitMQ） 、RMI Activation 的“按需激活”模型在云原生、容器化时代不再适用
2. Activation 依赖 rmid 守护进程，需额外端口和权限，存在安全隐患（如反序列化攻击） ，代码复杂，维护成本远高于其价值
### 移除项
```java
java.rmi.activation
```

## [密封类Sealed Classes（JEP409）](https://openjdk.org/jeps/409)

### 背景
发布情况：
1. 第一次预览：[JEP 360: Sealed Classes (Preview)](https://openjdk.org/jeps/360)
2. 第二次预览：[JEP 397: Sealed Classes (Second Preview)](https://openjdk.org/jeps/397)
3. 正式发布：JEP 409: Sealed Classes

为什么需要密封类？
1. 传统 final 或包私有（package-private）的不足：
final：完全禁止继承，太严格。
包私有：只能限制包外继承，包内仍可任意扩展
2. 密封类的优势：
精确控制继承：只允许特定子类。
类型系统更安全：编译器知道所有可能子类型。
支持代数数据类型（ADT）：类似 Scala 的 sealed trait 或 Kotlin 的 sealed class。
为模式匹配提供基础：是 JEP 406/420/427 完整性检查的前提。
### 定义与语法

示例代码详见[JEP409](./src/main/java/com/misitetong/jdk17/JEP409.java)
> 一个 密封类 是通过 sealed 修饰符声明的类或接口，它显式指定哪些类可以继承它（或实现它），这些子类称为 permitted subclasses（许可子类）

⚠️注意：所有直接子类必须显式指定其“密封性”，且只能是以下三种之一：
1. `final`: 不能再被继承（叶子类）✅ 最常用
2. `sealed`：自身也是密封类，需指定自己的 permits
3. `non-sealed`：可以被任意类继承（打破密封链）⚠️ 谨慎使用
```java
public sealed class Shape
    permits Circle, Rectangle, Square { }
```
如果子类与密封类在同一个源文件中，可以省略 permits：
```java
// Shape.java
sealed class Shape { }

final class Circle extends Shape { }
final class Rectangle extends Shape { }
non-sealed class Square extends Shape { } // 可继承
```
### 作用域与可见性要求
1. 所有 许可子类 必须与密封类在同一模块（module）中
2. 如果不在命名模块中（即 classpath 模式），则必须在同一包中
3. 子类必须直接继承密封类（不能隔代）
### 与 JEP 406（Pattern Matching for switch）协同
密封类的最大价值在于与 模式匹配的 switch 配合使用：
```java
int eval(Expr e) {
    return switch (e) {
        case Constant c -> c.value();
        case Add a      -> eval(a.left()) + eval(a.right());
        case Mul m      -> eval(m.left()) * eval(m.right());
    }; // ✅ 无需 default！编译器知道已覆盖所有子类
}
```
🔒 完整性检查（Completeness Check）：
编译器能静态验证 switch 是否覆盖了所有许可子类，避免运行时遗漏。

## [移除试验性的AOT和JIT编译器（JEP410）](https://openjdk.org/jeps/410)

### 背景
Java Ahead-of-time compilation (jaotc)工具是通过[JEP295](https://openjdk.org/jeps/295) 在 JDK 9 中作为实验性功能引入的。
jaotc 工具使用 Graal 编译器进行 AOT 编译，而 Graal 编译器本身是用 Java 编写的。
Graal 编译器在 JDK 10 中通过[JEP317](https://openjdk.org/jeps/317) 作为实验性的 JIT 编译器提供。

这些功能都可以由`GraalVM`替代

### 移除的内容
 - 移除的模块：
   ```text
   1. jdk.aot — the jaotc tool
   2. jdk.internal.vm.compiler — the Graal compiler
   3. jdk.internal.vm.compiler.management — Graal's MBean
   ```
 - 移除的代码
   ```text
   1. src/hotspot/share/aot — dumps and loads AOT code
   2. Additional code guarded by #if INCLUDE_AOT
   ```
 - 保留下面的内容，以确保`JVMCI`(jdk.internal.vm.ci,[JEP243](https://openjdk.org/jeps/243))模块能够继续正常构建
   ```text
   1. src/jdk.internal.vm.compiler/share/classes/module-info.java
   2. src/jdk.internal.vm.compiler.management/share/classes/module-info.java
   ```
   
## [废弃安全管理器SecurityManager以便后续移除（JEP411）](https://openjdk.org/jeps/411)

### 为什么要弃用
1. 时代变化 + 用法减少

   Security Manager + Java Policy 是过去 Java 平台用来做安全控制（sandboxing, 权限限制）的机制。
   随着现代应用使用容器化（Docker / Kubernetes）、模块化、安全沙箱机制、运维与部署方式的改变，以及其他语言／平台安全控制方案的丰富，许多开发者／系统已经不依赖 Java-level 的 Security Manager。
   实际上，在很多项目中，Security Manager 并不被启用或配置。使用频率很低。

2. 安全机制局限性与复杂性

   Security Manager 的配置和管理不够灵活，有时难以正确配置，或者对现代模块系统（JPMS）、反射、动态代理等功能支持不完善。
保持其兼容性和维护成本高：随着 JDK 演进，引入新特性，继续维护一个复杂且使用率低的安全子系统变得负担。

3. 更现代、更强、安全性更好的替代方案

   在现实部署中，更常用的是操作系统层面的安全隔离（容器、虚拟机、操作系统权限）、硬件隔离、沙箱／权限分离、模块化权限模型等。
对于需要更强隔离控制的场景，可采用专门的安全框架或运行时沙箱技术，而不依赖 Security Manager。
因此，继续维护一个过时、不灵活、安全性／兼容性有局限、使用率低的机制意义不大。

### 弃用内容

```text
1. java.lang.SecurityManager
2. java.lang.System::{setSecurityManager, getSecurityManager}
3. java.security.{Policy, PolicySpi, Policy.Parameters}
4. java.security.{AccessController, AccessControlContext, AccessControlException, DomainCombiner}
5. java.lang.Thread::checkAccess, java.lang.ThreadGroup::checkAccess, and java.util.logging.LogManager::checkAccess
6. java.util.concurrent.Executors::{privilegedCallable, privilegedCallableUsingCurrentClassLoader, privilegedThreadFactory}
7. java.rmi.RMISecurityManager
8. javax.security.auth.SubjectDomainCombiner and javax.security.auth.Subject::{doAsPrivileged, getSubject}
```

## [外部函数和管理内存的API（孵化器）（JEP412）](https://openjdk.org/jeps/412)

### 相关的JEP变更
1. [JEP 424](https://openjdk.org/jeps/424): Foreign Function & Memory API (Preview)
2. [JEP 389](https://openjdk.org/jeps/389): Foreign Linker API (Incubator)
3. [JEP 393](https://openjdk.org/jeps/393): Foreign-Memory Access API (Third Incubator)
4. [JEP 419](https://openjdk.org/jeps/419): Foreign Function & Memory API (Second Incubator)

### 代码示例

执行[JEP412.java](src/main/java/com/misitetong/jdk17/JEP412.java)需要带上`VM OPTIONS`
```shell
--add-modules=jdk.incubator.foreign
--enable-native-access=ALL-UNNAMED
```

```java
// 获取链接库中的strlen函数
MemoryAddress strlen = CLinker.systemLookup().lookup("strlen").get();
// 获取方法的handle
MethodHandle strlenHandle = CLinker.getInstance().downcallHandle(
       strlen,
       MethodType.methodType(int.class, MemoryAddress.class),
       FunctionDescriptor.of(C_INT, C_POINTER)
);
// 获取字符串
var string = CLinker.toCString("Hello World!!", ResourceScope.newImplicitScope());
// 调用C中的strlen函数
System.out.println(strlenHandle.invoke(string.address()));
```

## [向量API（第二次孵化）（JEP414）](https://openjdk.org/jeps/414)

### 相关的JEP变更
1. [JEP 338](https://openjdk.org/jeps/338): Vector API (Incubator)
2. [JEP 417](https://openjdk.org/jeps/417): Vector API (Third Incubator)

### 背景

向量API旨在提供一种清晰、可移植的方式，让 Java 开发者能编写高性能的向量化计算代码（如数值计算、机器学习、图像处理等）。
1. 在运行时自动利用 CPU 的 SIMD 指令（如 Intel AVX、ARM NEON），而无需手写 JNI 或汇编。
   ```asm
      vaddps ymm0, ymm1, ymm2  ; AVX 指令，一次加 8 个 float
   ```
2. 与 HotSpot JIT 编译器深度集成，在运行时将 Vector API 调用编译为高效的向量指令。
3. 保持 Java 的安全性和平台无关性，即使底层硬件不支持 SIMD，也能回退到标量实现（性能降级但功能正确）。

### 使用

1. 是否支持向量化优化
   ```shell
   # 期待输出true
   java -XX:+PrintFlagsFinal -version | grep UseSuperWord
   ```
2. 使用`Benchmark`测试优化效率详见[JEP414.java](src/main/java/com/misitetong/jdk17/JEP414.java)
   ```java
   // 普通循环版本
    @Benchmark
    public void scalarAdd(Blackhole bh) {
        double[] c = new double[SIZE];
        for (int i = 0; i < SIZE; i++) {
            c[i] = a[i] * a[i] + b[i] * b[i];
        }
        bh.consume(c); // 防止 JIT 优化掉整个计算（死码消除）
    }

    // Vector API 版本
    @Benchmark
    public void vectorAdd(Blackhole bh) {
        final var SPECIES = DoubleVector.SPECIES_PREFERRED;
        // JDK 21+ 请用: java.util.vector.FloatVector.SPECIES_PREFERRED
        double[] c = new double[SIZE];
        int i = 0;
        for (; i < SPECIES.loopBound(SIZE); i += SPECIES.length()) {
            var va = DoubleVector.fromArray(SPECIES, a, i);
            var vb = DoubleVector.fromArray(SPECIES, b, i);
            var vc = va.mul(va).add(vb.mul(vb));
            vc.intoArray(c, i);
        }
        bh.consume(c);
    }
   ```
3. 启动`Benchmark`
   1. 启动类：org.openjdk.jmh.Main
   2. 启动参数`VM OPTIONS` ：
   `--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED`
