package network;

import com.google.gson.Gson;
import utils.EmailUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class NetworkLayerJSON {
    private Socket dataSocket;
    private Scanner inputStream;
    private PrintWriter outputStream;
    private String hostname;
    private int port;

    private static final Gson gson = new Gson();


    public NetworkLayerJSON(String hostname, int port){
        this.hostname = hostname;
        this.port = port;
    }


    public NetworkLayerJSON(Socket dataSocket) throws IOException {
        if(dataSocket == null){
            throw new IllegalArgumentException("Socket cannot be null");
        }
        this.dataSocket = dataSocket;
        setStreams();
    }

    private void setStreams() throws IOException {
        this.inputStream = new Scanner(dataSocket.getInputStream());
        this.outputStream = new PrintWriter(dataSocket.getOutputStream());
    }


    public void connect() throws IOException {
        this.dataSocket = new Socket(EmailUtils.HOSTNAME, EmailUtils.PORT);
        setStreams();
    }


    public void send(String message){
        outputStream.println(message);
        outputStream.flush();
    }


    public String receive(){
        return inputStream.nextLine();
    }

    /**
     * Send any JSON‑serializable object (e.g. request or response).
     * It will be turned into one line of JSON + '\n'.
     */
    /*public void sendJson(Object obj) throws IOException {
        String json = MAPPER.writeValueAsString(obj);
        outputStream.println(json);
    }


    public <T> T receiveJson(Class<T> clazz) throws IOException {
        String line = inputStream.nextLine();
        return MAPPER.readValue(line, clazz);
    }*/

    /** Gracefully close streams and socket. */
    public void disconnect() throws IOException {
        if(this.dataSocket != null) {
            this.outputStream.close();
            this.inputStream.close();
            this.dataSocket.close();
        }
    }
}
