// Author: A0114171W

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

class FileReceiver {

    int portNo;
    PacketHandler handler;

    public static void main(String[] args) {
        // check if the number of command line argument is 1
        if (args.length != 1) {
            System.out.println("Usage: java FileReceiver port");
            System.exit(1);
        }

        FileReceiver receiver = new FileReceiver(args[0]);

        try {
            receiver.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public FileReceiver(String localPort) {
        portNo = Integer.parseInt(localPort);
        handler = new PacketHandler();
    }

    void receive() throws IOException {
        InetAddress ipAddress = InetAddress.getByName("localhost");
        DatagramSocket socket = new DatagramSocket(portNo);

        byte[] inBuffer = new byte[1000];
        DatagramPacket pktIn = new DatagramPacket(inBuffer, inBuffer.length);

        socket.receive(pktIn);

        byte[] outBuffer = handler.createSynPacket(new byte[0], 0, 0);
        DatagramPacket pktOut = new DatagramPacket(
                outBuffer, outBuffer.length, ipAddress, pktIn.getPort());

        socket.setSoTimeout(1);
        while (!handler.isGood(pktIn.getData())) {
            socket.send(pktOut);
            try {
                socket.receive(pktIn);
            } catch (SocketTimeoutException e) {
                continue;
            }
        }

        byte[] packet = pktIn.getData();
        byte[] data = handler.getPayload(packet);
        String fileName = new String(data);

        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int ackNo = 0;

        while (true) {
            ackNo = handler.getSeqNo(packet) + data.length;
            outBuffer = handler.createAckPacket(ackNo);
            pktOut = new DatagramPacket(
                        outBuffer, outBuffer.length, ipAddress, pktIn.getPort());
            do {
                socket.send(pktOut);
                try {
                    socket.receive(pktIn);
                } catch (SocketTimeoutException e) {
                    continue;
                }
            } while (handler.isCorruptedOrDuplicate(pktIn.getData(), ackNo));

            if (handler.isFin(packet)) {
                outBuffer = handler.createAckFinPacket(ackNo);
                pktOut = new DatagramPacket(
                        outBuffer, outBuffer.length, ipAddress, pktIn.getPort());
                socket.send(pktOut);
                System.out.println(fileName + " received.");
                break;
            }
            packet = pktIn.getData();
            data = handler.getPayload(packet);
            bos.write(data);
        }
        socket.close();
        bos.close();
    }
}
