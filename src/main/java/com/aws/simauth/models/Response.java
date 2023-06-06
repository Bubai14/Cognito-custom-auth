package com.aws.simauth.models;

import lombok.Data;

@Data
public class Response {

    private boolean autoConfirmUser;
    private boolean autoVerifyPhone;
    private boolean autoVerifyEmail;
}
