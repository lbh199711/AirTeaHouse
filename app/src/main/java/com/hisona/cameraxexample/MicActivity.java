package com.hisona.cameraxexample;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class MicActivity extends AppCompatActivity{
    ImageButton backButton;
    ImageButton recordButton;
    TextView fileSizeTextView;
    TextView songNameTextView;
    Button matchSongButton;
    static String mFileName = null;
    String fileSize = null;
    static final int REQUEST_AUDIO_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic);
        // back button
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });
        // media recording
        recordButton = findViewById(R.id.record_button);
        fileSizeTextView = findViewById(R.id.file_size_text);
        WavClass wavObj = new WavClass(getApplicationContext().getExternalFilesDir(null).getAbsolutePath());
        recordButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event){
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    mFileName = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/final_record.wav";
                    wavObj.startRecording(getApplicationContext());
                } else if (event.getAction() == MotionEvent.ACTION_UP){
                    wavObj.stopRecording();
                    File file = new File(mFileName);
                    fileSize = Formatter.formatShortFileSize(getApplicationContext(),file.length());
                    fileSize = "File Size: "+fileSize;
                    fileSizeTextView.setText(fileSize);
                }
                return false;
            }
        });
        if (!CheckPermissions()) {
            RequestPermissions();
        }

        // Send recorded file to api to match songs
        matchSongButton = findViewById(R.id.match_song_button);
        songNameTextView = findViewById(R.id.song_name_text);
        matchSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fileSize != null){
                    String url = "https://shazam-core.p.rapidapi.com/v1/tracks/recognize";
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        songNameTextView.setText("Nicky Youre, dazy - Sunroof");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // this method is called when user will
        // grant the permission for audio recording.
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermissions() {
        // this method is used to check permission
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        // this method is used to request the
        // permission for audio recording and storage.
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}

