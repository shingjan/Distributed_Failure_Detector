import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


public class MmpServer {
    protected DatagramSocket socket;
    protected boolean isRunning;
    protected byte[] buffer;
    protected static String[] membership = {
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
    protected Map<String, String> memberList;
    protected String introducerIP;
    protected String localIP;
    protected String nodeID;
    protected int portNum;
    protected NodeStatus nodeStatus;
    protected AtomicBoolean ackReceived = new AtomicBoolean(false);
    private String serverPrefix = "[SERVER]: ";

    public MmpServer(int portNum) throws SocketException {
        this.portNum = portNum;
        this.socket = new DatagramSocket(this.portNum);
        this.memberList = new HashMap<>();
        this.introducerIP = membership[0];
        this.localIP = this.findLocalIP();
        this.nodeStatus = NodeStatus.JOINED;
        this.nodeID = this.localIP+" "+System.currentTimeMillis();
        this.isRunning = true;
        this.buffer = new byte[512];
    }

    public boolean joinMmp(){
        Socket introducer = null;
        try{
            introducer = new Socket(this.introducerIP, this.portNum);
        }catch(IOException e){
            System.out.println(this.serverPrefix + "Cannot connect to Introducer. Joining not available.");
            return false;
        }
        System.out.println(this.serverPrefix + "Introducer Connected. Getting up-to-date mmp list from it...");
        PrintWriter toIntrocucer = null;
        BufferedReader fromIntroducer = null;
        try {
            fromIntroducer = new BufferedReader(
                    new InputStreamReader(introducer.getInputStream()));
            toIntrocucer = new PrintWriter(
                    new OutputStreamWriter(introducer.getOutputStream()));
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        System.out.println(this.serverPrefix + "Input/Ouput Stream inited");
        String line = null;
        try {
            toIntrocucer.println(this.nodeID);
            toIntrocucer.flush();
            while ((line = fromIntroducer.readLine()) != null) {
                System.out.println(this.serverPrefix + line +" updated from introducer");
                String[] tmp = line.split(" ");
                this.memberList.put(tmp[0], tmp[1]);
            }
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        System.out.println(this.serverPrefix + "Member list updated from introducer!");
        return true;
    }

    public void launch(){
        MmpReceiver mmpReceiver = new MmpReceiver(this.socket, this.memberList, this.portNum,
                this.localIP, this.nodeID, this.ackReceived);
        mmpReceiver.setDaemon(true);
        mmpReceiver.start();
        System.out.println(this.serverPrefix + "Receiver running in background");
        MmpSender mmpSender = new MmpSender(this.socket, this.memberList, this.portNum,
                this.localIP, this.nodeID, this.ackReceived);
        mmpSender.setDaemon(true);
        mmpSender.start();
        System.out.println(this.serverPrefix + "Sender running in background");
        //command line
        while(this.isRunning){
            try {
                System.out.println(this.serverPrefix + "Available cmds: status, self, decommission");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String command = br.readLine();
                if(command.equals("status")){
                    this.printMemberList();
                }else if(command.equals("decommission")){
                    System.out.println(this.serverPrefix + "Decommissioning " + this.localIP);
                    this.isRunning = false;
                }else if(command.equals("self")) {
                    System.out.println(this.serverPrefix + localIP);
                }else{
                    System.out.println(this.serverPrefix + "Wrong cmd, try again");
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        //kill sender and receiver thread
        mmpReceiver.terminate();
        mmpSender.terminate();
        try {
            mmpReceiver.join();
            mmpSender.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(this.serverPrefix + "sender & receiver terminated. Decommission finished");
        this.socket.close();
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
            String value = this.memberList.get(key);
            System.out.println(key + ", " + value);
        }
    }

    public static void main(String[] args){
        try {
            MmpServer mmpServer = new MmpServer(4445);
            //MmpClient mmpClient = new MmpClient();
            if(mmpServer.joinMmp()) {
                mmpServer.launch();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
