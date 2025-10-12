package com.misitetong.jdk11;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zyy
 * @Date 2025/10/12 22:40
 * @Description <a href="https://openjdk.org/jeps/318">JEP309</a>
 */
public class JEP318 {
    public static void main(String[] args) {
        final int _1MB = 1024 * 1024;
        long totalAllocated = 0;
        long start = System.nanoTime();
        List<byte[]> holder = new ArrayList<>(); // 保留引用

        try {
            while (true) {
                byte[] block = new byte[_1MB];
                holder.add(block); // 不释放
                totalAllocated += _1MB;

                if (totalAllocated % (100L * _1MB) == 0) {
                    long elapsed = System.nanoTime() - start;
                    double seconds = elapsed / 1_000_000_000.0;
                    double mbPerSec = totalAllocated / (1024.0 * 1024.0) / seconds;
                    System.out.printf("已分配：%d MB，耗时：%.2f s，速率：%.2f MB/s%n",
                            totalAllocated / _1MB, seconds, mbPerSec);
                }
            }
        } catch (OutOfMemoryError e) {
            long elapsed = System.nanoTime() - start;
            double seconds = elapsed / 1_000_000_000.0;
            System.out.println("\n=== 运行结束 ===");
            System.out.printf("总共分配了 %.2f MB 内存%n", totalAllocated / 1024.0 / 1024.0);
            System.out.printf("运行时长：%.2f 秒%n", seconds);
            System.out.println("触发了 OutOfMemoryError。");
        }
    }
}
