# JDK11 新特性

## [嵌套访问控制（JEP181）](https://openjdk.org/jeps/181)

### 背景
jvm允许一个源文件中放多个class。这对于用户是透明的，用户认为它们在一个class中，所以希望它们共享同一套访问控制体系。
因此在一个class中的类可以互相访问各自类的私有成员变量。
在JDK8中通过`INNERCLASS`来识别嵌套类，通过增加一个`static synthetic access`来实现私有变量的访问
```shell
# build module JDK8 with jdk8 first
cd path/to/class file/for/JDK8/src/main/java/com/misitetong/jdk11/JEP181.java
# for more details use "javap -verbose JEP181\$Nest2" or idea's plugin "Asm Bytecode Viewer"
javap -p JEP181\$Nest2 
```
```java
// result for javap
public class com.misitetong.jdk11.JEP181$Nest2 {
    private int nestVariable;
    public com.misitetong.jdk11.JEP181$Nest2();
    static int access$000(com.misitetong.jdk11.JEP181$Nest2);
}
```
### 缺点
1. 编译器通过增加`static synthetic access`方法来绕过JVM限制，这种方式和封装相违背，并且轻微的增加程序的大小，会干扰用户和工具。所以我们希望一种更直接，更安全，更透明的方式。
2. 这种绕过的方式不兼容反射，使用反射进行私有方法调用的时候会报错。而反射应该和源码级访问拥有相同权限，这种表现不一致的行为是有问题的。见`JDK11/src/main/java/com/misitetong/jdk11/JEP181.java`

### JEP181改进

1. 顶级类增加`NESTMEMBER`字段
2. 内部类增加`NESTHOST`字段，让jvm天然的知道嵌套关系，允许相同顶级类的类之间访问私有属性、方法
3. 修复了使用反射进行私有方法调用报错的问题，使其与源码级访问拥有相同权限

## [动态类文件常量（JEP309）](https://openjdk.org/jeps/309)
### 背景
在 JDK 8 之前，Java 的常量池项是固定几种类型：`CONSTANT_Integer`, `CONSTANT_String`, `CONSTANT_Class`, `CONSTANT_MethodHandle` 等等。
每种常量类型都要在类加载时一次性解析并存入内存。这些常量都是静态的、编译时确定的。但现实中，很多“逻辑上是常量”的值，无法在编译时确定，例如：
1. 从配置文件读取的字符串
2. 通过加密/哈希生成的密钥
3. 语言特性所需的运行时构造对象（如 lambda 表达式的实现类、模式匹配的谓词）

Java 7 引入的 invokedynamic 允许动态解析方法调用点，极大提升了动态语言（如 JRuby、Nashorn）在 JVM 上的性能。
JEP 309 将这一思想推广到“常量”领域：既然方法调用可以动态解析，为什么常量不能？
```shell
# build module JDK8 with jdk8 first
cd path/to/class file/for/JDK8/src/main/java/com/misitetong/jdk11/JEP309.java
javap -v JEP309
```
```text
// result for javap
static {};
    descriptor: ()V
    flags: (0x0008) ACC_STATIC
    Code:
      stack=3, locals=0, args_size=0
         0: new           #5                  // class java/lang/StringBuilder
         3: dup
         4: invokespecial #6                  // Method java/lang/StringBuilder."<init>":()V
         7: ldc           #7                  // String Hello World
         9: invokevirtual #8                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        12: invokestatic  #9                  // Method java/lang/System.currentTimeMillis:()J
        15: invokevirtual #10                 // Method java/lang/StringBuilder.append:(J)Ljava/lang/StringBuilder;
        18: invokevirtual #11                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        21: putstatic     #3                  // Field MSG:Ljava/lang/String;
        24: return
      LineNumberTable:
        line 10: 0
```
使用JDK8编译后，查看`JEP309`的字节码文件可以看到，类加载的时候就通过`invokestatic`指令调用`java/lang/System.currentTimeMillis()`创建了字符串拼接后的结果。
字符串拼接使用的是`java/lang/StringBuilder.append()`

