import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class MmpJoiner extends Thread {
    private final int port;
    private final AtomicBoolean isRunning;
    private Map<String, String> memberList;

    public MmpJoiner(Map<String, String> memberList, int port) {
        this.isRunning=new AtomicBoolean(false);
        this.port=port;
        this.memberList = memberList;
    }

    public void terminate() {
        this.isRunning.set(true);
    }

    @Override
    public void run() {
        System.out.println("mmpJoiner thread started");
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
            try {
                inputReader = new Scanner(new InputStreamReader(joinRequest.getInputStream()));
                inputReader.useDelimiter("\n");
            } catch (IOException e) {
                System.err.println("[ERROR] Error creating input stream to introducer");
                return;
            }
            PrintWriter outputWriter = null;
            try {
                outputWriter = new PrintWriter(new OutputStreamWriter(joinRequest.getOutputStream()));
            } catch (IOException e) {
                System.err.println("[ERROR] Error creating input stream from socket");
                return;
            }

            String joinMsg=inputReader.nextLine();
            System.out.println(joinMsg);
            String senderID = joinMsg.split(" ")[0];
            String senderTimeStamp = joinMsg.split(" ")[1];
            System.out.println("[JOINER THREAD] join requested by : " + senderID);
            for (String member : memberList.keySet()) {
                outputWriter.println(member+" "+memberList.get(member));
            }

            outputWriter.flush();

            try {
                joinRequest.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            memberList.put(senderID, senderTimeStamp);
            System.out.println(senderID + " added to membership list");
        }

        try {
            tcp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
