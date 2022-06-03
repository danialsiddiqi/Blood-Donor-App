package com.example.blooddonorapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class DonorRegistrationActivity extends AppCompatActivity {

    private TextView backButton;

    private CircleImageView profile_image;

    private TextInputEditText registerFullName,registerIdNumber,registerPhoneNumber,
            registerEmail,registerPassword;

    private Spinner bloodGroupSSpinner;

    private Button registerButton;

    private Uri resultUri;

    private ProgressDialog loader;

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_registration);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DonorRegistrationActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });

        profile_image= findViewById(R.id.profile_image);
        registerFullName=findViewById(R.id.registerFullName);
        registerIdNumber=findViewById(R.id.registerIdNumber);
        registerPhoneNumber=findViewById(R.id.registerPhoneNumber);
        registerEmail=findViewById(R.id.registerEmail);
        registerPassword=findViewById(R.id.registerPassword);
        bloodGroupSSpinner= findViewById(R.id.bloodGroupSpinner);
        registerButton= findViewById(R.id.registerButton);
        loader = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = registerEmail.getText().toString().trim();
                final String password= registerPassword.getText().toString().trim();
                final String fullName = registerFullName.getText().toString().trim() ;
                final String idNumber = registerIdNumber.getText().toString().trim() ;
                final String phoneNumber = registerPhoneNumber.getText().toString().trim();
                final String bloodGroup = bloodGroupSSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(email)){
                    registerEmail.setError("Email is required!");
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    registerPassword.setError("Password is required!");
                    return;
                }

                if (TextUtils.isEmpty(fullName)){
                    registerFullName.setError("Full name is required!");
                    return;
                }

                if (TextUtils.isEmpty(idNumber)){
                    registerIdNumber.setError("Id number is required!");
                    return;
                }

                if (TextUtils.isEmpty(phoneNumber)){
                    registerPhoneNumber.setError("Phone number is required!");
                    return;
                }

                if (bloodGroup.equals("Select your blood group")){
                    Toast.makeText(DonorRegistrationActivity.this,"Select Blood group",Toast.LENGTH_SHORT).show();
                    return;
                }

                else {
                    loader.setMessage("Registering you...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(DonorRegistrationActivity.this,"Error"+error,Toast.LENGTH_SHORT).show();
                            }
                            else {
                                String currentUserId = mAuth.getCurrentUser().getUid();
                                userDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserId);
                                HashMap<String, Object> userinfo = new HashMap<String, Object>();
                                userinfo.put("id",currentUserId);
                                userinfo.put("name",fullName);
                                userinfo.put("email",email);
                                userinfo.put("idnumber",idNumber);
                                userinfo.put("phonenumber",phoneNumber);
                                userinfo.put("bloodgroup",bloodGroup);
                                userinfo.put("type","donor");
                                userinfo.put("search","donor"+bloodGroup);

                                userDatabaseRef.updateChildren(userinfo).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()){
                                        Toast.makeText(DonorRegistrationActivity.this,"Data set successfully",Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(DonorRegistrationActivity.this, task1.getException().toString(),Toast.LENGTH_SHORT).show();
                                    }

                                    finish();
                                   // loader.dismiss();
                                });

                                if(resultUri != null){
                                    final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile image").child(currentUserId);
                                    Bitmap bitmap = null;

                                    try {
                                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG,30,byteArrayOutputStream);
                                    byte[] data = byteArrayOutputStream.toByteArray();
                                    UploadTask uploadTask = filePath.putBytes(data);

                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DonorRegistrationActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            if (taskSnapshot.getMetadata() !=null && taskSnapshot.getMetadata().getReference() != null){
                                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String imageUrl = uri.toString();
                                                        Map<String, Object> newImageMap = new HashMap<>();
                                                        newImageMap.put("profilepictureurl",imageUrl);

                                                        userDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                                            @Override
                                                            public void onComplete(@NonNull Task task) {
                                                                if(task.isSuccessful()){
                                                                    Toast.makeText(DonorRegistrationActivity.this, "Image url added to database successfullt", Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(DonorRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });

                                                        finish();
                                                    }
                                                });
                                            }
                                        }
                                    });

                                    Intent intent = new Intent(DonorRegistrationActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    loader.dismiss();
                                }

                            }
                        }
                    });

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}