### 缺点
1. 如果想支持一种新的常量池类型（比如新的 lambda 表达式常量），必须修改 JVM 规范和类文件结构。
2. 每种常量类型都要在类加载时一次性解析并存入内存。哪怕程序运行过程中根本没用到，也会提前加载。这导致应用启动慢、内存占用大。

### JEP309改进
动态类文件常量的核心思想是将常量的解析延迟到运行时，而不是在编译时确定。这为动态语言特性和运行时优化提供了更大的灵活性。
```shell
# build module JDK11 with jdk11 first
cd path/to/class file/for/JDK11/src/main/java/com/misitetong/jdk11/JEP309.java
javap -v JEP309
```
```text
...
Constant pool:
   #2 = InvokeDynamic      #0:#26         // #0:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
   #24 = MethodHandle       6:#33          // REF_invokeStatic java/lang/invoke/StringConcatFactory.makeConcatWithConstants:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
   #25 = String             #34            // Hello \u0001
   #26 = NameAndType        #35:#36        // makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
   #33 = Methodref          #43.#44        // java/lang/invoke/StringConcatFactory.makeConcatWithConstants:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
   #34 = Utf8               Hello \u0001
   #35 = Utf8               makeConcatWithConstants
   #43 = Class              #45            // java/lang/invoke/StringConcatFactory
   #44 = NameAndType        #35:#49        // makeConcatWithConstants:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
   #45 = Utf8               java/lang/invoke/StringConcatFactory
   #46 = Class              #51            // java/lang/invoke/MethodHandles$Lookup
   #47 = Utf8               Lookup
   #48 = Utf8               InnerClasses
   #49 = Utf8               (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
   #50 = Class              #52            // java/lang/invoke/MethodHandles
   #51 = Utf8               java/lang/invoke/MethodHandles$Lookup
   #52 = Utf8               java/lang/invoke/MethodHandles
...
0: aload_0
1: iconst_0
2: aaload
3: invokedynamic #2,  0              // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
8: astore_1
...
BootstrapMethods:
  0: #24 REF_invokeStatic java/lang/invoke/StringConcatFactory.makeConcatWithConstants:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
    Method arguments:
      #25 Hello \u0001
```
使用JDK11编译后，查看`JEP309`的字节码文件可以看到，字符串拼接使用的是`invokedynamic`指令，
调用 `java/lang/invoke/StringConcatFactory.makeConcatWithConstants()`，让
JVM 通过 bootstrap method 动态计算常量（CONSTANT_Dynamic）。因此常量可以在运行时动态计算，并缓存在常量池中，成为真正的动态常量（Constant_Dynamic）。

可以注意到JDK8和JDK11的两个例子仍然会在类加载的时候就讲常量值加载进来，而不是延迟加载的形式。
`JDK17/src/main/java/com/misitetong/jdk11/JEP309.java`体现了延迟加载的特性，运行该文件可以得到如下结果
```text
Program start at 1760108322985
bootstrap() executed!
Constant value = Value generated at 1760108323009
Program end at 1760108323022
```
可以看到，静态常量`DynamicConstantDesc<String> LAZY_VALUE`只在第一次获取的时候才会进行计算并存储到常量池。
因此上述两个缺点被完美解决。

## [改进Aarch64函数（JEP315）](https://openjdk.org/jeps/315)
### 摘要
改进现有的字符串和数组内置函数，并在 AArch64 处理器上为 java.lang.Math 的 sin、cos 和 log 函数实现新的内置函数。

