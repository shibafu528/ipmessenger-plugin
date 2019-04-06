package org.jenkinsci.plugins;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class IPMessengerSendService {
    private static final String charset = "MS932";
    private static final int port = 2425;

    private final String userName;
    private final String fromHost;

    public IPMessengerSendService(String userName, String fromHost) {
        if (userName == null || "".equals(userName)) {
            this.userName = "jenkins-ci";
        } else {
            this.userName = userName;
        }
        this.fromHost = fromHost;
    }

    public void sendMsg(String message, String toHost, PrintStream logger) {
        message = createTeregram(0x00000020, message);
        sendPacket(message, toHost, logger);
    }

    public void sendNooperation(PrintStream logger) {
        String message = createTeregram(0x00000000, null);
        sendPacket(message, "255.255.255.255", logger);
    }

    private String createTeregram(int command, String message) {
        StringBuffer sb = new StringBuffer();
        sb.append(1);// ipmessenger protocol version
        sb.append(":");
        // packet serial number
        sb.append((int) Math.floor(Math.random() * Integer.MAX_VALUE));
        sb.append(":");
        sb.append(userName);// sender username
        sb.append(":");
        sb.append(fromHost);// sender hostname
        sb.append(":");
        sb.append(command);// command number
        sb.append(":");
        sb.append(message);
        return sb.toString();
    }

    private void sendPacket(String message, String toHost, PrintStream logger) {
        byte[] byteMsg = null;
        DatagramPacket packet = null;
        DatagramSocket socket = null;
        try {
            byteMsg = message.getBytes(charset);
            socket = new DatagramSocket(port);
            packet = new DatagramPacket(byteMsg, byteMsg.length,
                    InetAddress.getByName(toHost), port);
            socket.send(packet);
        } catch (UnsupportedEncodingException e) {
            logger.println(this.getClass().getSimpleName()
                    + ": UnsupportedEncodingException happened. You should change message template."
                    + e.getMessage());
        } catch (SocketException e) {
            logger.println(this.getClass().getSimpleName()
                    + ": SocketException happened"
                    + e.getMessage());
        } catch (UnknownHostException e) {
            logger.println(this.getClass().getSimpleName()
                    + ": UnknownHostException: " + toHost
                    + e.getMessage());
        } catch (IOException e) {
            logger.println(this.getClass().getSimpleName()
                    + ": IOException happened"
                    + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
