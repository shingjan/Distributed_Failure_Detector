import java.io.*;
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
    private String introducerIP;
    private String localIP;
    private String nodeID;
    private int portNum;
    private NodeStatus nodeStatus;

    public MmpServer(int portNum) throws SocketException {
        this.portNum = portNum;
        this.socket = new DatagramSocket(this.portNum);
        this.memberList = new HashMap<>();
        this.introducerIP = membership[0];
        this.localIP = this.findLocalIP();
        this.nodeStatus = NodeStatus.ALIVE;
        this.nodeID = this.localIP+" "+System.currentTimeMillis();
        this.isRunning = true;
    }

    public boolean joinMmp(){
        Socket introducer = null;
        try{
            introducer = new Socket(this.introducerIP, this.portNum);
        }catch(IOException e){
            System.out.println("Cannot connect to Introducer. Rejoining not available.");
            return false;
        }
        System.out.println("Introducer Connected. Getting up-to-date mmp list from it...");
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
        System.out.println("Input/Ouput Stream inited");
        String line = null;
        try {
            toIntrocucer.println(this.nodeID);
            toIntrocucer.flush();
            while ((line = fromIntroducer.readLine()) != null) {
                System.out.println(line+"updated from introducer");
                this.memberList.put(line, NodeStatus.ALIVE);
            }
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        System.out.println("Member list updated from introducer!");
        return true;
    }

    public void launch(){
        MmpReceiver mmpReceiver = new MmpReceiver(this.socket);
        mmpReceiver.setDaemon(true);
        mmpReceiver.run();
        System.out.println("Receiver running in background");
        MmpSender mmpSender = new MmpSender(this.socket);
        mmpSender.setDaemon(true);
        mmpSender.run();
        System.out.println("Sender running in background");
        //command line
        while(this.isRunning){
            try {
                System.out.println("Available cmds: status, decommission");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String command = br.readLine();
                if(command.equals("status")){
                    this.printMemberList();
                }else if(command.equals("decommission")){
                    this.isRunning = false;
                }else{
                    System.out.println("Wrong cmd, try again");
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
        System.out.println("sender & receiver terminated.");
        this.socket.close();
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    public DatagramPacket receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
        this.socket.receive(packet);
        return packet;
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
            if(mmpServer.joinMmp()) {
                mmpServer.launch();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
