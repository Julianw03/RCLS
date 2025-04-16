package com.julianw03.rcls.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class Utils {
    private Utils() {}

    public static Optional<String> inputStreamToString(InputStream is) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(result.toString());
    }

    public static Optional<JsonNode> parseJson(ObjectMapper mapper, String string) {
        if (mapper == null || string == null) return Optional.empty();
        try {
            return Optional.ofNullable(mapper.readTree(string));
        } catch (Exception e) {
//            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<JsonNode> parseJson(ObjectMapper mapper, InputStream stream) {
        if (mapper == null || stream == null) return Optional.empty();
        try {
            return Optional.ofNullable(mapper.readTree(stream));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static <T> Optional<T> parseJson(ObjectMapper mapper, JsonNode node, Class<T> tClass) {
        if (mapper == null || node == null || tClass == null) return Optional.empty();
        if (JsonNode.class.equals(tClass)) return Optional.of((T) node);
        try {
            return Optional.ofNullable(mapper.convertValue(node, tClass));
        } catch (Exception e) {
//            log.error("", e);
            return Optional.empty();
        }
    }

    public static Integer getFreePort() {
        try {
            // 0 to get port assigned from OS
            ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));

            int assignedPort = serverSocket.getLocalPort();

            serverSocket.close();
            return assignedPort;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonNode readValue(ObjectMapper mapper, String val) {
        if (mapper == null || val == null) throw new IllegalArgumentException();
        try {
            return mapper.readTree(val);
        } catch (JsonProcessingException e) {}
        return NullNode.getInstance();
    }

    public static  <T> T wrapSecure(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Error in secure wrapper", e);
            return null;
        }
    }

    public static void wrapSecure(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Error in secure wrapper", e);
        }
    }
}
