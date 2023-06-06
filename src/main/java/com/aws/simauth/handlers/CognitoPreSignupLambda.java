package com.aws.simauth.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CognitoPreSignupLambda implements RequestStreamHandler {

    ObjectMapper mapper = new ObjectMapper();
    LambdaLogger logger = null;

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        logger = context.getLogger();
        try {
            JsonNode mainNode = mapper.readTree(inputStream);
            logger.log("EVENT: "+ mainNode.toString());
            JsonNode responseNode = mainNode.get("response");
            // Get the access token
            String token = "Bearer "+ getAccessToken();
            logger.log("TOKEN: "+ token);
            // Initiate the phone check workflow
            Map<String, String> phoneCheckResponse = phoneCheck(token,
                    mainNode.get("request").get("userAttributes").get("phone_number").asText());
            if(phoneCheckResponse.get("status").equals("ACCEPTED")) {
                logger.log("PHONE CHECK STATUS: ACCEPTED");
                // Invoke the redirect url
                String code = redirect(token, phoneCheckResponse.get("check_url"));
                // Complete the phone check
                Map<String, String> completedResponse = completePhoneCheck(token, phoneCheckResponse.get("check_id"), code);
                logger.log("status: "+completedResponse.get("status")+" | match: "+Boolean.valueOf(completedResponse.get("match")));
                if(completedResponse.get("status").equals("COMPLETED")
                        && Boolean.valueOf(completedResponse.get("match"))) {
                    logger.log("PHONE CHECK MATCHED");
                    ((ObjectNode) responseNode).put("autoConfirmUser", true);
                    ((ObjectNode) responseNode).put("autoVerifyPhone", true);
                } else {
                    logger.log("PHONE CHECK DID NOT MATCH");
                    ((ObjectNode) responseNode).put("autoConfirmUser", false);
                    ((ObjectNode) responseNode).put("autoVerifyPhone", false);
                }
            } else {
                logger.log("PHONE CHECK REQUEST WAS NOT ACCEPTED");
                ((ObjectNode) responseNode).put("autoConfirmUser", false);
                ((ObjectNode) responseNode).put("autoVerifyPhone", false);
            }
            logger.log("output "+mainNode.toString());
            mapper.writeValue(outputStream, mainNode);
        } catch (JsonProcessingException e) {
            logger.log("Error executing lambda: "+ e.toString());
        } catch (IOException e) {
            logger.log("Error executing lambda: "+ e.toString());
        }
    }

    private String getAccessToken() {
        StringBuilder urlEncodeParamsBuilder = new StringBuilder();
        urlEncodeParamsBuilder.append("grant_type").append("=")
                .append(URLEncoder.encode("client_credentials",  StandardCharsets.UTF_8))
                .append("&").append("scope").append("=")
                .append(URLEncoder.encode("phone_check",  StandardCharsets.UTF_8));
        logger.log("Creating the token call");
        String credential = System.getenv("CREDENTIAL");
        logger.log("CREDENTIAL: "+ credential);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://IN.api.tru.id/oauth2/v1/token"))
                .header("Authorization", "Basic "+ credential)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(urlEncodeParamsBuilder.toString()))
                .build();
        String token = "";
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            HashMap responseMap = mapper.readValue(response.body(), HashMap.class);
            token = responseMap.get("access_token").toString();
        } catch (IOException e) {
            logger.log("Error executing getAccessToken: "+ e.toString());
        } catch (InterruptedException e) {
            logger.log("Error executing getAccessToken: "+ e.toString());
        }
        return token;
    }

    private Map<String, String> phoneCheck(String token, String phoneNumber) {
        Map<String, String> phoneCheckMapping = new HashMap<>();
        try {
            Random random = new Random();
            ObjectNode node = mapper.createObjectNode();
            node.put("redirect_url", "");
            node.put("phone_number", phoneNumber);
            node.put("reference_id", String.valueOf(random.nextLong()));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            System.out.println("PARAMS: "+ json);
            logger.log("PARAMS for phone_check: "+json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://IN.api.tru.id/phone_check/v0.2/checks"))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("PHONE CHECK RESPONSE: "+response.body());
            JsonNode jsonNode = mapper.readTree(response.body());
            phoneCheckMapping.put("check_id", jsonNode.get("check_id").asText());
            phoneCheckMapping.put("status", jsonNode.get("status").asText());
            phoneCheckMapping.put("check_url", jsonNode.get("_links").get("check_url").get("href").asText());
        } catch (IOException e) {
            logger.log("Error executing phoneCheck: "+ e.toString());
        } catch (InterruptedException e) {
            logger.log("Error executing phoneCheck: "+ e.toString());
        }
        return phoneCheckMapping;
    }

    private String redirect(String token, String url) {
        logger.log("REDIRECT URL: "+url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .GET()
                .build();
        String code = "";
        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("REDIRECT RESPONSE: "+response.body());
            JsonNode jsonNode = mapper.readTree(response.body());
            code = jsonNode.get("code").asText();
        } catch (IOException e) {
            logger.log("Error executing redirect: "+ e.toString());
        } catch (InterruptedException e) {
            logger.log("Error executing redirect: "+ e.toString());
        }
        return code;
    }

    private Map<String, String> completePhoneCheck(String token, String checkId, String code) {
        Map<String, String> completedCheckMap = new HashMap<>();
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("op", "add");
            node.put("path", "/code");
            node.put("value", code);
            String json = "["+mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)+"]";
            logger.log("PARAMS: "+ json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://in.api.tru.id/phone_check/v0.2/checks/" + checkId))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json-patch+json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("COMPLETE RESPONSE: "+response.body());
            JsonNode jsonNode = mapper.readTree(response.body());
            completedCheckMap.put("status", jsonNode.get("status").asText());
            completedCheckMap.put("match", jsonNode.get("match").asText());
        } catch (IOException e) {
            logger.log("Error executing completePhoneCheck: "+ e.toString());
        } catch (InterruptedException e) {
            logger.log("Error executing completePhoneCheck: "+ e.toString());
        }
        return completedCheckMap;
    }
}