## [无操作垃圾回收器Epsilon（试验性）（JEP318）](https://openjdk.org/jeps/318)
### 背景
一个处理内存分配但不实现任何实际内存回收机制的GC。一旦可用的Java堆用尽，JVM就会关闭。
如果有System.gc()的调用，实际上什么也不会发生（这种场景下和-XX:+DisableExplicitGC效果一样），因为没有内存回收，这个实现可能会警告用户尝试强制GC是徒劳。
使用方法如下
```shell
cd path/to/JDKFeature/JDK11/src/main/java
javac com/misitetong/jdk11/JEP318.java
java -cp . -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -Xmx512m com.misitetong.jdk11.JEP318
```
执行上述命令可以看到如下结果（MacOS M1Max 32G）：
```text
已分配：100 MB，耗时：0.01 s，速率：11518.19 MB/s
已分配：200 MB，耗时：0.03 s，速率：6081.27 MB/s
已分配：300 MB，耗时：0.04 s，速率：7219.99 MB/s
已分配：400 MB，耗时：0.05 s，速率：8058.17 MB/s
已分配：500 MB，耗时：0.06 s，速率：8667.19 MB/s
Terminating due to java.lang.OutOfMemoryError: Java heap space
```
作为比较，这里贴上`Parallel GC`（默认 GC，JDK 8~11）的结果，与EpsilonGC相比，可以看到无GC的理论内存分配值会更高：
```text
# java -cp . -XX:+UseParallelGC -Xmx512m com.misitetong.jdk11.JEP318
已分配：100 MB，耗时：0.01 s，速率：13812.87 MB/s
已分配：200 MB，耗时：0.04 s，速率：4798.68 MB/s
已分配：300 MB，耗时：0.07 s，速率：4160.89 MB/s
已分配：400 MB，耗时：0.09 s，速率：4537.52 MB/s

=== 运行结束 ===
总共分配了 468.00 MB 内存
运行时长：0.10 秒
触发了 OutOfMemoryError。
```
### 主要用途
1. 性能测试：在做性能测试时，有时候我们并不想让 GC 干扰结果。例如测量纯计算性能或 JIT 编译性能。Epsilon 可以确保没有 GC 暂停，不影响测试纯度。（它可以帮助过滤掉GC引起的性能假象）；
2. 内存压力测试：如果想测试应用的内存分配速率（allocation rate）或内存峰值，可以用 Epsilon 作为工具，因为它能准确反映堆用尽的时间。（例如，知道测试用例应该分配不超过1 GB的内存，我们可以使用-Xmx1g配置-XX:+UseEpsilonGC，如果违反了该约束，则会heap dump并崩溃）；
3. 非常短的JOB任务（对于这种任务，接受GC清理堆那都是浪费空间）；
4. VM接口测试：出于VM开发目的，拥有一个简单的GC有助于了解VM-GC接口拥有功能分配器的绝对最低要求。对于无操作GC，接口不应该实现任何东西，良好的界面意味着Epsilon的BarrierSet只会使用默认实现中的无操作屏障实现；
5. Last-drop 延迟&吞吐改进：Epsilon GC 消除了所有 GC 成本，获得了Last-drop的极限性能，用于建立 JVM 性能的理论上限基准；

## [删除Java EE和CORBA模块（JEP320）](https://openjdk.org/jeps/320)
### 移除的模块
1. java.xml.ws (JAX-WS, plus the related technologies SAAJ and Web Services Metadata)
2. java.xml.bind (JAXB，XML到Java对象的绑定)
3. java.activation (JAF)
4. java.xml.ws.annotation (服务注解)
5. java.corba (CORBA)
6. java.transaction (JTA)
7. java.se.ee (Aggregator module for the six modules above)
8. jdk.xml.ws (Tools for JAX-WS)
9. jdk.xml.bind (Tools for JAXB)
### 移除的工具
1. jdk.xml.ws 模块中移除了以下JAX-WS 工具： 
   - wsgen 
   - wsimport
2. jdk.xml.bind 模块移除了以下JAXB 工具： 
   - schemagen 
   - xjc 
3. java.corba 模块移除以下CORBA 工具： 
   - idlj 
   - orbd 
   - servertool 
   - tnamesrv
### 其他
更多内容参考：[JDK 11 Release Notes, Important Changes, and Information](https://www.oracle.com/java/technologies/javase/11-relnote-issues.html#JDK-8190378)
