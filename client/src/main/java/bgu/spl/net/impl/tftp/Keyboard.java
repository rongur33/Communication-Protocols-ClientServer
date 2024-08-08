package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;

public class Keyboard implements Runnable{

    Socket socket;

    ClientTftpProtocol protocol;

    TftpClientEncoderDecoder encDec;

    public Keyboard(Socket socket, ClientTftpProtocol protocol, TftpClientEncoderDecoder encDec){
        this.socket=socket;
        this.protocol=protocol;
        this.encDec=encDec;
    }
    @Override
    public void run() {
        BufferedOutputStream out;
        Scanner scanner=new Scanner(System.in);
        try {
            out = new BufferedOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(!protocol.shouldTerminate()){
            String str=scanner.nextLine();
            byte[] send= keyboardProtocol(str,protocol);
            if(send!=null){
                try {
                    out.write(encDec.encode(send));
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
        }
       

    }


    private static byte[] keyboardProtocol(String str,ClientTftpProtocol protocol) {
        short opcode;
        String substring6="";
        String substring3="";
        byte end=0;
        byte[] ans;
        String userName;
        if(str.length()>=4)
        substring3 = str.substring(0, 4);
        if(str.length()>=6) {
            substring6 = str.substring(0, 6);
        }
        if(substring3.equals("RRQ ")){
            protocol.currOpcode=Opcode.RRQ;
            opcode=1;
            userName=str.substring(4);
            if(isFileExistsWithMatchingName(userName,"."+File.separator)){
                System.out.println("file already exist");
                ans=null;
            }
            else{
                protocol.fileToBeCreated=userName;
                protocol.newFileData=new LinkedList<byte[]>();
                String directoryPath = "src"+File.separator;
                try {
                    // Create directory if it doesn't exist
                    Files.createDirectories(Paths.get(directoryPath));

                    // Create the file
                    Path filePath = Paths.get(directoryPath, userName);
                    Files.createFile(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] userNameBytes=userName.getBytes(StandardCharsets.UTF_8);
                byte[] a_bytes = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
                ans=new byte[userNameBytes.length+a_bytes.length+1];
                System.arraycopy(a_bytes, 0, ans, 0, a_bytes.length);
                System.arraycopy(userNameBytes, 0, ans, a_bytes.length, userNameBytes.length);
                ans[ans.length-1]=end;
            }
        } else if (substring3.equals("WRQ ")) {
            protocol.currOpcode=Opcode.WRQ;
            userName=str.substring(4);
            if(!isFileExistsWithMatchingName(userName,"."+File.separator)){
                System.out.println("file does not exist");
                ans=null;
            }
            else {
                protocol.uploadFile=userName;
                opcode=2;
                byte[] userNameBytes=userName.getBytes(StandardCharsets.UTF_8);
                byte[] a_bytes = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
                ans=new byte[userNameBytes.length+a_bytes.length+1];
                System.arraycopy(a_bytes, 0, ans, 0, a_bytes.length);
                System.arraycopy(userNameBytes, 0, ans, a_bytes.length, userNameBytes.length);
                ans[ans.length-1]=end;
            }
        }
        else if (substring3.equals("DIRQ")) {
            protocol.currOpcode=Opcode.DIRQ;
            opcode=6;
            ans = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
        }else if (substring3.equals("DISC")) {
            protocol.currOpcode=Opcode.DISC;
            opcode=10;
            ans = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
        }
        else if(substring6.equals("LOGRQ ")){
            protocol.currOpcode=Opcode.LOGRQ;
            opcode=7;
            userName=str.substring(6);
            byte[] userNameBytes=userName.getBytes(StandardCharsets.UTF_8);
            byte[] a_bytes = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
            ans=new byte[userNameBytes.length+a_bytes.length+1];
            System.arraycopy(a_bytes, 0, ans, 0, a_bytes.length);
            System.arraycopy(userNameBytes, 0, ans, a_bytes.length, userNameBytes.length);
            ans[ans.length-1]=end;
        } else if (substring6.equals("DELRQ ")) {
            protocol.currOpcode=Opcode.DELRQ;
            opcode=8;
            userName=str.substring(6);
            byte[] userNameBytes=userName.getBytes(StandardCharsets.UTF_8);
            byte[] a_bytes = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
            ans=new byte[userNameBytes.length+a_bytes.length+1];
            System.arraycopy(a_bytes, 0, ans, 0, a_bytes.length);
            System.arraycopy(userNameBytes, 0, ans, a_bytes.length, userNameBytes.length);
            ans[ans.length-1]=end;
        }
        else {
            System.out.println("Invalid command!");
            ans=null;
        }
        return ans;
    }

    public static boolean isFileExistsWithMatchingName(String inputString, String directoryPath) {
        // Create a File object for the directory
        File directory = new File(directoryPath);

        // Check if the directory exists
        if (directory.exists() && directory.isDirectory()) {
            // Get list of files in the directory
            File[] files = directory.listFiles();

            // Check if any file name matches the input string
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(inputString)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
