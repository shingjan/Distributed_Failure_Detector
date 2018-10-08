import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;

public class MmpIntroducer extends MmpServer {
    private MmpJoiner mmpJoiner;
    public MmpIntroducer(int portNum) throws SocketException {
        super(portNum);
    }

    public void runJoiner(){
        System.out.println("Introducer node started");
        this.memberList.put(this.localIP, String.valueOf(System.currentTimeMillis()));
        this.mmpJoiner = new MmpJoiner(this.memberList, this.socket, this.portNum, this.nodeID);
        mmpJoiner.setDaemon(true);
        mmpJoiner.start();
    }

    public static void main(String[] args){
        try {
            MmpIntroducer mmpIntroducer = new MmpIntroducer(4445);
            mmpIntroducer.runJoiner();
            mmpIntroducer.launch();
            mmpIntroducer.mmpJoiner.terminate();
            try{
                mmpIntroducer.mmpJoiner.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }catch(SocketException e){
            e.printStackTrace();
        }
    }
}
