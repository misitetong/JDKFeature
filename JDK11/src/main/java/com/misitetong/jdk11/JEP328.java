package com.misitetong.jdk11;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * @author zyy
 * @Date 2025/10/19 15:33
 * @Description <a href="https://openjdk.org/jeps/328">com.misitetong.jdk11.JEP328</a>
 */
public class JEP328 {

    @Name("com.misitetong.jdk11.JEP328.MyEvent")
    @Label("My Custom Event")
    static class MyEvent extends Event {
        @Label("Message")
        String message = "";
    }

    public static void main(String[] args) throws Exception {
        MyEvent event = new MyEvent();
        event.message = "Hello JFR";
        event.begin();
        Thread.sleep(100);
        event.end();
        // 提交事件
        event.commit();
        System.out.println(event.message);
    }
}
