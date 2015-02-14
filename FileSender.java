// Author: A0114171W

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

class FileSender {

    String srcFileName, hostname, destFileName;
    int portNo;

    public static void main(String[] args) {

        // check if the number of command line argument is 4
        if (args.length != 4) {
            System.out.println("Usage: java FileSender <path/filename> "
                    + "<rcvHostName> <rcvPort> <rcvFileName>");
            System.exit(1);
        }

        FileSender sender = new FileSender(args[0], args[1], args[2], args[3]);

        try {
            sender.send();
            System.out.println(args[0] + " is successfully sent as " + args[3]);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public FileSender(String fileToOpen, String host, String port,
            String rcvFileName) {
        srcFileName = fileToOpen;
        hostname = host;
        portNo = Integer.parseInt(port);
        destFileName = rcvFileName;
    }

    /**
     * Packet format: 4 parts. First 4 bytes indicate length of file name,
     * followed by the file name. Third part is another 4 bytes indicating the
     * length of payload, followed by the payload.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    void send() throws IOException, InterruptedException {
        InetAddress rcvAddress = InetAddress.getByName(hostname);
        DatagramSocket socket = new DatagramSocket();

        byte[] outBuffer = new byte[1000];

        int nameLength = allocateHeaderForFileName(outBuffer, destFileName);

        int dataOffset = nameLength;
        int lengthForData = outBuffer.length - nameLength - 4;

        byte[] dataBuffer = new byte[lengthForData];
        FileInputStream fis = new FileInputStream(srcFileName);
        BufferedInputStream bis = new BufferedInputStream(fis);

        int bytesRead;
        while ((bytesRead = bis.read(dataBuffer)) > 0) {
            putDataToBuffer(outBuffer, dataOffset, dataBuffer, bytesRead);

            DatagramPacket pkt = new DatagramPacket(outBuffer,
                    outBuffer.length, rcvAddress, portNo);
            socket.send(pkt);
            Thread.sleep(10);
        }

        // For corner case where
        // the size of last packet == size of array available for data,
        // which may result in receiver to fail to terminate
        if (bytesRead == lengthForData) {
            putDataToBuffer(outBuffer, dataOffset, dataBuffer, 0);

            DatagramPacket pkt = new DatagramPacket(outBuffer,
                    outBuffer.length, rcvAddress, portNo);
            socket.send(pkt);
        }

        socket.close();
        bis.close();
    }

    /**
     * Puts data in dataBuffer to outBuffer with from index dataOffset for
     * bytesRead byte
     *
     * @param outBuffer
     *            The buffer to put the data into
     * @param dataOffset
     *            The offset to put the data into
     * @param dataBuffer
     *            The data to be put
     * @param bytesRead
     *            The amount of data to be put
     */
    private void putDataToBuffer(byte[] outBuffer, int dataOffset,
            byte[] dataBuffer, int bytesRead) {
        byte[] bytesReadInByte = ByteBuffer.allocate(4).putInt(bytesRead)
                .array();
        System.arraycopy(bytesReadInByte, 0, outBuffer, dataOffset, 4);
        System.arraycopy(dataBuffer, 0, outBuffer, dataOffset + 4, bytesRead);
    }

    /**
     * Inserts the packet header for file name
     *
     * @param sendBuffer
     *            The array buffer to be sent
     * @param fileName
     *            The name of the file
     * @return the number of bytes used for file name header
     */
    private int allocateHeaderForFileName(byte[] sendBuffer, String fileName) {
        byte[] fileNameInByte = destFileName.getBytes();
        int nameLength = fileNameInByte.length;
        byte[] nameLengthInByte = ByteBuffer.allocate(4).putInt(nameLength)
                .array();

        System.arraycopy(nameLengthInByte, 0, sendBuffer, 0, 4);
        System.arraycopy(fileNameInByte, 0, sendBuffer, 4, nameLength);
        return nameLength + 4;
    }
}
