package com.misitetong.jdk11;

/**
 * @author zyy
 * @Date 2025/10/9 21:39
 * @Description <a href="https://openjdk.org/jeps/309">JEP309</a>
 * @Tip you can find old combine result in JDK8/src/main/java/com/misitetong/jdk11/JEP309.java
 */
public class JEP309 {

    public static final String VALUE = "Hello, World! " + System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println(VALUE);
    }
}

