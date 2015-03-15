// Author: A0114171W

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

        while (!handler.isGood(pktIn.getData())) {
			socket.send(pktOut);
			socket.receive(pktIn);
		}

        byte[] packet = pktIn.getData();
        byte[] data = handler.getPayload(packet);
        String fileName = new String(data);

        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int ackNo = handler.getSeqNo(packet) + data.length;
        outBuffer = handler.createAckPacket(ackNo);
        pktOut = new DatagramPacket(outBuffer, outBuffer.length, ipAddress, pktIn.getPort());

        while (true) {
        	do {
        		socket.send(pktOut);
            	socket.receive(pktIn);
        	} while (handler.isCorruptedOrDuplicate(pktIn.getData(), ackNo));

        	packet = pktIn.getData();
            // Write the data to disk
        	data = handler.getPayload(packet);
            bos.write(data);

            ackNo = handler.getSeqNo(packet) + data.length;
            outBuffer = handler.createAckPacket(ackNo);
            pktOut = new DatagramPacket(
            		outBuffer, outBuffer.length, ipAddress, pktIn.getPort());

            if (handler.isFin(packet)) {
            	System.out.println("Sending FIN ACK");
            	socket.send(pktOut);
                System.out.println(fileName + " received.");
                break;
            }
        }
        socket.close();
        bos.close();
    }
}
