package com.augustanasi.camerasensorapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("deprecation")

/**
 *This class starts the devices camera and when prompted by the Control App, takes a picture and sends it back to the Control App.
 * Requires the device has a back-facing camera.
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback rawCallBack;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback pngCallBack;
    Context context;
    Button takeImg;
    Button startBtn;
    Button socketListen;
    static File storageDir;
    SocketConnection socketConnection;


    int port = 5678;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
  
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            new AlertDialog.Builder(this)
                    .setTitle("Required Camera")
                    .setMessage("This app requires the device to have a back facing camera")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
        socketListen = (Button)findViewById(R.id.listen);
        socketListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        boolean cameraOpen = true;
                        startCamera();
                        Log.d("camera started", "sensor is listening");
                        try{
                            Log.d("starting connection", "attempting to create a socket connect with port: " + port);
                            socketConnection = new SocketConnection(port);
                            Log.d("socket created", "socket connection: " + socketConnection.toString());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        try{
                            Log.d("wait for prompt", "will call waitForPrompt:");
                            int currentInput = socketConnection.waitForPrompt();
                            boolean stopped = false;
                             while(currentInput!=-1){
                                 System.out.println("Current Input = "+currentInput);
                                if(currentInput==0){
                                    System.out.println("Stop Camera");
                                    cameraOpen = false;
                                    camera.stopPreview();
                                    stopped = true;
                                }else{
                                    if(stopped){
                                        System.out.println("Restart Camera");
                                        cameraOpen = true;
                                        camera.startPreview();
                                    }
                                    captureImage();
                                    Log.d("Image","Started Image Capture");

                                }
                                currentInput = socketConnection.waitForPrompt();
                            }

                            Log.d("Socket","Exited While Loop");

                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        if(cameraOpen){
                            stopCamera();
                        }
                        socketConnection.closeSocket();
                    }
                });

            }
        });

        storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AugustanaRobotImgs");
        if(!storageDir.exists()){
            storageDir.mkdir();
        }




        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        rawCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Log","onPictureTaken - raw");
            }
        };

        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.i("Log","onshutter'd");
            }
        };
        pngCallBack = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("HERE","HERE");
                FileOutputStream outputStream;

                String state = Environment.getExternalStorageState();
                Log.d("State", "State - "+state);
                Log.d("LOCATION","Storage Location: "+storageDir.exists());
                Log.d("LOCATION","Storage Location: "+storageDir.toString());
                File image = new File(storageDir, "image.png");
                Log.d("Location","Image - "+image.exists());

                try{
                    outputStream = new FileOutputStream(image);
                    outputStream.write(data);
                    outputStream.close();
                    Log.d("Log", "onPictureTaken - wrote bytes: "+data.length);
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                } catch(IOException e){
                    e.printStackTrace();
                } finally {
                }
                Log.d("Log", "onPictureTaken - png");
                camera.stopPreview();
                camera.startPreview();
                try{
                    socketConnection.sendImage(image);
                }catch(IOException e){
                    e.printStackTrace();
                }


            }
        };
    }


    public void captureImage(){
        camera.takePicture(shutterCallback,rawCallBack,pngCallBack);
    }


    private void startCamera(){
        if(Camera.getNumberOfCameras()>0){
            camera = Camera.open();
        }else{
            return;
        }

        Camera.Parameters param;
        param = camera.getParameters();

        param.setPreviewFrameRate(40);
        param.setPreviewSize(surfaceView.getWidth(),surfaceView.getHeight());
        camera.setParameters(param);
        camera.setDisplayOrientation(90);

        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }catch (Exception e){
            Log.e("MainActivity","init_camera: "+e);
        }
        Log.d("CAMERA","Started");
    }

    public void stopCamera(){
        camera.stopPreview();
        camera.release();
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

