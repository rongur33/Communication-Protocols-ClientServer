package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Listening implements Runnable{

    Socket socket;

    ClientTftpProtocol protocol;

    TftpClientEncoderDecoder encDec;

    

    public Listening(Socket socket,ClientTftpProtocol protocol,TftpClientEncoderDecoder encDec){
        this.socket=socket;
        this.protocol=protocol;
        this.encDec=encDec;
    }
    @Override
    public void run() {
        BufferedInputStream in;
        BufferedOutputStream out ;
        try {
            in = new BufferedInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new BufferedOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int read;
        while (true&&!protocol.shouldTerminate()) {
            try {
                if (!(!protocol.shouldTerminate()  && (read = in.read()) >= 0)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            byte[] nextMessage = encDec.decodeNextByte((byte) read);
            if (nextMessage != null) {
                byte[] toProtocol= protocol.process(nextMessage);
                if(toProtocol!=null){
                    try {
                        out.write(encDec.encode(toProtocol));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        out.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }

    }

    public void close() throws IOException {
        socket.close();
    }



}
