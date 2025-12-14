package com.misitetong.jdk17;

import java.io.*;
import java.util.*;

/**
 * @author zyy
 * @Date 2025/12/14 23:46
 * @Description NONE
 */
public class JEP415 {

    public static class Data implements Serializable {
        private final String value;
        public Data(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(list);
        }

        Data data = new Data("a");
        ByteArrayOutputStream dataBos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(dataBos)) {
            oos.writeObject(data);
        }

        String pattern = "java.base/*;java.util.ArrayList;!*";
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(pattern);
        ObjectInputFilter.Config.setSerialFilter(filter);

        // 反序列化失败
        Object obj1 = deserialize(bos.toByteArray());
        System.out.println("Deserialized: " + obj1);

        // 序列化成功
        Object obj2 = deserialize(dataBos.toByteArray());
        System.out.println("Deserialized: " + obj2);
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }
}
