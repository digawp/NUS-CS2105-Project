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

        byte[] outBuffer = handler.createOutgoingPacket(null, 0, 0, 0);
        DatagramPacket pktOut =
        		new DatagramPacket(outBuffer, outBuffer.length, ipAddress, portNo);

        socket.receive(pktIn);
        while (!handler.isGood(pktIn.getData())) {
				socket.send(pktOut);
				socket.receive(pktIn);
			}

        byte[] packet = pktIn.getData();
        String fileName = new String(handler.getPayload(packet));

        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int ackNo = handler.getSeqNo(packet);
        outBuffer = handler.createOutgoingPacket(null, 0, 0, ackNo);
        pktOut = new DatagramPacket(outBuffer, outBuffer.length, ipAddress, portNo);

        // TODO work on this
        while (true) {

        	socket.send(pktOut);
        	socket.receive(pktIn);
        	while (!handler.isGood(pktIn.getData())) {
        		socket.send(pktOut);
            	socket.receive(pktIn);
			}
        	packet = pktIn.getData();
            // Write the data to disk
        	byte[] data = handler.getPayload(packet);
            bos.write(data);

            // TODO: If last packet.
            if (data.length < PacketHandler.MAX_PAYLOAD_LENGTH) {
                System.out.println(fileName + " received.");
                break;
            }
        }
        socket.close();
        bos.close();
    }
}
