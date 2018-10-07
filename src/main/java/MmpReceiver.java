import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MmpReceiver extends Thread {
    private DatagramSocket socket;
    private boolean isRunning;
    private byte[] buffer;
    private AtomicBoolean ackReceived;
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
    private Map<String, String> memberList;
    private String localIP;
    private String nodeID;
    private int portNum;
    private int timeOut = 1000;
    private String receiverPrefix = "[RECEIVER]: ";
    private static final String LOG_NAME = "../receiver.log";
    private BufferedWriter logWriter;
    //TO-DO: Writing changes to local member list to log file

    public MmpReceiver(DatagramSocket socket, Map<String, String> memberList, int portNum,
                       String localIP, String nodeID, AtomicBoolean ackReceived){
        this.socket = socket;
        this.memberList = memberList;
        this.portNum = portNum;
        this.localIP = localIP;
        this.nodeID = nodeID;
        this.ackReceived = ackReceived;
        this.isRunning = true;
        this.buffer = new byte[512];
        try {
            File file = new File(LOG_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            this.logWriter = new BufferedWriter(new OutputStreamWriter(fos));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public void sendPacket(String msg, InetAddress address, int portNum) throws IOException{
        this.buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, portNum);
        this.socket.send(packet);
    }

    public void execMessage(DatagramPacket packet) throws IOException{
        String msg = new String(packet.getData(), 0 ,packet.getLength());
        System.out.println(packet.getLength());
        System.out.println( this.receiverPrefix + "execute msg: " + msg);
        String[] nodeInfo = msg.split(",");
        String senderID = nodeInfo[0];
        String msgType = nodeInfo[1];
        System.out.println(this.receiverPrefix + msgType);
        if(msgType.equals(NodeStatus.PING.name())){
            String ack = this.nodeID + "," + NodeStatus.ACK.name();
            this.sendPacket(ack, packet.getAddress(), packet.getPort());
        }else if(msgType.equals(NodeStatus.ACK.name())){
            ackReceived.set(true);
            synchronized (ackReceived) {
                ackReceived.notify();
            }
        }else if(msgType.equals(NodeStatus.JOINED.name())){
            String [] tmp = senderID.split(" ");
            this.memberList.put(tmp[0], tmp[1]);
            this.writeToLog(this.receiverPrefix+ tmp[0] + " is added to the local member list with" +
                    " a timestamp of " + tmp[1]);
        }else if(msgType.equals(NodeStatus.FAILED.name())){
            if(this.memberList.containsKey(senderID)) {
                this.writeToLog(this.receiverPrefix + senderID + " is leaving the mmp");
                this.memberList.remove(senderID);
            }
        }
    }

    public void writeToLog(String msg){
        try {
            this.logWriter.write(msg + "\n");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try{
            this.socket.setSoTimeout(this.timeOut);
        }catch(SocketException e){
            e.printStackTrace();
        }
        while(this.isRunning) {
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
            try {
                this.socket.receive(packet);
                this.execMessage(packet);
            }catch(SocketTimeoutException e){
                this.writeToLog(this.receiverPrefix + "No packet received in one timeout!");
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public void terminate(){
        this.isRunning = false;
        try {
            this.logWriter.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
