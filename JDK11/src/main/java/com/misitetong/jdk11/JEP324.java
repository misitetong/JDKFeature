package com.misitetong.jdk11;

import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * @author zyy
 * @Date 2025/10/18 17:15
 * @Description <a href="https://openjdk.org/jeps/324">JEP324</a>
 */
public class JEP324 {

    public static void main(String[] args) throws Exception {
        executeAlgorithm("X25519");
        executeAlgorithm("X448");
        executeAlgorithm("EC");
    }

    public static void executeAlgorithm(String algorithm) throws Exception {
        // 1. 生成密钥对
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        KeyPair alice = kpg.generateKeyPair();
        KeyPair bob = kpg.generateKeyPair();

        // 2. Alice 计算共享密钥
        KeyAgreement aliceAgree = KeyAgreement.getInstance(Objects.equals(algorithm, "EC") ? "ECDH" : algorithm);
        aliceAgree.init(alice.getPrivate());
        aliceAgree.doPhase(bob.getPublic(), true);
        byte[] aliceSecret = aliceAgree.generateSecret();

        // 3. Bob 计算共享密钥
        KeyAgreement bobAgree = KeyAgreement.getInstance(Objects.equals(algorithm, "EC") ? "ECDH" : algorithm);
        bobAgree.init(bob.getPrivate());
        bobAgree.doPhase(alice.getPublic(), true);
        byte[] bobSecret = bobAgree.generateSecret();

        // 4. 验证双方共享密钥是否一致
        System.out.println("Shared " + algorithm + " secret equal: " + MessageDigest.isEqual(aliceSecret, bobSecret));

        long start = System.nanoTime();
        int iterations = 10_000;
        for (int i = 0; i < iterations; i++) {
            aliceAgree = KeyAgreement.getInstance(Objects.equals(algorithm, "EC") ? "ECDH" : algorithm);
            aliceAgree.init(alice.getPrivate());
            aliceAgree.doPhase(bob.getPublic(), true);
            aliceAgree.generateSecret();
        }
        long end = System.nanoTime();
        long elapsed = end - start;
        double perOpMicros = (elapsed / 1_000.0) / iterations;
        double opsPerSec = iterations / (elapsed / 1_000_000_000.0);

        System.out.printf("Total time: %.2f ms%n", elapsed / 1_000_000.0);
        System.out.printf("Average per op: %.3f μs%n", perOpMicros);
        System.out.printf("Throughput: %.1f ops/sec%n", opsPerSec);
    }
}
