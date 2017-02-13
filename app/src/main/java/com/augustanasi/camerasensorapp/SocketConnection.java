package com.augustanasi.camerasensorapp;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by viola on 2/7/2017.
 */

public class SocketConnection {

    private Socket client;
    private ServerSocket server;
    private File img;
    private BufferedReader br;
    private DataOutputStream dos;


    public SocketConnection(int portNum) throws Exception{
        server = new ServerSocket(portNum);
        server.setSoTimeout(0);
        Log.d("Socket","Waiting for connection");
        client = server.accept();
        Log.d("Socket","Connected");
        dos = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
    }

    public int waitForPrompt() throws IOException{
        String str = br.readLine();
        if(str.equals(null)){
            str = br.readLine();
        }
        if(str.equalsIgnoreCase("pic")){
            Log.d("Socket","Read In "+str);
            return 1;
        }
        if(str.equalsIgnoreCase("stop")){
            return 0;
        }
        if(str.equalsIgnoreCase("closing")){
            return -5;
        }
        Log.d("Socket","Read In "+str);
        return -1;
    }


    public void sendImage(File img) throws IOException{
               if(img==null){
                   Log.d("Socket","File = null");
               }else{
                   Log.d("Socket", "File not NULL");
               }
                dos.writeInt((int)img.length());
                Log.d("Socket",""+img.length());
               dos.flush();

                byte[] fileBytes = new byte[(int)img.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(img));
                bis.read(fileBytes, 0, fileBytes.length);
                Log.d("Socket","Sending "+img.toString()+" ("+fileBytes.length+" bytes)");
                dos.write(fileBytes);
                Log.d("Socket","Sent Message: Complete");
                dos.flush();
    }

    public void closeSocket(){
        try{
            client.close();
            server.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
