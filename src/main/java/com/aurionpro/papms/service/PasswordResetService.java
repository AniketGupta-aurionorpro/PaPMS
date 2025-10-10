package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.ForgotPasswordRequest;
import com.aurionpro.papms.dto.ResetPasswordRequest;

public interface PasswordResetService {

    void handleForgotPasswordRequest(ForgotPasswordRequest request);

    void handleResetPassword(ResetPasswordRequest request);
}