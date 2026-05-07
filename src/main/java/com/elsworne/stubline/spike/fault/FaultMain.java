package com.elsworne.stubline.spike.fault;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/** Runs a local echo server or times a request through Toxiproxy. */
public final class FaultMain {
  private FaultMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0) throw new IllegalArgumentException("server or client URL");
    if (args[0].equals("server")) {
      int port = args.length > 1 ? Integer.parseInt(args[1]) : 18082;
      var server =
          HttpServer.create(
              new InetSocketAddress(
                  System.getenv().getOrDefault("SPIKE_BIND_ADDRESS", "127.0.0.1"), port),
              0);
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.createContext(
          "/echo",
          exchange -> {
            try (exchange) {
              byte[] body = "x".repeat(65536).getBytes(java.nio.charset.StandardCharsets.UTF_8);
              exchange.sendResponseHeaders(200, body.length);
              exchange.getResponseBody().write(body);
            }
          });
      server.start();
      System.out.println("echo listening=" + port);
      new CountDownLatch(1).await();
    } else if (args[0].equals("client")) {
      URI url = URI.create(args.length > 1 ? args[1] : "http://127.0.0.1:18666/echo");
      long started = System.nanoTime();
      try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
        var response =
            client.send(
                HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(3)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        System.out.printf(
            "status=%d bytes=%d elapsedMs=%d%n",
            response.statusCode(),
            response.body().length,
            Duration.ofNanos(System.nanoTime() - started).toMillis());
      } catch (Exception e) {
        System.out.printf(
            "exception=%s elapsedMs=%d%n",
            e.getClass().getSimpleName(), Duration.ofNanos(System.nanoTime() - started).toMillis());
      }
    } else throw new IllegalArgumentException("server or client URL");
  }
}
