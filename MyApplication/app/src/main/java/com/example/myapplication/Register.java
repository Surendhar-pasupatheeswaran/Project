package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    private EditText phoneNumberInput;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        Button savePhoneNumberButton = findViewById(R.id.savePhoneNumberButton);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Save phone number to Firebase and SharedPreferences
        savePhoneNumberButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberInput.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                savePhoneNumberToFirebase(phoneNumber);
                savePhoneNumberToSharedPreferences(phoneNumber);
                goToMainActivity();
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePhoneNumberToFirebase(String phoneNumber) {
        databaseReference.child("phoneNumber").setValue(phoneNumber)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Register.this, "Phone number saved to Firebase", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Register.this, "Failed to save phone number to Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void savePhoneNumberToSharedPreferences(String phoneNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phoneNumber", phoneNumber);
        editor.apply();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(Register.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
