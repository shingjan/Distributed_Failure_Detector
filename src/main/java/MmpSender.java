import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class MmpSender extends Thread {
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
    public MmpSender(DatagramSocket socket) {
        this.socket = socket;
    }



    public void sendACK(DatagramPacket packet) throws IOException {
        this.sendPacket(NodeStatus.ACK.name(), packet.getAddress());
    }

    public void sendPING() throws IOException {
        for(String member : membership) {
            this.sendPacket(NodeStatus.PING.name(), InetAddress.getByName(member));
        }
    }

    public void sendPacket(String msg, InetAddress address, int portNum) throws IOException{
        byte[] buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, portNum);
        this.socket.send(packet);
    }
}
