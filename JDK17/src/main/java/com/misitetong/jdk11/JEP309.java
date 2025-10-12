package com.misitetong.jdk11;

import java.lang.constant.*;
import java.lang.invoke.MethodHandles;

/**
 * @author zyy
 * @Date 2025/10/10 22:57
 * @Description <a href="https://openjdk.org/jeps/309">JEP309</a>
 */
public class JEP309 {

    private static final DynamicConstantDesc<String> LAZY_VALUE =
        DynamicConstantDesc.of(
            MethodHandleDesc.ofMethod(
                DirectMethodHandleDesc.Kind.STATIC,
                ClassDesc.of("com.misitetong.jdk11.JEP309"),
                "bootstrap",
                MethodTypeDesc.of(
                    ClassDesc.of("java.lang.String"),
                    ClassDesc.of("java.lang.invoke.MethodHandles$Lookup"),
                    ClassDesc.of("java.lang.String"),
                    ClassDesc.of("java.lang.Class")
                )
            )
        );

    public static String bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        System.out.println("bootstrap() executed!");
        return "Value generated at " + System.currentTimeMillis();
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("Program start at " + System.currentTimeMillis());
        Thread.sleep(100);
        String value = LAZY_VALUE.resolveConstantDesc(MethodHandles.lookup());
        System.out.println("Constant value = " + value);
        Thread.sleep(100);
        System.out.println("Program end at " + System.currentTimeMillis());
    }
}
