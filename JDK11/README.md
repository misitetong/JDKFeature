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

## [HTTP Client API（JEP321）](https://openjdk.org/jeps/321)
### 背景
`HTTP/2`客户端在JDK9引入并在JDK10更新，现在JDK11将其标准化支持同步与异步模式，成为 Java 网络通信的核心API。
1. [HTTP/2客户端（孵化器）（JEP110）](https://openjdk.org/jeps/110)
2. [HTTP客户端API的HTTP/2（JEP321）](https://openjdk.org/jeps/321)
3. [HTTP客户端API的HTTP/3（JEP517）](https://openjdk.org/jeps/517)

| Compare                    | JEP 110 (JDK 9/10)                                   | JEP 321 (JDK 11)                                              |
|----------------------------|------------------------------------------------------|---------------------------------------------------------------|
| package name               | `jdk.incubator.http`                                 | `java.net.http`                                               |
| `HttpResponse.BodyHandler` | `HttpResponse.BodyHandler.asString()`                | `HttpResponse.BodyHandlers.ofString()`                        |
| use method                 | compile with `--add-modules jdk.incubator.httpclient` | direction                                                     |
| HttpResponse Implementation method | `BodyProcessor<T>` | `BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>>` |

### 深入理解`BodySubscriber`

```java
/**
 * copy from java.net.http.HttpResponse.BodySubscriber
 * @param <T>
 */
public interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {
     /**
      * Returns a {@code CompletionStage} which when completed will return
      * the response body object. This method can be called at any time
      * relative to the other {@link Flow.Subscriber} methods and is invoked
      * using the client's {@link HttpClient#executor() executor}.
      *
      * @return a CompletionStage for the response body
      */
     public CompletionStage<T> getBody();
 }
```
通过实现Reactive Streams 标准大大提升了`HttpClient`的异步处理能力
1. `BodySubscriber`的引入使得`HttpClient`能够逐步（异步）处理 HTTP 响应体数据。每次接收到一部分数据时，onNext() 会被调用，并可以立即对这些数据进行处理（例如，拼接字符串、处理流、保存文件等），而无需等到所有数据都完全接收完毕。
2. 这种非阻塞的处理方式允许`HttpClient`在等待响应体的过程中继续执行其他任务（例如发送其他请求、处理计算等），大大提升了系统的并发性能。
3. `BodySubscriber`使得`HttpClient API`具有很高的灵活性，可以支持自定义的响应体处理方式。开发者可以根据自己的需求，提供不同的 BodySubscriber 实现，以满足各种场景的需要（如将响应体直接存储到文件、将其转化为某种数据格式等）。

自定义`BodySubscriber`代码示例如下，使用方式详见[JEP321.java](src/main/java/com/misitetong/jdk11/JEP321.java)
```java
public class AsyncStringBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {

     private final CompletableFuture<T> result = new CompletableFuture<>();
     private final StringBuilder builder = new StringBuilder();

     @Override
     public void onSubscribe(Flow.Subscription subscription) {
         subscription.request(Long.MAX_VALUE);
     }

     @Override
     public void onNext(List<ByteBuffer> items) {
         for (ByteBuffer buffer : items) {
             byte[] bytes = new byte[buffer.remaining()];
             buffer.get(bytes);
             builder.append(new String(bytes));
         }
     }

     @Override
     public void onError(Throwable throwable) {
         result.completeExceptionally(throwable);
     }

     @Override
     public void onComplete() {
         result.complete(JSON.parseObject(builder.toString(), new TypeReference<T>() {
         }));
     }

     @Override
     public CompletionStage<T> getBody() {
         return result;
     }
 }
```

## [Lambda参数的局部变量语法（JEP323）](https://openjdk.org/jeps/323)

### 背景
局部变量类型推断指的是java不在需要给变量定义具体的数据类型而是使用var来定义，编译器能根据右边表达式自动推断类型。
在`JDK10`中可以用`var`推断局部变量的类型，例如：
```java
var message = "Hello, world!";
var list = new ArrayList<String>();
```
但是在`lambda`中`var`并不可用。而`JEP323`使得`var`在`lambda`中合法。
```java
(x, y) -> x + y        // ✅ 可行
(var x, var y) -> x + y // ❌ Java 10 不支持；✅Java 11 支持
```

### JEP323优点
1. 可以使得lambda参数带上注解
2. 可以使得lambda参数带上修饰符例如：`final`

如下面[JEP323.java](src/main/java/com/misitetong/jdk11/JEP323.java)代码所示加上了`final`之后，`a = a + 1`会在编译期报错。
```java
// JDK11/src/main/java/com/misitetong/jdk11/JEP323.java
BiFunction<String, String, String> concat = (final var a, final var b) -> {
   a = a + 1;
   return a + b;
};
```

### 注意点
由于`JEP323.java`不改变`lambda`在运行时的类型推断与元数据生成方式，因此对于`var`的注解和修饰符只在编译期有效。
因此运行`JEP323.java`可以得到如下结果。可以看到正常的方法可以在运行时获取到注解，而即使`lambda`的参数上标注了注解，也无法在运行使其被获取。
```text
arg0 -> [@jakarta.validation.constraints.NotNull(message="{jakarta.validation.constraints.NotNull.message}", groups={}, payload={})]
arg1 -> [@jakarta.validation.constraints.NotNull(message="{jakarta.validation.constraints.NotNull.message}", groups={}, payload={})]
arg0 -> []
arg1 -> []
```
因此如果想要在运行时期对`lambda`参数使用注解完成一些自定义操作如使用`@NotNull`进行非空校验是无法生效的。
尽管如此，我们在编译时期仍然可以对`lambda`参数上的注解进行一些操作。示例如下：
解开`JEP323.java:Line:47`的注释，重新运行，我们可以发现，`org.checkerframework.checker`会在编译时就将`Null`值报出来，
这表明`lambda`参数上的`@NotNull`注解在编译时起起效果了。为了更加直观的观察编译时期的`lambda`参数注解的变化，现将`JEP323.java`的编译文件展示如下：
```shell
javap -p -v JEP323.class | grep "RuntimeVisibleParameterAnnotations" -A 5
```
```text
RuntimeVisibleParameterAnnotations:
   parameter 0:
     0: #55()
       jakarta.validation.constraints.NotNull
   parameter 1:
     0: #55()
```
可见`JEP323.java`中编译的注解只有`staticConcat`方法参数的注解而没有`concat`方法参数的注解，继续执行下面命令观察：
```shell
javap -p -v JEP323.class | grep "lambda$static$0" -A 5
```
可见`lambda`表达式并不是直接编译成一个匿名类，而是通过`invokedynamic + LambdaMetafactory`动态生成方法引用，编译后会生成一个合成方法（synthetic method）。
```text
private static synthetic lambda$static$0(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
```

## [现代椭圆曲线密钥协商算法X25519和X448（JEP324）](https://openjdk.org/jeps/324)

### JEP324的优化
运行`JEP325.java`可以得到如下的结果（MACOS M1MAX 32G）。可以看到新的算法明显提升了运行速度
```text
Shared X25519 secret equal: true
Total time: 1131.82 ms
Average per op: 113.182 μs
Throughput: 8835.3 ops/sec
Shared X448 secret equal: true
Total time: 4530.48 ms
Average per op: 453.048 μs
Throughput: 2207.3 ops/sec
Shared EC secret equal: true
Total time: 6290.61 ms
Average per op: 629.061 μs
Throughput: 1589.7 ops/sec
```
下面表格生成自GPT-5
 - 算法核心原理对比

   | 特性                     | X25519        | X448          | ECDH (P-256/P-384)  |
   | ---------------------- | ------------- | ------------- | ------------------- |
   | 曲线类型                   | Montgomery 曲线 | Montgomery 曲线 | Weierstrass 曲线      |
   | 模数位长                   | 255-bit       | 448-bit       | 256-bit / 384-bit   |
   | 对应安全强度                 | ≈128-bit      | ≈224-bit      | ≈128-bit / ≈192-bit |
   | 参数来源                   | 透明随机生成（无魔法常数） | 同上            | 由 NIST 发布（存在信任争议）   |
   | 算法流程                   | 固定形式，不可配置     | 固定形式          | 参数化可变               |
   | 是否抗侧信道攻击               | ✅ 是（固定时间实现）   | ✅ 是           | ❌ 否（实现依赖）           |
   | 是否支持 Montgomery Ladder | ✅             | ✅             | ❌                   |

 - 安全性对比

   | 安全维度              | X25519         | X448           | ECDH (P-256/P-384) |
   | ----------------- | -------------- | -------------- | ------------------ |
   | 抗侧信道攻击            | ✅ 固定时间实现       | ✅ 固定时间实现       | ⚠️ 实现依赖（可能泄露）      |
   | 抗故障攻击             | ✅ 固定 ladder 算法 | ✅ 固定 ladder 算法 | ⚠️ 实现依赖            |
   | 随机性需求             | 仅需私钥随机         | 同上             | 需随机点选曲线参数          |
   | 常数生成方式            | 无魔法常数（透明）      | 无魔法常数（透明）      | 存在不透明常数（争议）        |
   | 安全审计              | 独立验证，广泛审查      | 独立验证           | NIST 标准但有信任争议      |
   | 对等 Diffie-Hellman | ✅ 简单           | ✅ 简单           | ✅ 标准支持             |

 - 标准与兼容性
   
   | 维度         | X25519                             | X448     | ECDH (P-256/P-384) |
   | ---------- | ---------------------------------- | -------- | ------------------ |
   | TLS 1.3 支持 | ✅ 默认推荐                             | ✅ 可选     | ✅ 向后兼容             |
   | OpenSSL 支持 | ✅                                  | ✅        | ✅                  |
   | SSH 支持     | ✅ (`curve25519-sha256@libssh.org`) | ❌ 较少     | ✅                  |
   | JWT/JWE 支持 | ✅                                  | ❌        | ✅                  |
   | 硬件/老系统兼容性  | ✅ 广泛                               | ⚠️ 有限    | ✅ 最兼容              |
   | 密钥长度       | 32 bytes                           | 56 bytes | 32 / 48 bytes      |

 - 经典应用场景
   
   | 应用领域                                | 推荐算法        | 原因                         |
   | ----------------------------------- | ----------- | -------------------------- |
   | **TLS 1.3 / HTTPS**                 | X25519      | 默认推荐，性能最佳                  |
   | **Signal / WhatsApp / Telegram 加密** | X25519      | 安全 + 高效                    |
   | **SSH 密钥交换**                        | X25519      | 默认支持 (`curve25519-sha256`) |
   | **高安全 VPN / 政府通信**                  | X448        | 更高安全强度                     |
   | **旧系统兼容 (TLS 1.2)**                 | ECDH(P-256) | 保留兼容性                      |

 - 总结
   
   | 维度      | X25519      | X448        | ECDH(P-256/P-384) |
   | ------- | ----------- | ----------- | ----------------- |
   | 性能      | 🥇 快        | 🥈 中        | 🥉 慢              |
   | 安全强度    | 高           | 最高          | 中等                |
   | 抗侧信道攻击  | ✅           | ✅           | ⚠️                |
   | 参数透明性   | ✅           | ✅           | ❌                 |
   | 兼容性     | ✅           | ⚠️ 一般       | ✅                 |
   | 推荐用途    | 现代协议        | 高保密场景       | 向后兼容              |
   | Java 支持 | ✅ (JDK 11+) | ✅ (JDK 11+) | ✅ (JDK 1.7+)      |

## [Unicode 10（JEP327）](https://openjdk.org/jeps/327)

### 目标
支持最新版本的Unicode，主要在以下类中：

`java.lang`包中的`String`和`Character`，
`java.awt.font`包中的`NumericShaper`，以及
`java.text`包中的`Bidi`、`BreakIterator`和`Normalizer`。

Java SE 10实现Unicode 8.0。Unicode 9.0增加了7500个字符和六个新脚本，Unicode 10.0.0增加了8518个字符和四个新脚本。此升级将包括Unicode 9.0更改，因此将总共增加16,018个字符和十个新脚本。

### 非目标
本JEP不会实现四个相关的Unicode规范：

 - UTS #10，Unicode整理算法
 - UTS #39，Unicode安全机制
 - UTS #46，Unicode IDNA兼容性处理
 - UTS #51，Unicode表情符号

## [Flight Recorder（JEP328）](https://openjdk.org/jeps/328)

### 背景
JFR （Java Flight Recorder）是一种分析工具，之前是Oracle JDK 中的商业插件，但在 Java 11 中，其代码被包含到公开代码库中，用于从正在运行的 Java 应用程序中收集诊断和分析数据。它的性能开销可以忽略不计，通常低于 1%。 因此它可以用于生产应用程序。

Java 语言中的飞行记录器类似飞机上的黑盒子，是一种低开销的事件信息收集框架，主要用于对 应用程序和 JVM 进行故障检查、分析。飞行记录器记录的 主要数据源于应用程序、JVM 和 OS，这些事件信息 保存在单独的事件记录文件中，故障发生后，能够从事件记录文件中 提取出有用信息 对故障进行分析。

### JFR使用
```shell
# 开启飞行记录器，开启60s，保存文件到app.jfr，在程序退出时自动保存文件
java -XX:StartFlightRecording=duration=60s,filename=app.jfr,dumponexit=true -jar app.jar

# 动态开启飞行记录器，配置文件profile
jcmd <pid> JFR.start name=MyRecording settings=profile duration=60s filename=app.jfr
# 手动保存当前的jfr
jcmd <pid> JFR.dump filename=app.jfr
# 查询当前录制状态
jcmd <pid> JFR.check
jcmd <pid> JFR.stop
```
官方JMC：[下载链接](https://jdk.java.net/jmc)

使用下面命令运行`JEP328.java`，或者在`idea`的`Run Configuration`中配置`vm options`参数
```shell
cd path/to/JDKFeature/JDK11/src/main/java
javac com/misitetong/jdk11/JEP328.java
java -cp . -XX:StartFlightRecording=duration=60s,filename=app.jfr,dumponexit=true com.misitetong.jdk11.JEP328
```
 - 使用`jmc`打开`jfr`文件可以得到如下结果：
![JEP328.png](assets/JEP328.png)

 - 代码读取`jfr`文件
```java
import java.nio.file.*;
import jdk.jfr.consumer.*;

Path p = Paths.get("recording.jfr");
for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
   System.out.println(e.getStartTime() + " : " + e.getValue("message"));
}
```

### JFR 与常见性能分析工具对比表
数据由GTP-5生成

| 对比维度                  | **Java Flight Recorder (JFR)**   | **VisualVM**      | **async-profiler**     | **YourKit / JProfiler** | **Perf (Linux)**          | **BPF / eBPF 工具链**     |
| --------------------- | -------------------------------- | ----------------- | ---------------------- | ----------------------- | ------------------------- | ---------------------- |
| **来源 / 许可证**          | OpenJDK 官方（JEP 328 / 349），开源     | OpenJDK 附带（GPLv2） | 开源（GPLv2）              | 商业软件（付费）                | Linux 原生                  | 开源（内核工具）               |
| **引入版本**              | JDK 11 (正式集成)，JDK 14 起带 `jfr` 命令 | 早期工具              | 独立原生工具                 | 第三方商业版                  | 内核级工具                     | 内核级工具                  |
| **采样方式**              | 事件驱动 + 周期采样                      | 抽样 + attach 监控    | 原生信号采样（perf_event）     | Instrumentation + 采样    | 低层硬件计数器                   | eBPF 程序插桩              |
| **性能开销**              | 🟢 极低（<1%，可长期开启）                 | 🟡 中等（5%~15%）     | 🟢 低（~2%）              | 🔴 高（10%~40%）           | 🟢 低（~1%）                 | 🟢 低（~1%）              |
| **生产环境可用性**           | ✅ 非侵入式，可长期启用                     | ⚠️ 可短期调试，不推荐长期    | ✅ 适合线上分析               | ❌ 不推荐生产使用               | ✅ 专业性能工程                  | ✅ 高级生产追踪               |
| **是否需要 Agent Attach** | ❌（内置于 JVM）                       | ✅ 需要 attach JVM   | ✅ 需要 attach            | ✅ 需要 attach agent       | ❌                         | ✅ 通过 eBPF 插桩           |
| **支持的事件类型**           | JVM + 应用层事件（GC、锁、I/O、CPU、异常、线程）  | JVM 指标、堆、线程       | CPU、堆栈采样               | 方法级调用链、内存分析             | 硬件事件（cycles、cache misses） | 自定义系统调用、网络、I/O         |
| **自定义事件支持**           | ✅ 支持（`jdk.jfr.Event`）            | ❌                 | ❌                      | ✅（通过探针）                 | ❌                         | ✅（写 eBPF 脚本）           |
| **堆分析能力**             | 🟡 可采集对象分配                       | 🟢 实时堆转储          | 🟡 简要采样                | 🟢 全堆快照                 | ❌                         | ❌                      |
| **方法级性能分析**           | ✅ 支持采样热点方法                       | 🟢                | 🟢                     | 🟢                      | 🟢                        | 🟢                     |
| **线程与锁分析**            | ✅ 内置锁竞争事件                        | 🟢 可视化线程状态        | 🟢 采样锁持有               | 🟢                      | 🟢（系统级）                   | 🟢（系统级）                |
| **可视化工具**             | 🟢 Java Mission Control (JMC)    | 🟢 自带 GUI         | 🔵 火焰图 (Flame Graph)   | 🟢 图形界面                 | 🔵 火焰图工具                  | 🔵 bpftrace/FlameGraph |
| **命令行工具**             | `jfr`、`jcmd`                     | 无（GUI为主）          | `profiler.sh`          | 无                       | `perf`                    | `bpftrace`、`bcc`       |
| **输出格式**              | `.jfr` 二进制文件                     | `.nps`、`.hprof`   | 火焰图（SVG/HTML）          | 专有格式                    | perf.data                 | 文本 / 火焰图               |
| **分析深度**              | JVM 层 + 应用层 + OS 层统计             | JVM 层             | 原生层                    | JVM 层                   | 硬件层                       | 内核层                    |
| **使用复杂度**             | 🟢 简单（JVM 内置命令）                  | 🟢 简单             | 🟡 需配置                 | 🟢 图形化操作                | 🔴 较复杂                    | 🔴 需要编写 eBPF 脚本        |
| **可持续运行监控**           | ✅ 支持长期采集                         | ❌                 | ✅                      | ❌                       | ✅                         | ✅                      |
| **典型场景**              | 性能分析、线上问题回溯、GC/线程诊断              | 本地调试、小规模测试        | 火焰图分析、低级 CPU Profiling | 离线性能调试                  | 系统级性能瓶颈分析                 | 高级生产监控                 |
| **JDK 依赖性**           | 高（JVM 内置）                        | 高                 | 无                      | 无                       | 无                         | 无                      |
| **优点总结**              | ✅ JVM 原生、低开销、事件丰富、生产可用           | ✅ 图形界面直观、易用       | ✅ 原生火焰图高精度             | ✅ 功能全面、界面强大             | ✅ 系统级性能精确                 | ✅ 可自定义内核/系统追踪          |
| **缺点总结**              | ❌ 分析需 JMC / jfr 工具，非即时           | ❌ 开销较高            | ❌ 无法记录 JVM 内部事件        | ❌ 侵入性强，非免费              | ❌ JVM 语义缺失                | ❌ 学习曲线陡峭               |

| 使用场景                     | 推荐工具                  | 说明               |
| ------------------------ | --------------------- | ---------------- |
| 🧩 **JVM 性能分析（GC、线程、锁）** | 🟢 **JFR**            | 内置、低开销、可长期启用     |
| 🧮 **CPU 火焰图分析**         | 🟢 async-profiler     | 原生信号采样更精准        |
| 🧰 **开发调试阶段性能测试**        | 🟢 VisualVM / YourKit | 图形化界面直观          |
| 🖥 **系统级性能瓶颈分析**         | 🟢 perf / eBPF        | 适合内核、I/O、网络分析    |
| 🧭 **生产环境持续监控 + 事后分析**   | 🟢 JFR + JMC          | 最安全、低扰动、JVM 原生支持 |


