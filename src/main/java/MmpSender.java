import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MmpSender extends Thread {
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
    private String senderPrefix = "[SENDER]: ";
    public MmpSender(DatagramSocket socket, Map<String, String> memberList, int portNum,
                     String localIP, String nodeID, AtomicBoolean ackReceived) {
        this.socket = socket;
        this.memberList = memberList;
        this.portNum = portNum;
        this.localIP = localIP;
        this.nodeID = nodeID;
        this.ackReceived = ackReceived;
    }

    public void sendPacket(String msg, InetAddress address, int portNum) throws IOException{
        this.buffer = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, portNum);
        this.socket.send(packet);
    }

    public void sendPing(InetAddress address, int portNum) throws IOException{
        StringBuilder pingMsg = new StringBuilder();
        System.out.println(this.senderPrefix + "Ping msg send to " + address.getHostName()
                + " from " + this.localIP);
        pingMsg.append(this.nodeID).append(",").append(NodeStatus.PING.name());
        sendPacket(pingMsg.toString(), address, portNum);
    }

    public void broadcastToAll(String msg){
        //For broadcase, it wouldn't require any ACK to be sent back
        try {
            for (String member : memberList.keySet()) {
                this.sendPacket(msg, InetAddress.getByName(member), this.portNum);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public String[] getMonitorList(){
        String[] monitors = new String[3];
        int index = Arrays.asList(membership).indexOf(this.localIP) + 1;
        int indexMonitor = 0;
        for(int i = index; i < membership.length + index; i++){
            int curr = (i >= membership.length) ? i - membership.length : i;
            if(this.memberList.keySet().contains(membership[curr])) {
                monitors[indexMonitor] = membership[curr];
                //System.out.println(monitors[indexMonitor] + " would be pinged by " + this.localIP);
                indexMonitor++;
                if(indexMonitor == 3){
                    break;
                }
            }
        }
        return monitors;
    }

    @Override
    public void run(){
        String[] monitorList = this.getMonitorList();
        while(isRunning){
            for(String monitor : monitorList){
                try {
                    this.sendPing(InetAddress.getByName(monitor), this.portNum);
                }catch (IOException e){
                    e.printStackTrace();
                }
                try {
                    synchronized (ackReceived) {
                        ackReceived.wait(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (ackReceived.get()) {
                    System.out.println(this.senderPrefix + "ACK from " + monitor + " is received");
                }else{
                    System.out.println(this.senderPrefix + "Failure of node " + monitor +" detected. Remove it from" +
                            "local member list" );
                    this.memberList.remove(monitor);
                }
            }
            //dynamically change the monitor list after each loop
            monitorList = this.getMonitorList();
        }
    }

    public void terminate(){
        this.isRunning = false;
    }
}
