package com.misitetong.jdk11;

/**
 * @author zyy
 * @Date 2025/10/8 19:46
 * @Description <a href="https://openjdk.org/jeps/181">JEP181</a>
 * @Tip combine this class with jdk8 to find method: "static synthetic access$000"
 * @Tip idea's plugin Asm Bytecode Viewer will help you a lot to view .class file
 */
public class JEP181 {
    public static class Nest1 {
        private final int nestVariable = 1;

        public void nestMethod() {
            Nest2  nest2 = new Nest2();
            System.out.println(nest2.nestVariable);
        }
    }

    public static class Nest2 {
        /**
         * if variable "nestVariable" be declared with final, you will not find method: "static synthetic access$000"
         * because it has been optimized by jvm combine. So you need to remove final
         */
//        private final int nestVariable = 2;
        private int nestVariable = 2;
    }
}
