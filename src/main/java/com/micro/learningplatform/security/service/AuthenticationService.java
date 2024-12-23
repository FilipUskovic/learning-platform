package com.micro.learningplatform.security.service;

import com.micro.learningplatform.security.UserRole;
import com.micro.learningplatform.security.dto.*;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(String refreshToken);

    void addRoleToUser(String email, UserRole role);

    void removeRoleFromUser(String email, UserRole role);

    AuthenticationResponseWithRoles registerUserWithRoles(RegisterWithRolesRequest request);

    AuthenticationResponse registerInstructor(RegisterRequest request);

    AuthenticationResponse registerAdmin(RegisterRequest request);

    String registerWithoutdToken(RegisterRequest request);

    AuthenticationResponse authenticateOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User);



}
