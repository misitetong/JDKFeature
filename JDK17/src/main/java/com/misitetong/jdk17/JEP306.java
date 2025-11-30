package com.misitetong.jdk17;

/**
 * @author zyy
 * @Date 2025/11/10 20:34
 * @Description <a href="https://openjdk.org/jeps/306">com.misitetong.jdk11.JEP306</a>
 */
public class JEP306 {

    public static void main(String[] args) {
        double a = 1e16;
        double b = 1.0000000000000002;
        double c = -1e16;
        System.out.println("Result (default): " + (a + b + c));
    }

}
