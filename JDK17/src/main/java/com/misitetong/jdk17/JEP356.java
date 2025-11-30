package com.misitetong.jdk17;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.random.RandomGenerator.SplittableGenerator;

/**
 * @author zyy
 * @Date 2025/11/30 15:35
 * @Description <a href="https://openjdk.org/jeps/356">com.misitetong.jdk17.JEP356</a>
 */
public class JEP356 {

    public static void main(String[] args) {
        // 使用默认生成器
        RandomGenerator defaultRng = RandomGenerator.getDefault();
        System.out.println("默认生成器 (RandomGenerator): ");
        System.out.println("随机整数: " + defaultRng.nextInt());
        System.out.println("随机整数 [0, 100): " + defaultRng.nextInt(100));
        System.out.println("随机长整数: " + defaultRng.nextLong());
        System.out.println("随机双精度: " + defaultRng.nextDouble());

        // 使用 SplittableRandom
        RandomGenerator splittableRandom = RandomGeneratorFactory.of("SplittableRandom").create();
        System.out.println("\nSplittableRandom: ");
        splittableRandom.ints().limit(5).forEach(System.out::println);

        // 使用 Xoroshiro128PlusPlus（高质量生成器）
        RandomGenerator xoroshiroRng = RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create();
        System.out.println("\nXoroshiro128PlusPlus: ");
        xoroshiroRng.ints().limit(5).forEach(System.out::println);

        // 演示生成器拆分
        SplittableGenerator splitRng = (SplittableGenerator) splittableRandom;
        // 拆分成多个子生成器
        RandomGenerator rng1 = splitRng.split();
        RandomGenerator rng2 = splitRng.split();
        RandomGenerator rng3 = splitRng.split();

        // 模拟不同线程生成随机数
        System.out.println("子生成器1随机数: " + rng1.nextInt());
        System.out.println("子生成器2随机数: " + rng2.nextInt());
        System.out.println("子生成器3随机数: " + rng3.nextInt());

    }
}
