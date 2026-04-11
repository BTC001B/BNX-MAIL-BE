package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class AcceptInviteRequest {
    private String inviteToken;
    private String username;
    private String password;
}
