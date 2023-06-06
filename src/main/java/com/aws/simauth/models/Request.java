package com.aws.simauth.models;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Request {

    private Map<String, String> userAttributes = new HashMap<>();
    private Map<String, String> validationData = new HashMap<>();
    private Map<String, String> clientMetadata = new HashMap<>();
}
