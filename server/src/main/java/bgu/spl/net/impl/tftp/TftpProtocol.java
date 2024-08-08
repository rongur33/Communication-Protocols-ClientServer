package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    boolean shouldTerminate;
     int id;
    Connections<byte[]> connections;

    String uploadFile;

    LinkedList<byte[]> uploadData;

    byte end=0;

    LinkedList<byte[]> files;
    



    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connections = connections;
        this.id = connectionId;
        shouldTerminate = false;
    }

    @Override
    public void process(byte[] message) {
        Opcode myOpcode = peekOpcode(message);
        if (myOpcode == Opcode.LOGRQ) {
            login(message);
        } else if (myOpcode == Opcode.DISC) {
            disconnect();
        } else if (myOpcode == Opcode.DIRQ) {
            dirq();
        } else if (myOpcode == Opcode.ACK) {
            receiveAck(message);
        } else if (myOpcode == Opcode.RRQ) {
            rrq(message);
        } else if (myOpcode == Opcode.None) {
            error((short)4, "Illegal TFTP operation unknown opcode");
        } else if (myOpcode == Opcode.DELRQ) {
            delrq(message);
        } else if (myOpcode == Opcode.DATA) {
            receiveData(message);
        }
        else{
            wrq(message);
        }
    }
    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public Integer getId() {
        return id;
    }

    private void login(byte[] massage) {
        String newName = extractName(massage);
        if (((ConnectionsImpl) connections).isNameExcited(id) == true) {
            error((short)7, "User already logged in");
        } else if (((ConnectionsImpl) connections).isNameExcited(newName) == true) {
            error((short)0, "username already excited");
        } else {
            ((ConnectionsImpl) connections).addName(id, newName);
            ack();
        }
    }

    private void disconnect() {
        if (((ConnectionsImpl) connections).isNameExcited(id) == false) {
            error((short)6, "User not logged in");
        } else {
            ConnectionHandler myHandler = ((ConnectionsImpl) connections).getHandler(id);
            ack();
            ((ConnectionsImpl) connections).removeName(id);
            ((ConnectionsImpl) connections).disconnect(id);
            shouldTerminate = true;
        //     try {
        //         myHandler.close();
        //     } catch (IOException ex) {
        //  }
        }
    }

    private void error(short errorCode, String errorMsg) {
        short a = 5;
        byte [] a_bytes = new byte []{( byte) (a >> 8) , ( byte ) (a & 0xff),( byte) (a >> 8),( byte ) (errorCode & 0xff)};
        byte[] array2 =errorMsg.getBytes(StandardCharsets.UTF_8);
        byte[] combinedArray = new byte[a_bytes.length + array2.length];
        System.arraycopy(a_bytes, 0, combinedArray, 0, a_bytes.length);
        System.arraycopy(array2, 0, combinedArray, a_bytes.length, array2.length);
        byte[] newArray = new byte[combinedArray.length + 1];
        System.arraycopy(combinedArray, 0, newArray, 0, combinedArray.length);
        newArray[newArray.length - 1] = end;
        ((ConnectionsImpl) connections).send(id, newArray);
    }

    private void ack() {
        short a = 4;
        byte [] a_bytes = new byte []{( byte) (a >> 8) , ( byte ) (a & 0xff)};
        short b=0;
        byte[] array2 =new byte []{( byte) (b >> 8) , ( byte ) (b & 0xff)};
        byte[] ans = new byte[4];
        System.arraycopy(a_bytes, 0, ans, 0, a_bytes.length);
        System.arraycopy(array2, 0, ans, a_bytes.length, array2.length);
        ((ConnectionsImpl) connections).send(id, ans);
    }

    public static String extractName(byte[] array) {
        int length = array.length;
        byte[] usernameBytes = new byte[length - 2]; // excluding opcode and terminator
        System.arraycopy(array, 2, usernameBytes, 0, length - 2);
        return new String(usernameBytes, StandardCharsets.UTF_8);
    }

    public Opcode peekOpcode(byte[] curr) {
        assert curr.length >= 2;
        int u16Opcode = ((curr[0] & 0xFF) << 8) | (curr[1] & 0xFF);
        return Opcode.fromU16(u16Opcode);
    }

    private void dirq() {
        if (((ConnectionsImpl) connections).isNameExcited(id) == false) {
            error((short)6, "User not logged in");
        } else {
                String directoryPath = "Flies"+File.separator;

                // Get the list of files in the directory
                File directory = new File(directoryPath);
                File[] files = directory.listFiles();

                // Create a StringBuilder to concatenate file names
                StringBuilder sb = new StringBuilder();

                // Iterate through the files and append their names to the StringBuilder
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            sb.append(file.getName());
                            sb.append(end); // Append a null byte to separate file names
                        }
                    }
                }

                // Convert the concatenated string to a byte array
                byte[] byteArray = sb.toString().getBytes(StandardCharsets.UTF_8);
                for (int i = 0; i < byteArray.length; i++) {
                    if (byteArray[i] == 48) {
                        byteArray[i] = end;
                    }
                }
                ackData(byteArray);
        }
    }

    private void ackData(byte[] massage) {
        // Your byte array here
        int chunkSize = 512;
        int arrayLength = massage.length;
        int numOfChunks = (int) Math.ceil((double) arrayLength / chunkSize);
        files = new LinkedList<byte[]>();
        for (int i = 0; i < numOfChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(arrayLength, start + chunkSize);
            byte[] chunk = Arrays.copyOfRange(massage, start, end);
            files.add(chunk);
        }
        short code=3;
        short numOfChunks_short = 1;
        // for (byte[] _chunk : files) {
            byte[] _chunk=files.get(0);
            short length=(short)_chunk.length;
            byte[] a_bytes = new byte[]{(byte)(length >> 8), (byte)(length & 0xff)};
            byte[] c_bytes= new byte[]{(byte)(numOfChunks_short >> 8), (byte)(numOfChunks_short & 0xff)};
            byte[] prependBytes =new byte[]{end,( byte ) (code & 0xff)};
            int combinedLength = prependBytes.length + a_bytes.length + c_bytes.length;
            byte[] combinedArray = new byte[combinedLength];
            System.arraycopy(prependBytes, 0, combinedArray, 0, prependBytes.length);
            System.arraycopy(a_bytes, 0, combinedArray, prependBytes.length, a_bytes.length);
            System.arraycopy(c_bytes, 0, combinedArray, prependBytes.length + a_bytes.length, c_bytes.length);
            byte[] newArray = new byte[combinedArray.length + _chunk.length];
            System.arraycopy(combinedArray, 0, newArray, 0, combinedArray.length);
            System.arraycopy(_chunk, 0, newArray, combinedArray.length, _chunk.length);
            ((ConnectionsImpl) connections).send(id, newArray);
            // try {
            //     Thread.sleep(50);
            // } catch (InterruptedException e) {
            //     throw new RuntimeException(e);
            // }
            // numOfChunks_short++;
        
    }

    private void receiveAck(byte[] msg) {
        byte [] b = {msg[2],msg[3]};
        short b_short = ( short ) ((( short ) b[0]) << 8 | ( short ) ( b[1]) & 0x00ff );
        if(b_short!=0&&files.size()!=b_short){
            short code=3;
           short numOfChunks=(short) (b_short+1);
           byte[] _chunk=files.get(b_short);
           short length=(short)_chunk.length;
           byte[] a_bytes = new byte[]{(byte)(length >> 8), (byte)(length & 0xff)};
           byte[] c_bytes= new byte[]{(byte)(numOfChunks >> 8), (byte)(numOfChunks & 0xff)};
           byte[] prependBytes =new byte[]{end,( byte ) (code & 0xff)};
           int combinedLength = prependBytes.length + a_bytes.length + c_bytes.length;
           byte[] combinedArray = new byte[combinedLength];
           System.arraycopy(prependBytes, 0, combinedArray, 0, prependBytes.length);
           System.arraycopy(a_bytes, 0, combinedArray, prependBytes.length, a_bytes.length);
           System.arraycopy(c_bytes, 0, combinedArray, prependBytes.length + a_bytes.length, c_bytes.length);
           byte[] newArray = new byte[combinedArray.length + _chunk.length];
           System.arraycopy(combinedArray, 0, newArray, 0, combinedArray.length);
           System.arraycopy(_chunk, 0, newArray, combinedArray.length, _chunk.length);
           ((ConnectionsImpl) connections).send(id, newArray);
        }

    }

    private void rrq(byte[] message) {
        if (((ConnectionsImpl) connections).isNameExcited(id) == false) {
            error((short)6, "User not logged in");
        } else {
            String fileName = extractName(message);
                if (!isFileExistsWithMatchingName(fileName, "Flies"+File.separator)) {
                    error((short) 1, "File not found");
                } else {
                     ((ConnectionsImpl) connections).getLock(fileName).readLock().lock();
                        if (((ConnectionsImpl) connections).isFileExist(fileName) == true) {
                            String filePath = "Flies"+File.separator+fileName;
                            try {
                                File file = new File(filePath);
                                byte[] byteArray = readFileToByteArray(file);
                                // Now you have your file contents in a byte array (byteArray)
                                // You can use this byte array as needed
                                ackData(byteArray);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        else {
                            error((short) 1, "File not found");
                        }
                    ((ConnectionsImpl) connections).getLock(fileName).readLock().unlock();
                }
        }
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }

        fis.close();
        bos.close();

        return bos.toByteArray();
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

    private void delrq(byte[] message) {
        if (((ConnectionsImpl) connections).isNameExcited(id) == false) {
            error((short)6, "User not logged in");
        }
        else{
            String fileName = extractName(message);
            boolean shouldDelete=false;
                if (!isFileExistsWithMatchingName(fileName, "Flies"+File.separator)) {
                    error((short) 1, "File not found");
                } else {
                    ((ConnectionsImpl) connections).getLock(fileName).writeLock().lock();
                        if(((ConnectionsImpl) connections).isFileExist(fileName)==true) {
                            String filePath = "Flies" +File.separator+ fileName;
                            try {
                                File fileToDelete = new File(filePath);
                                fileToDelete.delete();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            ack();
                            bCast(fileName, "delete");
                            shouldDelete=true;
                        }
                        else {
                            error((short) 1, "File not found");
                        }
                    ((ConnectionsImpl) connections).getLock(fileName).writeLock().unlock();
                      if(shouldDelete==true){
                          ((ConnectionsImpl) connections).removeLock(fileName);
                      }
                }
        }
    }

    private void bCast(String name,String operation) {
        byte[] ans;
        short a = 9;
        byte [] array2 =name.getBytes(StandardCharsets.UTF_8);
        if(operation.equals("delete")){
            byte [] a_bytes = new byte []{( byte) (a >> 8) , ( byte ) (a & 0xff),( byte) (a >> 8)};
            byte[] combinedArray = new byte[a_bytes.length + array2.length];
            System.arraycopy(a_bytes, 0, combinedArray, 0, a_bytes.length);
            System.arraycopy(array2, 0, combinedArray, a_bytes.length, array2.length);
            ans = new byte[combinedArray.length + 1];
            System.arraycopy(combinedArray, 0, ans, 0, combinedArray.length);
            ans[ans.length - 1] = end;
        }
        else {
                short b=1;
                byte [] a_bytes = new byte []{( byte) (a >> 8) , ( byte ) (a & 0xff),( byte) (b & 0xff)};
                byte[] combinedArray = new byte[a_bytes.length + array2.length];
                System.arraycopy(a_bytes, 0, combinedArray, 0, a_bytes.length);
                System.arraycopy(array2, 0, combinedArray, a_bytes.length, array2.length);
                ans = new byte[combinedArray.length + 1];
                System.arraycopy(combinedArray, 0, ans, 0, combinedArray.length);
                ans[ans.length - 1] = end;
        }
        ConcurrentHashMap<Integer,String> myClients=((ConnectionsImpl) connections).getNamesOfClients();
        for (Integer key : myClients.keySet()) {
            ((ConnectionsImpl) connections).send(key,ans);
        }
    }

    private void wrq(byte[] message) {
        if (((ConnectionsImpl) connections).isNameExcited(id) == false) {
            error((short)6, "User not logged in");
        }
        else{
            String fileName = extractName(message);
            if (isFileExistsWithMatchingName(fileName, "Flies"+File.separator)){
                error((short)5,"File already exists");
            } else if (fileName.contains("0")) {
                error((short)0,"Illegal file name - can't contain 0");
            } else{
                ack();
                String directoryPath = "src"+File.separator;
                uploadFile=fileName;
                uploadData=new LinkedList<byte[]>();
                try {
                    // Create directory if it doesn't exist
                    Files.createDirectories(Paths.get(directoryPath));

                    // Create the file
                    Path filePath = Paths.get(directoryPath, fileName);
                    Files.createFile(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void receiveData(byte[] message) {
        String directoryPath = "src"+File.separator;
        short a=4;
        byte[] separateArray = Arrays.copyOfRange(message, 6, message.length);
        uploadData.add(separateArray);
        byte[] ack=new byte []{( byte) (a >> 8) , ( byte ) (a & 0xff)};
        byte[] blockNum= {message[4],message[5]};
        byte[] newAck=new byte[4];
        System.arraycopy(ack, 0, newAck, 0, ack.length);
        System.arraycopy(blockNum, 0, newAck, ack.length, blockNum.length);
        ((ConnectionsImpl) connections).send(id,newAck);
        if(separateArray.length<512){
            try (FileOutputStream fos = new FileOutputStream(Paths.get(directoryPath, uploadFile).toString())) {
                for(byte[] data:uploadData) {
                    fos.write(data);
                }
            } catch (IOException e) {e.printStackTrace();}
            String srcFilePath = "src"+File.separator+uploadFile; // Change this to the actual path of your file in the src directory
          //  String destFilePath = "Flies/"+uploadFile; // Change this to the desired path in the Files directory

            // Create a File object for the source file
            File srcFile = new File(srcFilePath);

            // Create a File object for the destination directory
            File destDir = new File("Flies");


            // Check if the destination directory exists, if not, create it

            // Create a File object for the destination file
            File destFile = new File(destDir, srcFile.getName());
            srcFile.renameTo(destFile);
            ((ConnectionsImpl) connections).addFile(uploadFile,new ReentrantReadWriteLock(true));
            bCast(uploadFile,"upload");
        }


    }
}


