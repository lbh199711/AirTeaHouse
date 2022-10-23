package com.hisona.cameraxexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int RGBA_COMPRESSION_RATE = 25;
    private static final int TOP_N_COUNT = 8;

    ConstraintLayout container;
    ImageButton camera_capture_button;
    ImageButton mic_activity_button;
    PreviewView view_finder;
    Executor executor;

    TextView text_color_1_view;
    TextView text_color_2_view;
    TextView text_color_3_view;
    TextView text_color_4_view;
    TextView text_color_5_view;
    TextView text_color_6_view;
    TextView text_color_7_view;
    TextView text_color_8_view;

    private long mLastAnalysisResultTime;
    Map<String, Integer> hm = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        container = findViewById(R.id.camera_container);
        text_color_1_view = findViewById(R.id.text_color_1);
        text_color_2_view = findViewById(R.id.text_color_2);
        text_color_3_view = findViewById(R.id.text_color_3);
        text_color_4_view = findViewById(R.id.text_color_4);
        text_color_5_view = findViewById(R.id.text_color_5);
        text_color_6_view = findViewById(R.id.text_color_6);
        text_color_7_view = findViewById(R.id.text_color_7);
        text_color_8_view = findViewById(R.id.text_color_8);
        camera_capture_button = findViewById(R.id.camera_capture_button);
        view_finder = findViewById(R.id.view_finder);

        executor = Executors.newSingleThreadExecutor();

        camera_capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] rgbaHexArray = new String[TOP_N_COUNT];

                List<Map.Entry<String, Integer>> mostCommonRgba = findGreatest(hm, TOP_N_COUNT);
                for (int i = 0; i < mostCommonRgba.size(); i++){
                    String[] rgbaStrArrayFrag = mostCommonRgba.get(i).getKey().split("\\|",4);
                    String[] rgbaHexArrayFrag = new String[rgbaStrArrayFrag.length];
                    for (int j = 0; j < rgbaStrArrayFrag.length; j++) {
                        String hexString = Integer.toHexString(
                                Integer.parseInt(rgbaStrArrayFrag[j])*RGBA_COMPRESSION_RATE
                        );
                        if (hexString.length() == 1){
                            // pad zero make sure color coding is always 6 digits
                            hexString = "0"+hexString;
                        }
                        rgbaHexArrayFrag[j] = hexString;
                    }
                    rgbaHexArray[i] = String.join("", rgbaHexArrayFrag);
                }

                text_color_1_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[0]));
                text_color_2_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[1]));
                text_color_3_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[2]));
                text_color_4_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[3]));
                text_color_5_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[4]));
                text_color_6_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[5]));
                text_color_7_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[6]));
                text_color_8_view.setBackgroundColor(Color.parseColor("#"+rgbaHexArray[7]));

                hm = new HashMap<>();

            }
        });

        if(checkPermission()) {
            startCamera();
        }

        // to camera view
        mic_activity_button = findViewById(R.id.mic_activity_button);
        mic_activity_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent switchActivityIntent = new Intent(MainActivity.this, MicActivity.class);
                startActivity(switchActivityIntent);
            }
        });

    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't use image classification example without granting CAMERA permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                startCamera();
            }
        }
    }

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        executor = Executors.newSingleThreadExecutor();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(224, 224))
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
//                int rotationDegrees = image.getImageInfo().getRotationDegrees();

                // color calculation
                if (image.getFormat() == PixelFormat.RGBA_8888){
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();

                    for (int x = 0; x < 244; x++)
                        for (int y = 0; y < 244; y++){
                            int i = (x + (244 * y)) * 4;
                            int r = buffer.get(i) & 0xff;
                            int g = buffer.get(i+1) & 0xff;
                            int b = buffer.get(i+2) & 0xff;
                            int a = buffer.get(i+3) & 0xff; // this is always FA
                            String rgbaStr = Integer.toString(r/RGBA_COMPRESSION_RATE) +'|'
                                    + g/RGBA_COMPRESSION_RATE +'|'
                                    + b/RGBA_COMPRESSION_RATE;
                            int count = hm.getOrDefault(rgbaStr, 0);
                            hm.put(rgbaStr, ++count);
                        }
                }
                image.close();
            }
        });

        cameraProvider.unbindAll();

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this,
                cameraSelector, imageAnalysis, preview);

        preview.setSurfaceProvider(view_finder.getSurfaceProvider());

    }

    private static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>>
    findGreatest(Map<K, V> map, int n)
    {
        Comparator<? super Map.Entry<K, V>> comparator =
                (Comparator<Map.Entry<K, V>>) (e0, e1) -> {
                    V v0 = e0.getValue();
                    V v1 = e1.getValue();
                    return v0.compareTo(v1);
                };
        PriorityQueue<Map.Entry<K, V>> highest =
                new PriorityQueue<Map.Entry<K,V>>(n, comparator);
        for (Map.Entry<K, V> entry : map.entrySet())
        {
            highest.offer(entry);
            while (highest.size() > n)
            {
                highest.poll();
            }
        }

        List<Map.Entry<K, V>> result = new ArrayList<Map.Entry<K,V>>();
        while (highest.size() > 0)
        {
            result.add(highest.poll());
        }
        return result;
    }

}