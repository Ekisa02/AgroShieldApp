package com.Joseph.agroshieldapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private TextView loginText;
    private MaterialButton registerButton, googleSignUpButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private ProgressDialog progressDialog;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        googleSignUpButton = findViewById(R.id.googleSignUpButton);
        loginText = findViewById(R.id.loginText);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        loginText.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        registerButton.setOnClickListener(v -> registerUser());
        googleSignUpButton.setOnClickListener(v -> signInWithGoogle());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // from Firebase settings
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    try {
                        if (task.isSuccessful()) {
                            String userId = firebaseAuth.getCurrentUser().getUid();
                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", userId);
                            userMap.put("name", name);
                            userMap.put("email", email);

                            userRef.child(userId).setValue(userMap)
                                    .addOnCompleteListener(saveTask -> {
                                        try {
                                            if (saveTask.isSuccessful()) {
                                                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                                                firebaseAuth.signOut();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Failed to save user info", Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        } finally {
                                            progressDialog.dismiss();
                                        }
                                    });
                        } else {
                            throw task.getException();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                });
    }

    private void signInWithGoogle() {
        progressDialog.show();
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    try {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", user.getUid());
                            userMap.put("name", user.getDisplayName());
                            userMap.put("email", user.getEmail());

                            userRef.child(user.getUid()).setValue(userMap)
                                    .addOnCompleteListener(saveTask -> {
                                        try {
                                            if (saveTask.isSuccessful()) {
                                                Toast.makeText(this, "Signed in with Google", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Failed to save user info", Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        } finally {
                                            progressDialog.dismiss();
                                        }
                                    });
                        } else {
                            throw task.getException();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Authentication Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                });
    }
}
