package com.material.login;

public interface SignInListener {
    void onLogin(String email, String password);

    void onSignUp(String uName, String email, String password);
    void onGoogleLogin();
    void onTwitterLogin();
    void onForgetPassword(String email);
}
