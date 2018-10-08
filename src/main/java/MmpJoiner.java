import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class MmpJoiner extends Thread {
    private final int portNum;
    private final AtomicBoolean isRunning;
    private Map<String, String> memberList;
    private DatagramSocket socket;
    private String nodeID;
    private final String joinerPrefix = "[JOINER]: ";

    public MmpJoiner(Map<String, String> memberList, DatagramSocket socket, int port, String nodeID) {
        this.isRunning=new AtomicBoolean(true);
        this.portNum=port;
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
        ServerSocket joiner = null;
        try {
            joiner = new ServerSocket(this.portNum);
            joiner.setSoTimeout(500);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (isRunning.get()) {
            Socket join = null;
            try {
                join = joiner.accept();
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                System.out.println(this.joinerPrefix + "Cannot connect to joiner");
            }
            Scanner input = null;
            PrintWriter output = null;
            try {
                input = new Scanner(new InputStreamReader(join.getInputStream()));
                output = new PrintWriter(new OutputStreamWriter(join.getOutputStream()));
                input.useDelimiter("\n");
            } catch (IOException e) {
                System.out.println(this.joinerPrefix + "Input/Output buffer not working properly");
                return;
            }

            String joinMsg = input.nextLine();
            String senderID = joinMsg.split(" ")[0];
            String senderTimeStamp = joinMsg.split(" ")[1];
            System.out.println( this.joinerPrefix + senderID + " joining the group");
            this.memberList.put(senderID, senderTimeStamp);
            for (String member : memberList.keySet()) {
                output.println(member + " "+ memberList.get(member));
            }
            output.flush();
            try {
                join.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String msg = joinMsg + "," + NodeStatus.JOINED;
            this.broadcastToAll(msg);
            System.out.println(this.joinerPrefix + senderID + " added to membership list");
        }
        try {
            joiner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
