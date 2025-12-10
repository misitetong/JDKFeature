package com.misitetong.jdk17;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * @author zyy
 * @Date 2025/12/10 23:38
 * @Description <a href="https://openjdk.org/jeps/414">com.misitetong.jdk17.JEP414</a>
 */
// 基准测试模式：平均时间（每次调用耗时）
@BenchmarkMode(Mode.AverageTime)
// 时间单位：纳秒
@OutputTimeUnit(TimeUnit.NANOSECONDS)
// 每个线程独享状态（避免干扰）
@State(Scope.Thread)
// JVM 启动 1 次（避免 fork 开销太大）
@Fork(value = 1, warmups = 0)
// 预热 3 轮（让 JIT 优化充分）
@Warmup(iterations = 3, time = 1)
// 正式测量 5 轮
@Measurement(iterations = 5, time = 1)
public class JEP414 {
    private static final int SIZE = 1_000_000;
    private final double[] a = new double[SIZE];
    private final double[] b = new double[SIZE];

    // 初始化数据（在 benchmark 前执行）
    @Setup
    public void setup() {
        for (int i = 0; i < SIZE; i++) {
            a[i] = i;
            b[i] = i * 2;
        }
    }

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
}
