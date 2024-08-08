package bgu.spl.net.srv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionsImpl<T> implements Connections <T>  {
private ConcurrentHashMap<Integer,ConnectionHandler<T>> database;
private static int counter;
private  ConcurrentHashMap<Integer,String> namesOfClients;

private ConcurrentHashMap<String, ReentrantReadWriteLock> locksPerFile;

private static  ConnectionsImpl obj ;


private ConnectionsImpl() {
    counter=0;
    database=new ConcurrentHashMap<>();
    namesOfClients=new ConcurrentHashMap<>();
    locksPerFile=new ConcurrentHashMap<>();
    Path directoryPath = Paths.get("Flies");
    try {
        // Iterate through the files in the directory
        Files.list(directoryPath)
                .filter(Files::isRegularFile) // Filter only regular files
                .forEach(filePath -> {
                    // Extract the file name
                    String fileName = filePath.getFileName().toString();
                    // Add the file name to the ConcurrentHashMap with a new Object as value
                    locksPerFile.put(fileName, new ReentrantReadWriteLock(true));
                });
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public static ConnectionsImpl getInstance()
    {
       if (obj == null) {
            //To make thread safe
           synchronized (ConnectionsImpl.class)
            {
                // check again as multiple threads
                // can reach above step
                if (obj == null) {
                    obj = new ConnectionsImpl();
                }
            }
       }
        return obj;
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        database.put(connectionId,handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        database.get(connectionId).send(msg);

        return true;//מה לעשות פה?
    }

    @Override
    public void disconnect(int connectionId) {
        database.remove(connectionId);
    }
//    public void getNextId(){
//        int i;
//        do{
//            i = counter.get();
//        }while(!counter.compareAndSet(i,i+1));
//    }

//    public AtomicInteger getCounter(){
//        return counter;
//    }

    public int getCounter(){
    counter=counter+1;
       return counter;
    }

    public ConnectionHandler<T> getHandler(int id){
        return database.get(id);
    }

    public void addName(Integer id,String name){
        namesOfClients.put(id, name);
    }
    public boolean isNameExcited(String name){
        if(namesOfClients.containsValue(name)){
            return true;
        }
        return false;
    }

    public boolean isNameExcited(Integer id){
        if(namesOfClients.containsKey(id)){
            return true;
        }
        return false;
    }

    public void removeName(Integer id){
        namesOfClients.remove(id);
    }

    public ConcurrentHashMap<Integer,String> getNamesOfClients(){
        return namesOfClients;
    }

    public void addFile(String fileName,ReentrantReadWriteLock lock){
        locksPerFile.put(fileName,lock);
    }

    public ReentrantReadWriteLock getLock(String fileName){
        return locksPerFile.get(fileName);
    }

    public void removeLock(String fileName){
        locksPerFile.remove(fileName);
    }

    public ConcurrentHashMap<String,ReentrantReadWriteLock> getAllLocks(){
        return locksPerFile;
    }

    public boolean isFileExist(String fileName){
        if(locksPerFile.containsKey(fileName)){
            return true;
        }
        return false;
    }


}
