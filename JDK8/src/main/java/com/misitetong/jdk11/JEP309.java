package com.misitetong.jdk11;

/**
 * @author zyy
 * @Date 2025/10/9 21:50
 * @Description <a href="https://openjdk.org/jeps/181">JEP181</a>
 */
public class JEP309 {

    public static final String VALUE = "Hello, World! " + System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println(VALUE);
    }
}
