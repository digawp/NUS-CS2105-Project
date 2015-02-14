// Author: A0114171W

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

class FileReceiver {

    public DatagramSocket socket;
    public DatagramPacket pkt;
    int portNo;

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
    }

    void receive() throws IOException {
        byte[] inBuffer = new byte[1000];
        socket = new DatagramSocket(portNo);
        pkt = new DatagramPacket(inBuffer, inBuffer.length);

        while (true) {
            socket.receive(pkt);

            // Get length of file names
            ByteBuffer wrapper = ByteBuffer.wrap(pkt.getData(), 0, 4);
            int nameLength = wrapper.getInt();

            // Get file name
            String fileName = new String(pkt.getData(), 4, nameLength);

            int dataOffset = nameLength + 4;

            FileOutputStream fos = new FileOutputStream(fileName, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            // Get length of data payload
            wrapper = ByteBuffer.wrap(pkt.getData(), dataOffset, 4);
            int dataLength = wrapper.getInt();

            // Get the data payload
            wrapper = ByteBuffer.wrap(pkt.getData(), dataOffset + 4, dataLength);
            byte[] data = new byte[dataLength];
            wrapper.get(data);

            // Write the data to disk
            bos.write(data);
            bos.close();

            // If the packet is not full, it is the last packet.
            if (nameLength + dataLength + 8 < 1000) {
                System.out.println(fileName + " received.");
                break;
            }
        }
    }
}
