package com.omnip.shared.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Custom Docker client strategy for Docker Desktop 4.38+ on macOS.
 *
 * Docker Desktop 4.38+ only accepts API version >= 1.46 on the Unix socket;
 * docker-java's default (1.41) returns HTTP 400.
 *
 * Configure via ~/.testcontainers.properties:
 *   docker.client.strategy=com.omnip.shared.testcontainers.DockerDesktopModernStrategy
 */
public class DockerDesktopModernStrategy extends DockerClientProviderStrategy {

    private static final String API_VERSION = "1.47";
    private final URI dockerHost = URI.create("unix://" + detectDockerSocket());

    @Override
    public String getDescription() {
        return "Docker Desktop macOS (API v" + API_VERSION + ", socket: " + dockerHost + ")";
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder()
                .dockerHost(dockerHost)
                .build();
    }

    /**
     * Override to return a DockerClient with explicit API version 1.47.
     * DockerClientProviderStrategy.test() calls this to validate connectivity.
     */
    @Override
    public DockerClient getDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost.toString())
                .withApiVersion(API_VERSION)
                .build();

        ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(dockerHost)
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    private static String detectDockerSocket() {
        String[] candidates = {
            System.getProperty("user.home") + "/.docker/run/docker.sock",
            "/var/run/docker.sock"
        };
        for (String path : candidates) {
            if (Files.exists(Path.of(path))) {
                return path;
            }
        }
        throw new InvalidConfigurationException("No Docker socket found. Is Docker Desktop running?");
    }
}
