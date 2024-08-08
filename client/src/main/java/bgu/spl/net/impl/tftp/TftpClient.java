package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) throws IOException {
        Thread keyboard;
        Thread listening;
        final Socket sock=new Socket("127.0.0.1", 7777);
        ClientTftpProtocol protocol=new ClientTftpProtocol();
        TftpClientEncoderDecoder encDec=new TftpClientEncoderDecoder();
        Keyboard first=new Keyboard(sock,protocol,encDec);
        Listening second=new Listening(sock,protocol,encDec);
        listening=new Thread(second);
        keyboard=new Thread(first);
        TerminateThreads currThreads=new TerminateThreads(keyboard, listening);
        keyboard.start();
        listening.start();

        try{
            keyboard.join();
            listening.join();
        }catch (InterruptedException e){}
    }

}