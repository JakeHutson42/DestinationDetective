package com.example.destinationdetective;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.destinationdetective.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.snackbar.Snackbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class RegisterActivity extends AppCompatActivity {

        private EditText editTextEmail, editTextPassword, editTextUsername;
        private CoordinatorLayout coordinatorLayout;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            editTextEmail = findViewById(R.id.editTextEmail);
            editTextPassword = findViewById(R.id.editTextPassword);
            coordinatorLayout = findViewById(R.id.coordinatorLayout);

            Button buttonRegister = findViewById(R.id.buttonRegister);
            buttonRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    registerUser();
                }
            });

            TextView textViewRegister = findViewById(R.id.textViewRegister);
            textViewRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                }
            });
        }

        private void registerUser() {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Registration successful, navigate to main activity
                                startActivity(new Intent(RegisterActivity.this, MapsActivity.class));
                                finish();
                            } else {
                                // Login failed, display a message to the user in the form of a Snackbar.
                                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Unsuitable Username or Password", Snackbar.LENGTH_LONG);
                                snackbar.show();
                            }
                        }
                    });
        }

    }
