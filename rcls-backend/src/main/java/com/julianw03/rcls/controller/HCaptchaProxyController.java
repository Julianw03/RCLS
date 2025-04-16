package com.julianw03.rcls.controller;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping(value = "/hcaptcha-proxy", consumes = {MimeTypeUtils.APPLICATION_JSON_VALUE, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE})
public class HCaptchaProxyController {

    private static final String                                                                                     SUBDOMAIN_NONE       = "RCLS-INTERNAL";
    private static final String                                                                                     RIOT_MAGIC_USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) riot-client-ux/100.0.0 Chrome/108.0.5359.215 Electron/22.3.27 Safari/537.36";
    private static final HashMap<String, BiFunction<RequestObject, ResponseEntity<byte[]>, ResponseEntity<byte[]>>> handlerMap           = new HashMap<>();
    private static final Pattern                                                                                    pattern              = Pattern.compile("((\\w{1,20})\\.)hcaptcha\\.com"); //For the sake of performance we limit the amount of chars in the subdomain

    @Getter
    protected static class RequestObject {
        private final String              subdomain;
        private final String              originalSubdomain;
        private final String              path;
        private final Map<String, String> query;
        private final String regexSubstitution;

        public RequestObject(
                String subdomain,
                String path,
                Map<String, String> query,
                String regexSubstitution
        ) {
            this.originalSubdomain = subdomain;
            this.subdomain = SUBDOMAIN_NONE.equals(subdomain) ? null : subdomain;
            this.path = path;
            this.query = query;
            this.regexSubstitution = regexSubstitution;
        }

        public String getExternalUrl(String baseUrl) {
            StringBuilder sb = new StringBuilder();
            sb.append("https://");
            if (subdomain != null) {
                sb.append(subdomain).append('.');
            }
            sb.append(baseUrl);
            if (path != null) {
                sb.append(path);
            }
            if (query != null) {
                sb.append('?');
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    sb.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
                }
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    static {
        handlerMap.put(SUBDOMAIN_NONE, (originalRequest, originalResponse) -> {
            if (originalResponse.getBody() != null) {
                if (originalRequest.getPath().endsWith("api.js")) {
                    String body = readStream(new ByteArrayInputStream(originalResponse.getBody()));
                    String newResp = pattern.matcher(body).replaceAll(originalRequest.getRegexSubstitution());

                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(originalResponse.getHeaders());
                    headers.remove(HttpHeaders.CONTENT_LENGTH);

                    return new ResponseEntity<>(
                            newResp.getBytes(StandardCharsets.UTF_8),
                            headers,
                            originalResponse.getStatusCode()
                    );
                }
            }

            return originalResponse;
        });
        handlerMap.put("newassets", (originalRequest, originalResponse) -> {
            if (originalResponse.getBody() != null) {
                if (originalRequest.getPath().endsWith("hsw.js")) {
                    String body = readStream(new ByteArrayInputStream(originalResponse.getBody()));
                    String newResp = pattern.matcher(body).replaceAll(originalRequest.getRegexSubstitution());
                    newResp = newResp.replaceAll("<meta http-equiv=\"Content-Security-Policy\"[^>]+>", "");

                    byte[] modifiedBody = newResp.getBytes(StandardCharsets.UTF_8);

                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(originalResponse.getHeaders());
                    headers.remove(HttpHeaders.CONTENT_LENGTH);

                    return new ResponseEntity<>(
                            modifiedBody,
                            headers,
                            originalResponse.getStatusCode()
                    );
                } else if (originalRequest.getPath().endsWith("hcaptcha.html")) {
                    String body = readStream(new ByteArrayInputStream(originalResponse.getBody()));
                    String newResp = pattern.matcher(body).replaceAll(originalRequest.getRegexSubstitution());
                    newResp = newResp.replaceAll("<meta http-equiv=\"Content-Security-Policy\"[^>]+>", "");

                    byte[] modifiedBody = newResp.getBytes(StandardCharsets.UTF_8);

                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(originalResponse.getHeaders());
                    headers.remove(HttpHeaders.CONTENT_LENGTH);

                    return new ResponseEntity<>(
                            modifiedBody,
                            headers,
                            originalResponse.getStatusCode()
                    );
                }

                return new ResponseEntity<>(
                        originalResponse.getBody(),
                        originalResponse.getHeaders(),
                        originalResponse.getStatusCode()
                );
            }

            return originalResponse;
        });
    }

    @Value("${proxy.target.url}")
    private String targetUrl;

    @Value("${server.port}")
    private String port;

    private String regexSubstitution;

    private final RestTemplate restTemplate;


    @PostConstruct
    private void init() {
        this.regexSubstitution = "127.0.0.1:" + port + "/hcaptcha-proxy/$2";
    }

    public HCaptchaProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("Proxy controller up and running. Will use \"{}\" as external User-Agent", RIOT_MAGIC_USERAGENT);
    }

    @RequestMapping(path = "/{subdomain}/{*path}", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH,
            RequestMethod.OPTIONS
    })
    public ResponseEntity<byte[]> proxyRequest(
            @PathVariable String subdomain,
            @PathVariable String path,
            @RequestParam Map<String, String> query,
            @RequestHeader HttpHeaders originalHeaders,
            @RequestBody(required = false) byte[] body,
            HttpMethod method
    ) {


        RequestObject requestObject = new RequestObject(
                subdomain,
                path,
                query,
                regexSubstitution
        );

        String requestUrl = requestObject.getExternalUrl(targetUrl);

        log.info("Proxying from /{}{} to {}", subdomain, path, requestUrl);

        URI uri = URI.create(requestUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(originalHeaders);
        headers.remove(HttpHeaders.HOST);
        headers.put(HttpHeaders.USER_AGENT, Collections.singletonList(RIOT_MAGIC_USERAGENT));
        headers.put(HttpHeaders.ACCEPT_ENCODING, Collections.emptyList());

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, method, entity, byte[].class);

            ResponseEntity<byte[]> intermediateResponse = handlerMap
                    .getOrDefault(requestObject.getOriginalSubdomain(), (p, resp) -> resp)
                    .apply(requestObject, response);

            HttpHeaders returnHeaders = new HttpHeaders();
            returnHeaders.putAll(intermediateResponse.getHeaders());

            ResponseEntity<byte[]> returnResponse = new ResponseEntity<>(
                    intermediateResponse.getBody(),
                    returnHeaders,
                    intermediateResponse.getStatusCode()
            );

            return returnResponse;
        } catch (HttpClientErrorException e) {
            log.warn("Error while trying to access {} {}", method, uri);
            try {
                return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
            } catch (Exception ex) {}
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private static String readStream(InputStream is) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = is.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }

            return result.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
