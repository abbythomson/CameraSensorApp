package com.augustanasi.camerasensorapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@SuppressWarnings("deprecation")

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback rawCallBack;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback pngCallBack;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        rawCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {

            }
        };

        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {

            }
        };

        pngCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outputStream;
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AppPics");
                File image = new File(storageDir, "image.png");

                try {
                    outputStream = new FileOutputStream(image);
                    outputStream.write(data);
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }
            }
        };

        startCamera();
        int port = 5678;
        try {
            ServerSocket server = new ServerSocket(port);
            server.setSoTimeout(0);
            Socket client = server.accept();

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"AppPics");
            File image = new File(storageDir,"image.png");

            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String str = br.readLine();

            while(str!=null){
                captureImage();
                byte[] fileBytes = new byte[(int)image.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(image));
                bis.read(fileBytes, 0, fileBytes.length);
                OutputStream os = client.getOutputStream();
                os.write(fileBytes,0,fileBytes.length);
                os.flush();
                str=br.readLine();
            }
            client.close();
            stopCamera();

        }catch (IOException e){

        }
    }

    private void captureImage(){
        camera.takePicture(shutterCallback,rawCallBack,pngCallBack);
        stopCamera();
        startCamera();
    }

    private void startCamera(){
        if(Camera.getNumberOfCameras()>0){
            if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
                camera = Camera.open(0);
            }else {
                camera = Camera.open();
            }
        }else{
            return;
        }

        Camera.Parameters param;
        param = camera.getParameters();

        param.setPreviewFrameRate(40);
        param.setPreviewSize(surfaceView.getWidth(),surfaceView.getHeight());
        camera.setParameters(param);

        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }catch (Exception e){
            return;
        }
    }

    private void stopCamera(){
        camera.stopPreview();
        camera.release();
        surfaceView.clearFocus();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
