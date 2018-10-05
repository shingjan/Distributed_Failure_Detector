import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class MmpServer {
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

    public MmpServer(int portNum) throws SocketException {
        this.portNum = portNum;
        this.socket = new DatagramSocket(this.portNum);
        this.memberList = new HashMap<>();
        this.localIP = this.findLocalIP();
        this.nodeStatus = NodeStatus.ALIVE;
        this.isRunning = true;
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

    public void sendACK(DatagramPacket packet) throws IOException {
        this.sendPacket(NodeStatus.ACK.name(), packet.getAddress());
    }

    public void sendPING() throws IOException {
        for(String member : membership) {
            this.sendPacket(NodeStatus.PING.name(), InetAddress.getByName(member));
        }
    }

    public void sendPacket(String msg, InetAddress address) throws IOException{
        this.buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(this.buffer, this.buffer.length, address, this.portNum);
        this.socket.send(packet);
    }

    public void updateMemberList(DatagramPacket packet) {
        String received = new String(packet.getData(), 0, packet.getLength());
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

    public void printMemberList(){
        for (String key : this.memberList.keySet()){
            String value = this.memberList.get(key).toString();
            System.out.println(key + ", " + value);
        }
    }

    public static void main(String[] args){
        try {
            MmpServer mmpServer = new MmpServer(4445);
            //MmpClient mmpClient = new MmpClient();
            System.out.println("The IP address of this node is " + mmpServer.localIP);
            while(mmpServer.isRunning) {
                mmpServer.sendPING();
                DatagramPacket packet = mmpServer.receivePacket();
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println(received);
                if(received.equals(NodeStatus.PING.name())){
                    mmpServer.sendACK(packet);
                }else if(received.equals(NodeStatus.ACK.name())){
                    mmpServer.updateMemberList(packet);
                }else if(received.equals(NodeStatus.ALIVE.name())){

                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
