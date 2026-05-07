package com.elsworne.stubline.spike.fault;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("docker")
@EnabledIfSystemProperty(named = "run.docker", matches = "true")
class ToxiproxyDockerTest {
  @Test
  void latencyDelaysAnOtherwiseSuccessfulResponse() throws Exception {
    try (var fixture = new Fixture()) {
      fixture.toxic("latency", "{\"latency\":800,\"jitter\":0}");
      long start = System.nanoTime();
      assertEquals(200, fixture.get(Duration.ofSeconds(3)).statusCode());
      assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 700);
    }
  }

  @Test
  void bandwidthLimitsTransferRate() throws Exception {
    try (var fixture = new Fixture()) {
      fixture.toxic("bandwidth", "{\"rate\":16}");
      long start = System.nanoTime();
      assertEquals(65536, fixture.get(Duration.ofSeconds(8)).body().length);
      assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 3000);
    }
  }

  @Test
  void timeoutIsObservedByTheClient() throws Exception {
    try (var fixture = new Fixture()) {
      fixture.toxic("timeout", "{\"timeout\":0}");
      assertThrows(
          java.net.http.HttpTimeoutException.class, () -> fixture.get(Duration.ofSeconds(2)));
    }
  }

  @Test
  void resetIsObservedAsConnectionFailure() throws Exception {
    try (var fixture = new Fixture()) {
      fixture.toxic("reset_peer", "{\"timeout\":0}");
      assertThrows(IOException.class, () -> fixture.get(Duration.ofSeconds(3)));
    }
  }

  private static final class Fixture implements AutoCloseable {
    private final HttpServer origin;
    private final ToxiproxyContainer proxy;
    private final HttpClient client = HttpClient.newHttpClient();
    private final URI api;

    Fixture() throws Exception {
      origin = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
      origin.createContext(
          "/echo",
          exchange -> {
            try (exchange) {
              byte[] body = new byte[65536];
              exchange.sendResponseHeaders(200, body.length);
              exchange.getResponseBody().write(body);
            }
          });
      origin.start();
      Testcontainers.exposeHostPorts(origin.getAddress().getPort());
      proxy =
          new ToxiproxyContainer(
              DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.13.0")
                  .asCompatibleSubstituteFor("shopify/toxiproxy"));
      proxy.start();
      api = URI.create("http://" + proxy.getHost() + ":" + proxy.getMappedPort(8474));
      post(
          "/proxies",
          "{\"name\":\"echo\",\"listen\":\"0.0.0.0:8666\",\"upstream\":\"host.testcontainers.internal:"
              + origin.getAddress().getPort()
              + "\"}",
          201);
    }

    void toxic(String type, String attributes) throws Exception {
      post(
          "/proxies/echo/toxics",
          "{\"name\":\"fault\",\"type\":\""
              + type
              + "\",\"stream\":\"downstream\",\"attributes\":"
              + attributes
              + "}",
          200);
    }

    HttpResponse<byte[]> get(Duration timeout) throws Exception {
      URI url = URI.create("http://" + proxy.getHost() + ":" + proxy.getMappedPort(8666) + "/echo");
      return client.send(
          HttpRequest.newBuilder(url).timeout(timeout).GET().build(),
          HttpResponse.BodyHandlers.ofByteArray());
    }

    private void post(String path, String body, int expected) throws Exception {
      var response =
          client.send(
              HttpRequest.newBuilder(api.resolve(path))
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString(body))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(expected, response.statusCode(), response.body());
    }

    @Override
    public void close() {
      proxy.stop();
      origin.stop(0);
      client.close();
    }
  }
}
