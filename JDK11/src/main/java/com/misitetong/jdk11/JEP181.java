package com.misitetong.jdk11;

import java.lang.reflect.Field;

/**
 * @author zyy
 * @Date 2025/10/8 19:28
 * @Description <a href="https://openjdk.org/jeps/181">JEP181</a>
 * @Tip you can find old combine method for nest class in JDK8/src/main/java/com/misitetong/jdk11/JEP181.java
 */
public class JEP181 {

    public static void main(String[] args) throws Exception{
        new Nest1().nestMethod();
    }

    public static class Nest1 {
        private final int nestVariable = 1;

        public void nestMethod() throws Exception {
            Nest2 nest2 = new Nest2();
            /**
             * jdk8 can run successfully
             */
            nest2.nestVariable = nestVariable;
            Field nestVariable2 = Nest2.class.getDeclaredField("nestVariable");
            /**
             * run failed with jdk8 but can run successfully with jdk11
             * Error: A JNI error has occurred, please check your installation and try again
             */
            nestVariable2.setInt(nest2, 0);
            System.out.println(nest2.nestVariable);
        }
    }

    public static class Nest2 {
        private int nestVariable = 2;
    }
}
