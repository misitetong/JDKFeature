package com.misitetong.jdk11;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author zyy
 * @Date 2025/10/21 21:12
 * @Description <a href="https://openjdk.org/jeps/332">com.misitetong.jdk11.JEP332</a>
 */
public class JEP332 {

    public static void main(String[] args) throws Exception {
        // 指定TLSv1.3
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, null, null);

        SSLSocketFactory factory = sslContext.getSocketFactory();

        // 连接到支持 TLS 1.3 的服务器
        try (SSLSocket socket = (SSLSocket) factory.createSocket("www.google.com", 443)) {
            // 可以启用所有支持的协议，也可以只启用 TLSv1.3
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});
            // 发起握手
            socket.startHandshake();
            System.out.println("Protocol: " + socket.getSession().getProtocol());
            System.out.println("Cipher suite: " + socket.getSession().getCipherSuite());

            // 可选：发送 HTTPS 请求获取响应
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            System.out.println("First line of response: " + reader.readLine());
        }
    }
}
