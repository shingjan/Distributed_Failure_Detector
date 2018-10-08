import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;

public class MmpIntroducer extends MmpServer {
    public MmpIntroducer(int portNum) throws SocketException {
        super(portNum);
    }

    public void runJoiner(){
        System.out.println("Introducer node started");
        this.memberList.put(this.localIP, String.valueOf(System.currentTimeMillis()));
        MmpJoiner mmpJoiner = new MmpJoiner(this.memberList, this.socket, this.portNum, this.nodeID);
        mmpJoiner.setDaemon(true);
        mmpJoiner.start();
        this.launch();
        mmpJoiner.terminate();
        try{
            mmpJoiner.join();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        try {
            MmpIntroducer mmpIntroducer = new MmpIntroducer(4445);
            mmpIntroducer.runJoiner();
        }catch(SocketException e){
            e.printStackTrace();
        }
    }

}
