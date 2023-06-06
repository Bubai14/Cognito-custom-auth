package com.aws.simauth.models;

import lombok.Data;

@Data
public class SignUpEvent {

    private Request request;
    private Response response;
}
