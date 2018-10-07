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
    private String joinerPrefix = "[JOINER]: ";

    public MmpJoiner(Map<String, String> memberList, DatagramSocket socket, int port) {
        this.isRunning=new AtomicBoolean(false);
        this.port=port;
        this.memberList = memberList;
        this.socket = socket;
    }

    public void terminate() {
        this.isRunning.set(true);
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
        System.out.println("MmpJoiner thread started");
        ServerSocket tcp=null;

        try {
            tcp=new ServerSocket(this.port);
            tcp.setSoTimeout(100);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!isRunning.get()) {
            Socket joinRequest = null;
            try {
                joinRequest=tcp.accept();
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }

            Scanner inputReader = null;
            PrintWriter outputWriter = null;
            try {
                inputReader = new Scanner(new InputStreamReader(joinRequest.getInputStream()));
                outputWriter = new PrintWriter(new OutputStreamWriter(joinRequest.getOutputStream()));
                inputReader.useDelimiter("\n");
            } catch (IOException e) {
                System.err.println("Input/Output buffer not working properly");
                return;
            }

            String joinMsg=inputReader.nextLine();
            System.out.println(joinMsg);
            String senderID = joinMsg.split(" ")[0];
            String senderTimeStamp = joinMsg.split(" ")[1];
            System.out.println( this.joinerPrefix + "join requested by : " + senderID);
            this.memberList.put(senderID, senderTimeStamp);
            for (String member : memberList.keySet()) {
                outputWriter.println(member+" "+memberList.get(member));
            }

            outputWriter.flush();
            String msg = joinMsg + " " + NodeStatus.JOINED;
            this.broadcastToAll(msg);
            try {
                joinRequest.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            memberList.put(senderID, senderTimeStamp);
            System.out.println(this.joinerPrefix + senderID + " added to membership list");
        }

        try {
            tcp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
