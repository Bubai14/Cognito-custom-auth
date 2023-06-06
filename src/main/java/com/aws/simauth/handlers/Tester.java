package com.aws.simauth.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Tester {

    public static void main(String[] args) throws JsonProcessingException {
        Tester tester = new Tester();
        String token = "Bearer "+ tester.getAccessToken();
        System.out.println("TOKEN: "+token);
        Map<String, String> map = tester.subscriberCheck(token, "+919830540773");
        String url = map.get("check_url");
        String checkId = map.get("check_id");
        String code = tester.redirect(token, url);
        System.out.println("CODE: "+code);
        Map<String, String> completedMap = tester.completePhoneCheck(token, checkId, code);
        System.out.println("MATCH: "+completedMap.get("match"));
    }

    private String getAccessToken() {
        String urlEncodeParamsBuilder = "grant_type" + "=" +
                URLEncoder.encode("client_credentials", StandardCharsets.UTF_8) +
                "&" + "scope" + "=" +
                URLEncoder.encode("phone_check", StandardCharsets.UTF_8);
         HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://IN.api.tru.id/oauth2/v1/token"))
                .header("Authorization", "Basic Y2NhMDFlZDItOTFhNS00OTY4LTk2ZjgtNWIwMGVlMGEzM2JmOngyWmwwaFouUm1JRmFJNHlOUlJBVXZVNm5O")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(urlEncodeParamsBuilder))
                .build();
        String token = "";
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            HashMap responseMap = mapper.readValue(response.body(), HashMap.class);
            token = responseMap.get("access_token").toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return token;
    }

    private Map<String, String> subscriberCheck(String token, String phoneNumber) throws JsonProcessingException {
        Random random = new Random();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("redirect_url", "");
        node.put("phone_number", phoneNumber);
        node.put("reference_id", String.valueOf(random.nextLong()));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        System.out.println("PARAMS: "+ json);
         HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://IN.api.tru.id/phone_check/v0.2/checks"))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        Map<String, String> phoneCheckMapping = new HashMap<>();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response: "+ response.body());
            JsonNode jsonNode = mapper.readTree(response.body());
            phoneCheckMapping.put("check_id", jsonNode.get("check_id").asText());
            phoneCheckMapping.put("status", jsonNode.get("status").asText());
            phoneCheckMapping.put("check_url", jsonNode.get("_links").get("check_url").get("href").asText());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return phoneCheckMapping;
    }

    private String redirect(String token, String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", token)
                .GET()
                .build();
        String code = "";
        try {
             HttpResponse<String> response = HttpClient.newBuilder()
                     .followRedirects(HttpClient.Redirect.ALWAYS)
                     .build()
                     .send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Redirect Response: "+ response.body());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.body());
            code = jsonNode.get("code").asText();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return code;
    }

    private Map<String, String> completePhoneCheck(String token, String checkId, String code) {
        Map<String, String> completedCheckMap = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("op", "add");
            node.put("path", "/code");
            node.put("value", code);
            String json = "["+mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)+"]";
            System.out.println("PARAMS: "+ json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://in.api.tru.id/phone_check/v0.2/checks/" + checkId))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json-patch+json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("COMPLETE RESPONSE: "+ response.body());
            JsonNode jsonNode = mapper.readTree(response.body());
            completedCheckMap.put("status", jsonNode.get("status").asText());
            completedCheckMap.put("match", jsonNode.get("match").asText());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return completedCheckMap;
    }
}
