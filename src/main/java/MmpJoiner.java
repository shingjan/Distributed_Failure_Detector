import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class MmpJoiner extends Thread {
    private final int port;
    private final AtomicBoolean isRunning;
    private Map<String, String> memberList;
    private DatagramSocket socket;
    private String nodeID;
    private final String joinerPrefix = "[JOINER]: ";

    public MmpJoiner(Map<String, String> memberList, DatagramSocket socket, int port, String nodeID) {
        this.isRunning=new AtomicBoolean(true);
        this.port=port;
        this.memberList = memberList;
        this.socket = socket;
        this.nodeID = nodeID;
    }

    public void terminate() {
        this.isRunning.set(false);
    }

    public void broadcastToAll(String msg){
        //For broadcase, it wouldn't require any ACK to be sent back
        try {
            for (String member : memberList.keySet()) {
                this.sendPacket(msg, InetAddress.getByName(member), 4445);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void sendPacket(String msg, InetAddress address, int portNum) throws IOException{
        byte[] buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, portNum);
        this.socket.send(packet);
    }

    @Override
    public void run() {
        System.out.println(this.joinerPrefix + "Joiner running in background");
        byte[] buffer = this.nodeID.getBytes();


        while (isRunning.get()) {
            DatagramPacket firstMsg = new DatagramPacket(buffer, buffer.length);
            try {
                this.socket.receive(firstMsg);
            }catch(IOException e){
                e.printStackTrace();
            }

            String joinMsg = new String(firstMsg.getData(), 0, firstMsg.getLength());
            System.out.println(joinMsg);
            String senderID = joinMsg.split(" ")[0];
            String senderTimeStamp = joinMsg.split(" ")[1];
            System.out.println( this.joinerPrefix + "join requested by : " + senderID);
            this.memberList.put(senderID, senderTimeStamp);
            StringBuilder updatedList = new StringBuilder();
            for (String member : memberList.keySet()) {
                updatedList.append(member+" "+memberList.get(member)).append("|");
            }
            byte[] memberByteArr = updatedList.toString().getBytes();
            DatagramPacket memberPacket = new DatagramPacket(memberByteArr, memberByteArr.length);
            try{
                this.socket.send(memberPacket);
            }catch(IOException e){
                e.printStackTrace();
            }

            String msg = joinMsg + "," + NodeStatus.JOINED;
            this.broadcastToAll(msg);
            memberList.put(senderID, senderTimeStamp);
            System.out.println(this.joinerPrefix + senderID + " added to membership list");
        }

    }
}
