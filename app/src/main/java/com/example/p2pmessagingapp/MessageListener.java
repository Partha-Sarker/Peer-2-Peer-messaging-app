//package com.example.p2pmessagingapp;
//
//import android.util.Log;
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//public class MessageListener extends Thread{
//    int port;
//    ServerSocket serverSocket;
//    WritableGUI gui;
//
//    public MessageListener(int port, WritableGUI gui) {
//        this.port = port;
//        this.gui = gui;
//        try {
//            serverSocket = new ServerSocket(port);
//            Log.d("ahtrap", "Listening on port: "+port);
//        } catch (IOException e) {
//            Log.d("ahtrap", "Can't create server socket");
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void run() {
//        Socket clientSocket;
//        boolean connected = false;
//        Log.d("ahtrap", "Waiting for client");
//        try {
//            while((clientSocket = serverSocket.accept()) != null){
//                if(!connected){
//                    Log.d("ahtrap", "Connected to client");
//                    connected = true;
//                }
//                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
//                String message = dis.readUTF();
//                gui.write(message);
//            }
//        }catch (IOException e){
//            Log.d("ahtrap","error occured while reading data");
//        }
//    }
//}
