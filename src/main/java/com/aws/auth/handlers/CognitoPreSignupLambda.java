package com.aws.auth.handlers;

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

public class CognitoPreSignupLambda implements RequestStreamHandler {
    ObjectMapper mapper = new ObjectMapper();
    LambdaLogger logger = null;

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        logger = context.getLogger();
        try {
            JsonNode mainNode = mapper.readTree(inputStream);
            logger.log("EVENT: "+ mainNode.toString());
            JsonNode responseNode = mainNode.get("response");
            // auto confirm the user on sign up
            ((ObjectNode) responseNode).put("autoConfirmUser", true);
            ((ObjectNode) responseNode).put("autoVerifyPhone", true);
            logger.log("output "+mainNode.toString());
            mapper.writeValue(outputStream, mainNode);
        } catch (JsonProcessingException e) {
            logger.log("Error executing lambda: "+ e.toString());
        } catch (IOException e) {
            logger.log("Error executing lambda: "+ e.toString());
        }
    }
}
