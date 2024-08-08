package bgu.spl.net.impl.tftp;

public class TerminateThreads {

     static Thread keyboard;
     static Thread Listening;

    public TerminateThreads(Thread keyboard,Thread Listening){
        this.keyboard=keyboard;
        this.Listening=Listening;
    }
    public static void terminate(){
        keyboard.interrupt();
        Listening.interrupt();
    }
    
}
