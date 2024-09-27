package com.auth.utils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.material.login.LoginAuth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class LogInUtils {

    public interface ResultCallback {
        void onLogInSuccess(FirebaseUser user);

        default void onLogInFailure(Exception e) {
        }

        default void onResetEmailSent() {
        }

        default void onVerificationEmailSent() {
        }
    }


    private final FirebaseAuth firebaseAuth;
    private final Activity context;

    private OnCompleteListener<AuthResult> onCompleteListener;
    private static final List<ResultCallback> callbacks = new ArrayList<>();
    public long lastEmailSent = 0;
    public long lastForgotPasswordEmailSent = 0;

    public LogInUtils(Activity context) {
        this.context = context;
        firebaseAuth = FirebaseAuth.getInstance();
        onCompleteListener = task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = task.getResult().getUser();
                if (!user.isEmailVerified())
                    firebaseAuth.signOut();
                if (!callbacks.isEmpty())
                    for (Iterator<ResultCallback> iterator = callbacks.iterator();iterator.hasNext();) {
                        ResultCallback callback = iterator.next();
                        callback.onLogInSuccess(user);
                        iterator.remove();
                    }
            } else {
                if (!callbacks.isEmpty())
                    callbacks.get(0).onLogInFailure(task.getException());
            }
        };
    }

    public void logIn(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(context, onCompleteListener);
    }

    public void signUp(TabLayout loginSelector, String uName, String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(context,
                        task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = task.getResult().getUser();
                                UserProfileChangeRequest profileUpdates =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(uName)
                                                .build();
                                user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                                    loginSelector.selectTab(loginSelector.getTabAt(0));
                                    if (!callbacks.isEmpty())
                                        for (Iterator<ResultCallback> iterator = callbacks.iterator();iterator.hasNext();) {
                                            ResultCallback callback = iterator.next();
                                            callback.onLogInSuccess(user);
                                            iterator.remove();
                                        }
                                });
                            } else {
                                if (!callbacks.isEmpty())
                                    callbacks.get(0).onLogInFailure(task.getException());
                            }
                        });
    }

    public void logInGoogle() {
        CredentialManager credentialManager = CredentialManager.create(context);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(com.cab.app.R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .setNonce(getRandomNonce())
                .build();
        GetCredentialRequest credentialRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
        credentialManager.getCredential(context, credentialRequest, new Continuation<GetCredentialResponse>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                if (o instanceof Result.Failure) {
                    if (!callbacks.isEmpty())
                        callbacks.get(0).onLogInFailure(new Exception(((Result.Failure) o).exception));
                    return;
                }
                GetCredentialResponse response = ((GetCredentialResponse) o);
                handleSignIn(response.getCredential());
            }
        });
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof PublicKeyCredential) {

        } else if (credential instanceof PasswordCredential) {

        } else if (credential instanceof CustomCredential) {
            // GoogleIdToken credentialt
            if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.getData());
                AuthCredential authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.getIdToken(), null);
                firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(onCompleteListener);
            }
        }
    }

    public void signInTwitter() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");
        provider.addCustomParameter("oauth_nonce", getRandomNonce());
        Task<AuthResult> pendingResultTask = firebaseAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask.addOnCompleteListener(onCompleteListener);
        } else {
            firebaseAuth
                    .startActivityForSignInWithProvider(context, provider.build())
                    .addOnCompleteListener(onCompleteListener);
        }
    }

    public void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!callbacks.isEmpty()) {
                            callbacks.get(0).onVerificationEmailSent();
                            lastEmailSent = System.currentTimeMillis();
                        }
                    } else {
                        if (!callbacks.isEmpty())
                            callbacks.get(0).onLogInFailure(task.getException());
                    }
                });
    }

    public void sendPasswordRestEmail(LoginAuth authHandler, String email) {
        if (authHandler.isNotFormat(false, false)) return;
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!callbacks.isEmpty())
                            callbacks.get(0).onResetEmailSent();
                        lastForgotPasswordEmailSent = System.currentTimeMillis();
                    } else {
                        if (!callbacks.isEmpty())
                            callbacks.get(0).onLogInFailure(task.getException());
                    }
                });
    }

    public String getRandomNonce() {
        try {
            String uid = UUID.randomUUID().toString();
            byte[] bytes = uid.getBytes();
            MessageDigest md = null;

            md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return getHexValue(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHexValue(byte[] byteArray) {
        String hex = "";

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex += String.format("%02X", i);
        }

        return hex;
    }

    public static void addResultCallback(ResultCallback listener) {
        callbacks.add(listener);
    }

    public static void removeResultCallback(ResultCallback listener) {
        callbacks.remove(listener);
    }
}
