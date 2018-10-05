import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

public class MmpReceiver extends Thread {
    private DatagramSocket socket;
    private boolean isRunning;
    private byte[] buffer = new byte[256];
    private static String[] membership = {
            "172.22.158.208",
            "172.22.154.209",
            "172.22.156.209",
            "172.22.158.209",
            "172.22.154.210",
            "172.22.156.210",
            "172.22.158.210",
            "172.22.154.211",
            "172.22.156.211",
            "172.22.158.211"
    };
    private Map<String, NodeStatus> memberList;
    private String localIP;
    private int portNum;
    private NodeStatus nodeStatus;
    public MmpReceiver(){

    }

    public DatagramPacket receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
        this.socket.receive(packet);
        return packet;
    }

    @Override
    public void run() {

    }
}
