package com.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.auth.utils.LogInUtils;
import com.cab.app.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.material.login.LoginAuth;
import com.material.login.R;
import com.material.login.SignInListener;

public class LoginActivity extends AppCompatActivity implements LogInUtils.ResultCallback {
    private final String TAG = "LoginActivity";
    private View backBtn;
    private View forgotPass;
    private User currentUser;
    private LogInUtils logInUtils;
    private FirebaseDatabase userDb;
    private LoginAuth authHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_MaterialLogin);
        setContentView(R.layout.login_page);
        backBtn = findViewById(R.id.backBtn);
        authHandler = new LoginAuth(this);
        authHandler.getLoginSelector();

        logInUtils = new LogInUtils(this);
        LogInUtils.addResultCallback(this);

        userDb = FirebaseDatabase.getInstance();
        authHandler.setOnTabSelectedListener(null);/*set null for default*/
        authHandler.setSignInListener(new SignInListener() {
            @Override
            public void onLogin(String email, String password) {
                //logic for login (old user)
                authHandler.getLoginButton().setEnabled(false);//disable button till result
                logInUtils.logIn(email, password);
            }

            @Override
            public void onSignUp(String uName, String email, String password) {
                //logic for signUp (new usew)
                authHandler.getLoginButton().setEnabled(false);//disable button till result
                logInUtils.signUp(authHandler.getLoginSelector(), uName, email, password);
            }

            @Override
            public void onGoogleLogin() {
                authHandler.getGoogleButton().setEnabled(false);//disable button till result
                //logic for google api
                logInUtils.logInGoogle();
            }

            @Override
            public void onTwitterLogin() {
                authHandler.getTwitterBtn().setEnabled(false);//disable button till result
                //logic for twitter api
                logInUtils.signInTwitter();
            }

            @Override
            public void onForgetPassword(String email) {
                //logic for forgot password
                authHandler.getForgetPasswordView().setEnabled(true);//disable button till result
                AlertDialog dialog = new MaterialAlertDialogBuilder(LoginActivity.this)
                        .setCancelable(true)
                        .setTitle("Forgot Password!")
                        .setMessage(getString(com.cab.app.R.string.forgot_password_message))
                        .setPositiveButton("Ok", (dialogInterface, i) -> {

                        })
                        .setNegativeButton("Send", (dialogInterface, i) -> logInUtils.sendPasswordRestEmail(authHandler, email)).create();
                dialog.show();
                long timePassed = System.currentTimeMillis() - logInUtils.lastForgotPasswordEmailSent;
                if (1000 * 60 > timePassed /*less then 1 minute passed*/) {
                    Button negButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    negButton.setEnabled(false);
                    negButton.setText("ReSend in " + (60000 - timePassed) / 1000 + "s");
                }

            }
        });
        backBtn.setOnClickListener(view -> {
            //close app
            finishAffinity();
        });
    }


    @Override
    public void onLogInSuccess(FirebaseUser user) {
        Runnable pendingRunnable = () -> {
            authHandler.getLoginButton().setEnabled(true);
            authHandler.getGoogleButton().setEnabled(true);
            authHandler.getTwitterBtn().setEnabled(true);
            if (!user.isEmailVerified()) {
                String userName = user.getDisplayName() != null ? user.getDisplayName() : "User";
                AlertDialog dialog = new MaterialAlertDialogBuilder(LoginActivity.this)
                        .setCancelable(true)
                        .setTitle("Email verification!")
                        .setMessage("Hi " + userName +
                                getString(com.cab.app.R.string.email_verification_message))
                        .setPositiveButton("Ok", (dialogInterface, i) -> {

                        })
                        .setNegativeButton("Send", (dialogInterface, i) -> logInUtils.sendVerificationEmail(user)).create();
                dialog.show();
                long timePassed = System.currentTimeMillis() - logInUtils.lastEmailSent;
                if (1000 * 60 > timePassed /*less then 1 minute passed*/) {
                    Button negButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    negButton.setEnabled(false);
                    negButton.setText("ReSend in " + (60000 - timePassed) / 1000 + "s");
                }

                return;
            }
            //update user to the db
            User updatedUser = new User(user.getDisplayName(), user.getEmail(), String.valueOf(user.getPhotoUrl()), UserType.PASSENGER);
            if (currentUser != null)
                updatedUser.setUserType(currentUser.getUserType());
            DatabaseReference userRef = userDb.getReference("users").child(user.getUid());
            userRef.updateChildren(updatedUser.toMap());
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
            finish();
        };
        if (!User.getInstance().isUpdatedFromDb()) {
            User.getInstance().updateFromDatabase().addOnSuccessListener(snapshot -> {
                currentUser = User.getInstance();
                pendingRunnable.run();
            });
        } else{
            currentUser = User.getInstance();
            pendingRunnable.run();
        }
    }

    @Override
    public void onLogInFailure(Exception e) {
        authHandler.getLoginButton().setEnabled(true);//enable button after result
        authHandler.getGoogleButton().setEnabled(true);
        authHandler.getTwitterBtn().setEnabled(true);
        String errMsg = e.getMessage();
        if (e instanceof FirebaseAuthWeakPasswordException)
            errMsg = "Weak passward";
        else if (e instanceof FirebaseAuthUserCollisionException)
            errMsg = "User already exists";
        else if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException)
            errMsg = "Invalid Credentials entered";
        else if (e.getMessage().startsWith("androidx.credentials.exceptions.GetCredentialCancellationException"))
            return;//account picker canceled
        Toast.makeText(LoginActivity.this, "Authentication failed : " + e.getClass(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "onLogInFailure: ", e);
    }

    @Override
    public void onResetEmailSent() {
        authHandler.getForgetPasswordView().setEnabled(true);//enable button after result
        Toast.makeText(LoginActivity.this, "Password reset email sent to your Email.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVerificationEmailSent() {
        authHandler.getLoginButton().setEnabled(true);
        Toast.makeText(LoginActivity.this, "Verification email set Successfully.", Toast.LENGTH_SHORT).show();
    }
}
