# JDK17 新特性

## [恢复始终严格模式（Always-Strict）的浮点语义（JEP306）](https://openjdk.org/jeps/306)

### 背景
在老版本的JDK时，JVM 为提高运算效率，允许在部分情况下使用更高精度的浮点寄存器（尤其在 x87 FP 上），导致可能不严格遵守IEEE 754标准，即
某些 CPU 浮点寄存器精度更高，导致结果与标准不同。这违反了JAVA的跨平台一致性。

而`strictfp`关键字的作用就是使得JVM强制遵守IEEE 754的标准，达到跨平台的一致性。
即在`non-strict`模式下浮点运算可能仍保留扩展精度 (80-bit)，但是`strictfp`会在每一步舍入到 (64-bit)

对比代码如[JEP306](../JDK11/src/main/java/com/misitetong/jdk17/JEP306.java)所示，但是需要在老的机器上运行才能看出差异。

因此现在取消`strictfp`关键字是因为现代JVM/CPU已普遍支持IEEE 754指令集（如 SSE2、AVX），无需依赖高精度寄存器优化。
