package org.gustavotbett;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final S3Client s3Client = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        String pathParamenters = stringObjectMap.get("rawPath").toString();
        String shortUrlCode = pathParamenters.replace("/", "");

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid short URL code");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("gustavotbett-url-shortener")
                .key(shortUrlCode + ".json")
                .build();

        InputStream object;

        try {
            object = s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error getting data from S3: " + e.getMessage(), e);
        }

        UrlData urlData;

        try {
            urlData = objectMapper.readValue(object, UrlData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing data from S3: " + e.getMessage(), e);
        }

        long currentTime = System.currentTimeMillis() / 1000;
        Map<String, Object> response = new HashMap<>();

        if (urlData.getExpirationTime() < currentTime) {
            response.put("statusCode", 410);
            response.put("body", "Url expired");

            return response;
        }

        response.put("statusCode", 302);
        response.put("headers", Map.of("Location", urlData.getOriginalUrl()));

        return response;
    }
}