package com.av.avmessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class setting extends AppCompatActivity {

    ImageView setprofile;
    EditText setname, setstatus;
    Button donebut;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    String email, password;
    Uri setImageUri;
    StorageReference storageReference;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize storageReference and reference
        storageReference = storage.getReference().child("upload").child(auth.getUid());
        reference = database.getReference().child("user").child(auth.getUid());

        setprofile = findViewById(R.id.settingprofile);
        setname = findViewById(R.id.settingname);
        setstatus = findViewById(R.id.settingstatus);
        donebut = findViewById(R.id.donebutt);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                email = snapshot.child("mail").getValue().toString();
                password = snapshot.child("password").getValue().toString();
                String name = snapshot.child("userName").getValue().toString();
                String profile = snapshot.child("profilepic").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                setname.setText(name);
                setstatus.setText(status);
                Picasso.get().load(profile).into(setprofile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi nếu có (ví dụ: hiển thị thông báo lỗi)
            }
        });

        setprofile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10);
        });

        donebut.setOnClickListener(v -> {
            String name = setname.getText().toString();
            String status = setstatus.getText().toString();

            if (setImageUri != null) {
                // New image selected
                storageReference.putFile(setImageUri).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            updateUserData(uri.toString(), name, status);
                        });
                    } else {
                        Toast.makeText(setting.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // No new image, use existing profile picture
                reference.child("profilepic").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String existingProfileUrl = snapshot.getValue(String.class);
                        updateUserData(existingProfileUrl, name, status);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(setting.this, "Error fetching profile image.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image Source");
        builder.setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
            if (which == 0) {
                // Chọn từ thư viện ảnh
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), 10);
            } else {
                // Chụp ảnh mới
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // ... (Thêm logic để lưu ảnh chụp vào file tạm thời và set output cho cameraIntent) ...
                startActivityForResult(cameraIntent, 11); // 11 là requestCode cho camera
            }
        });
        builder.show();
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, mở trình chọn ảnh hoặc camera
                showImagePickerDialog();
            } else {
                // Quyền bị từ chối, xử lý tương ứng (ví dụ: hiển thị thông báo)
                Toast.makeText(setting.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ... (các phương thức khác: updateUserData, onActivityResult, ...) ...



    private void updateUserData(String finalImageUri, String name, String status) {
        Users users = new Users(auth.getUid(), name, email, password, finalImageUri, status);
        reference.setValue(users).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(setting.this, "Data Is Saved", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(setting.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(setting.this, "Something went wrong...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (data != null) {
                setImageUri = data.getData();
                setprofile.setImageURI(setImageUri);
            }
        }
    }
}
