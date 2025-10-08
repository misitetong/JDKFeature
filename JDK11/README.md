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


