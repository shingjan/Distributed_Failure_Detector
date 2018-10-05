import java.io.IOException;
import java.net.*;


public class MmpClient{
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buffer;

    public MmpClient() throws SocketException {
        socket = new DatagramSocket();
        buffer = new byte[256];
        try {
            address = InetAddress.getByName("localhost");
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
    }

    public String sendEcho(String msg) throws IOException {
        this.buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, 4445);
        socket.send(packet);
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

    public void closeSocket() {
        socket.close();
    }

    public static void main(String[] args){
        MmpClient mmpClient;
        try {
            mmpClient = new MmpClient();
            String received = mmpClient.sendEcho("hi there");
            System.out.println(received);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}