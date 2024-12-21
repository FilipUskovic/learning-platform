package com.micro.learningplatform.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseWithRoles {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String roles;

}
