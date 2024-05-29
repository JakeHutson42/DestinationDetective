package com.example.destinationdetective;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.destinationdetective.MapsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.snackbar.Snackbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;


public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private CheckBox checkBoxSaveLogin;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;
    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxSaveLogin = findViewById(R.id.saveLogin);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        TextView textViewRegister = findViewById(R.id.textViewRegister);
        //Initialise the coordinatorlayout for Snackbars.
        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();

        boolean savedLogin = sharedPreferences.getBoolean("savedLogin", false);
        if (savedLogin) {
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                editTextEmail.setText(savedEmail);
                editTextPassword.setText(savedPassword);
                checkBoxSaveLogin.setChecked(true);
            }
        }

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void loginUser() {
        final String email = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString().trim();

        if (checkBoxSaveLogin.isChecked()) {
            saveLoginInformation(email, password);
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login successful, navigate to main activity
                            startActivity(new Intent(LoginActivity.this, MapsActivity.class));
                            finish();
                        } else {
                            // Login failed, display a message to the user in the form of a Snackbar
                            Snackbar snackbar = Snackbar.make(coordinatorLayout, "Incorrect Username or Password", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }
                });
    }

    //Handle saving a users entered information for when the application is next started up
    private void saveLoginInformation(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("savedLogin", true);
        editor.putString("email", email);
        editor.putString("password", password);
        editor.apply();
    }
}
