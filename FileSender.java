// Author: A0114171W

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class FileSender {

    String srcFileName, hostname, destFileName;
    int portNo;
    PacketHandler handler;

    public static void main(String[] args) {

        // check if the number of command line argument is 3
        if (args.length != 3) {
            System.out.println("Usage: java FileSender <path/filename> "
                    + "<rcvPort> <rcvFileName>");
            System.exit(1);
        }

        FileSender sender = new FileSender(args[0], "localhost", args[1], args[2]);

        try {
        	Stopwatch stopwatch = new Stopwatch();
        	stopwatch.start();
            sender.send();
            stopwatch.stop();
            System.out.println(args[0] + " is successfully sent as " + args[2]);
            long minutes = stopwatch.getElapsedTimeSecs() / 60;
            long seconds = stopwatch.getElapsedTimeSecs() % 60;
            System.out.println("Elapsed: " + minutes + " minutes "
            			+ seconds + " seconds.");
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
        handler = new PacketHandler();
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

        byte[] dataBuffer = new byte[PacketHandler.MAX_PAYLOAD_LENGTH];

        FileInputStream fis = new FileInputStream(srcFileName);
        BufferedInputStream bis = new BufferedInputStream(fis);

        int seqNo = setupConnection(rcvAddress, socket, destFileName);
        int bytesRead;

        while ((bytesRead = bis.read(dataBuffer)) > 0) {
            byte[] outBuffer =
            		handler.createOutgoingPacket(dataBuffer, bytesRead, seqNo);
            DatagramPacket pktOut =
            		new DatagramPacket(outBuffer, outBuffer.length, rcvAddress, portNo);

            byte[] inBuffer = new byte[1000];
            DatagramPacket pktIn = new DatagramPacket(inBuffer, inBuffer.length);

            seqNo += bytesRead;
 			do {
 				socket.send(pktOut);
 				socket.receive(pktIn);
 				System.out.println("Received");
 			} while (handler.isCorruptedReply(pktIn.getData(), seqNo));
        }
        closeConnection(rcvAddress, socket, seqNo);
        socket.close();
        bis.close();
    }

	private void closeConnection(InetAddress rcvAddress, DatagramSocket socket,
			int seqNo) {
		byte[] outBuffer =
				handler.createFinPacket(new byte[0], 0, seqNo);
		DatagramPacket pktOut =
				new DatagramPacket(outBuffer, outBuffer.length, rcvAddress, portNo);

		byte[] inBuffer = new byte[1000];
		DatagramPacket pktIn = new DatagramPacket(inBuffer, inBuffer.length);

		try {
			do {
				System.out.println("Sending FIN");
				socket.send(pktOut);
				socket.receive(pktIn);
			} while (handler.isCorruptedReply(pktIn.getData(), seqNo));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int setupConnection(InetAddress rcvAddress, DatagramSocket socket,
			String fileName) throws IOException {
		byte[] data = fileName.getBytes();

		int seqNo = 0;
		byte[] outBuffer =
				handler.createSynPacket(data, data.length, seqNo);
		DatagramPacket pktOut =
				new DatagramPacket(outBuffer, outBuffer.length, rcvAddress, portNo);

		byte[] inBuffer = new byte[1000];
		DatagramPacket pktIn = new DatagramPacket(inBuffer, inBuffer.length);

		seqNo += data.length;
		do {
			socket.send(pktOut);
			socket.receive(pktIn);
			System.out.println("Syncing...");
		} while (handler.isCorruptedReply(pktIn.getData(), seqNo));

		return seqNo;
	}
}
