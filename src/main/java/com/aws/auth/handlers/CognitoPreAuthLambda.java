package com.aws.auth.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CognitoPreAuthLambda implements RequestStreamHandler {

    ObjectMapper mapper = new ObjectMapper();
    LambdaLogger logger = null;

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        logger = context.getLogger();
        try {
            JsonNode mainNode = mapper.readTree(inputStream);
            logger.log("EVENT: "+ mainNode.toString());
            // Get the registered attributes and signing attributes
            String registeredPhoneNumber = mainNode.get("request").get("userAttributes").get("phone_number").asText();
            String registeredMPIN = mainNode.get("request").get("userAttributes").get("custom:mpin").asText();
            String signingPhoneNumber = mainNode.get("request").get("validationData").get("phone_number").asText();
            String signingMPIN = mainNode.get("request").get("validationData").get("mpin").asText();

            // Match the registered attributes with signin attributes
            if(registeredPhoneNumber.equals(signingPhoneNumber)
                    && registeredMPIN.equals(signingMPIN)) {
                logger.log("ATTRIBUTES MATCHED");
                mapper.writeValue(outputStream, mainNode);
            } else {
                logger.log("ATTRIBUTES DID NOT MATCH");
                throw new IOException("Phone number and MPIN did not match");
            }
        } catch (JsonProcessingException e) {
            logger.log("Error executing lambda: "+ e.toString());
        }
    }
}
