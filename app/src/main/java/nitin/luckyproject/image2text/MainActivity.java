package nitin.luckyproject.image2text;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE =200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE =1000;
    private static final int IMAGE_PICK_CAMERA_CODE =1001;

    EditText mResult;
    ImageView mPreview;
    Button mStart,mStop;

    TextToSpeech mTTs;
    String[] cameraPermission;
    String[] storagePermission;
    Uri image_uri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar =getSupportActionBar();
        actionBar.setSubtitle("CLick + button to insert Image");

        mResult =findViewById(R.id.result);
        mPreview =findViewById(R.id.imagepreview);

        mStart =findViewById(R.id.speak_btn);
        mStop =findViewById(R.id.stop_btn);

        cameraPermission =new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission =new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        mTTs =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                //if no error set language
                if(i != TextToSpeech.ERROR){
                    mTTs.setLanguage(Locale.US);
                }
                else{
                    Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get text from EditText
                String tospeak =mResult.getText().toString().trim();
                if(tospeak.equals("")){
                    Toast.makeText(MainActivity.this, "Empty", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, tospeak, Toast.LENGTH_SHORT).show();
                    //speak the text
                    mTTs.speak(tospeak,TextToSpeech.QUEUE_FLUSH,null);
                }

            }
        });

        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTTs.isSpeaking()){
                    mTTs.stop();
                    Toast.makeText(MainActivity.this, "Stopped!!!", Toast.LENGTH_SHORT).show();

                }
                else{
                    Toast.makeText(MainActivity.this, "Not Speaking", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    //action bar menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id= item.getItemId();
        if(id==R.id.addimage){

            showImageImportDialog();
        }
        if(id==R.id.settings){

        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        //item to display in dialog
        String[] items ={" Camera", "Gallery"};
        AlertDialog.Builder dialog =new AlertDialog.Builder(this);

        //set text
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if(i==0){

                    //camera options clicked
                    //here we have to ask for permission for camera
                    if(!checkCameraPermission()){
                        //camera permission note allowed,request it
                        requestCameraPermission();
                    }
                    else{
                        //permission allowed,take puctures
                        pickCamera();
                    }

                }
                if(i==1){

                    //gallery options clicked
                    if(!checkStoragePermission()){
                        //camera permission note allowed,request it
                        requestStoragePermission();
                    }
                    else{
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show(); //show dialog
    }


    private void pickGallery() {
    //intent to pick image from galley
     Intent intent =new Intent(Intent.ACTION_PICK);
     intent.setType("image/*");
     startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickCamera() {
        //intent to take image from camera,it will also save to storage to get high quality image
        ContentValues values =new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Pic"); //title of picture
        values.put(MediaStore.Images.Media.DESCRIPTION,"IMAGE2TEXT"); //descrpition

        image_uri =getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraintent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraintent,IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);

    }

    private boolean checkStoragePermission() {
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        //check camera permission and return the result
        /*
        in order to get high quality image we have to save image to external storage first before inserting to image view
        that's why storage permission are also required
         */
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    //handle permission result


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length >0){
                    boolean cameraAccepteed = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted =grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepteed && writeStorageAccepted)
                    {
                        pickCamera();
                    }
                    else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case STORAGE_REQUEST_CODE:

                boolean writeStorageAccepted =grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                if( writeStorageAccepted)
                {
                    pickGallery();
                }
                else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

//handle image result


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //got image from camera
        if(resultCode ==RESULT_OK){
            if(requestCode ==IMAGE_PICK_GALLERY_CODE){
                //crop the image which come from gallery
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);//enable image guidelines
            }
            if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //crop image which come from camera
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);//enable image guidelines

            }
        }
        //get cropped image
        if(requestCode ==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK){
                Uri resultUri = result.getUri(); //get image uri
                //set image to image view
                mPreview.setImageURI(resultUri);

                //get drawable bitmap
                BitmapDrawable bitmapDrawable = (BitmapDrawable)mPreview.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer =new TextRecognizer.Builder(getApplicationContext()).build();

                if(!recognizer.isOperational()){
                    Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
                }
                else{
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();

                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb =new StringBuilder();
                    //get text from sb until there is no texr
                    for(int i=0;i<items.size();i++){
                        TextBlock myitem =items.valueAt(i);
                        sb.append(myitem.getValue());
                        sb.append("\n");
                    }

                    mResult.setText(sb.toString());
                }
            }
            else if(resultCode ==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception eror =result.getError();
                Toast.makeText(this, ""+eror, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        if(mTTs !=null || mTTs.isSpeaking()){
            mTTs.stop();

        }
        super.onPause();
    }
}
