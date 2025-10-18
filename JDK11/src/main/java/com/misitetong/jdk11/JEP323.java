package com.misitetong.jdk11;

import jakarta.validation.constraints.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author zyy
 * @Date 2025/10/14 22:47
 * @Description <a href="https://openjdk.org/jeps/323">JEP323</a>
 */
public class JEP323 {

    public static String staticConcat(@NotNull String a, @NotNull String b) {
        return a + b;
    }

    public static BiFunction<String, String, String> concat = (@NotNull var a, @NotNull var b) -> {
        a = a + " ";
        return a + b;
    };

    public static void main(String[] args) throws Exception {

//        BiFunction<String, String, String> concat = (final var a, final var b) -> {
//            a = a + 1;
//            return a + b;
//        };

        System.out.println("Result: " + concat.apply("Hello", "World!"));

        Optional<Method> optionalMethod = Arrays.stream(JEP323.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("staticConcat")).findFirst();
        optionalMethod.ifPresent(JEP323::printMethodAnnotation);

        Method[] declaredMethods = concat.getClass().getDeclaredMethods();
        Arrays.stream(declaredMethods).forEach(method -> {
            method.setAccessible(true);
            printMethodAnnotation(method);
        });

//        System.out.println("Result: " + concat.apply("Hello", null));
    }

    public static void printMethodAnnotation(Method method) {
        for (Parameter p : method.getParameters()) {
            System.out.println(p.getName() + " -> " + java.util.Arrays.toString(p.getAnnotations()));
        }
    }
}
