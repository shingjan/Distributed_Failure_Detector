import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class MmpServer {
    public DatagramSocket socket;
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

    public MmpServer(int portNum) throws SocketException {
        this.socket = new DatagramSocket(portNum);
        this.memberList = new HashMap<>();
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    public void closeSocket(){
        this.socket.close();
    }

    public DatagramPacket receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
        this.socket.receive(packet);
        return packet;
    }

    public void sendPacket(DatagramPacket packet, String msg) throws IOException{
        this.buffer = msg.getBytes();
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(buffer, buffer.length, address, port);
        this.socket.send(packet);
    }

    public String findLocalIP(){
        String ip = "";
        try(final DatagramSocket tmpSocket = new DatagramSocket()){
            tmpSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = tmpSocket.getLocalAddress().getHostAddress();
        }catch(SocketException | UnknownHostException e){
            e.printStackTrace();
        }
        return ip;
    }

    public static void main(String[] args){
        try {
            MmpServer mmpServer = new MmpServer(4445);
            DatagramPacket packet = mmpServer.receivePacket();
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println(received);
            mmpServer.sendPacket(packet, "Here is looking at you, kid");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
