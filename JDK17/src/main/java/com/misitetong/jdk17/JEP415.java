package com.misitetong.jdk17;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author zyy
 * @Date 2025/12/14 23:36
 * @Description <a href="https://openjdk.org/jeps/415">com.misitetong.jdk17.JEP415</a>
 */
public class JEP415 {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        List<String> list = Arrays.asList("a", "b", "c");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(list);
        }

        // 反序列化失败
//        Object obj = deserialize(bos.toByteArray(), "!*");
//        System.out.println("Deserialized: " + obj);
        // 序列化成功
        Object obj = deserialize(bos.toByteArray(), "java.base/*;java.util.ArrayList;!*");
        System.out.println("Deserialized: " + obj);
    }

    public static Object deserialize(byte[] data, String pattern) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            // 为当前流设置上下文过滤器
            ois.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter(pattern)
            );
            return ois.readObject();
        }
    }
}
