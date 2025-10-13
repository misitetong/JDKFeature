package com.misitetong.jdk11;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * @author zyy
 * @Date 2025/10/13 21:22
 * @Description <a href="https://openjdk.org/jeps/321">JEP321</a>
 */
public class JEP321 {

    public static void syncRequest() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .header("User-Agent", "Java11 HttpClient")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("sync request end with status: " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static class AsyncStringBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {

        private final CompletableFuture<T> result = new CompletableFuture<>();
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            for (ByteBuffer buffer : items) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                builder.append(new String(bytes));
            }
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            result.complete(JSON.parseObject(builder.toString(), new TypeReference<T>() {
            }));
        }

        @Override
        public CompletionStage<T> getBody() {
            return result;
        }
    }

    public static void asyncRequest() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .header("User-Agent", "Java11 HttpClient")
                .GET()
                .build();
        System.out.println("request start at: " + LocalDateTime.now());
        // simple use
//        CompletableFuture<HttpResponse<String>> future =
//                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        // use custom Subscriber
        HttpResponse.BodySubscriber<String> subscriber = new AsyncStringBodySubscriber<>();
        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, HttpResponse.BodyHandlers.fromSubscriber(
                        subscriber,
                        stringBodySubscriber -> stringBodySubscriber.getBody().toString()
                ));

        System.out.println("request has sent, waiting response at: " + LocalDateTime.now());

        future.thenApply(HttpResponse::statusCode)
                .thenAccept( statusCode -> {
                    System.out.println("status: " + statusCode);
                    System.out.println("request end at: " + LocalDateTime.now());
                })
                .join();
    }

    public static void main(String[] args) {
        syncRequest();
        asyncRequest();
    }
}
