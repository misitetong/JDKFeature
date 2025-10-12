# JDK11 新特性

## 嵌套访问控制（JEP181）

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

## 动态类文件常量（JEP309）
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
