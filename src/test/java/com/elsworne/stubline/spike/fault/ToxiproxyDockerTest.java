package com.elsworne.stubline.spike.fault;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("docker")
@EnabledIfSystemProperty(named = "run.docker", matches = "true")
class ToxiproxyDockerTest {
  @Test
  void toxiproxyApiCreatesLatencyToxic() throws Exception {
    try (var proxy =
            new ToxiproxyContainer(
                DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.13.0")
                    .asCompatibleSubstituteFor("shopify/toxiproxy"));
        var http = HttpClient.newHttpClient()) {
      proxy.start();
      URI api = URI.create("http://" + proxy.getHost() + ":" + proxy.getMappedPort(8474));
      var created =
          http.send(
              HttpRequest.newBuilder(api.resolve("/proxies"))
                  .header("Content-Type", "application/json")
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "{\"name\":\"echo\",\"listen\":\"0.0.0.0:8666\",\"upstream\":\"127.0.0.1:8080\"}"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(201, created.statusCode());
      var toxic =
          http.send(
              HttpRequest.newBuilder(api.resolve("/proxies/echo/toxics"))
                  .header("Content-Type", "application/json")
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "{\"name\":\"fault\",\"type\":\"latency\",\"stream\":\"downstream\",\"attributes\":{\"latency\":100}}"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, toxic.statusCode());
    }
  }
}
