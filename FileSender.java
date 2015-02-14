// Author: 

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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
    }
    
    public FileSender(String fileToOpen, String host, String port, String rcvFileName) {
        srcFileName = fileToOpen;
        hostname = host;
        portNo = Integer.parseInt(port);
        destFileName = rcvFileName;
        
        // UDP transmission is unreliable. Sender may overrun
        // receiver if sending too fast, giving packet lost as a result.
        // In that sense, sender may need to pause once in a while.
        // E.g. Thread.sleep(1); // pause for 1 millisecond
    }
    
    void send() throws IOException {
    	InetAddress rcvAddress = InetAddress.getByName(hostname);
    	DatagramSocket socket = new DatagramSocket(portNo, rcvAddress);
		
		byte[] sendBuffer = new byte[1000];
		
		int nameLength = allocateHeaderForFileName(sendBuffer, destFileName);
		
		int dataOffset = nameLength;
		int lengthForData = sendBuffer.length - nameLength - 4;
		
		byte[] dataBuffer = new byte[lengthForData];
		FileInputStream fis = new FileInputStream(srcFileName);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		int bytesRead;
		while ((bytesRead = bis.read(dataBuffer)) > 0) {
			byte[] bytesReadInByte = ByteBuffer.allocate(4).putInt(nameLength).array();
			System.arraycopy(bytesReadInByte, 0, sendBuffer, dataOffset, 4);
			System.arraycopy(dataBuffer, 0, sendBuffer, dataOffset + 4, bytesRead);
			DatagramPacket pkt =
					new DatagramPacket(sendBuffer, sendBuffer.length, rcvAddress, portNo);
			socket.send(pkt);
		}
		socket.close();
		bis.close();
    }

    /**
     * Inserts the packet header for file name
     * @param sendBuffer The array buffer to be sent
     * @param fileName The name of the file
     * @return the number of bytes used for file name header
     */
	private int allocateHeaderForFileName(byte[] sendBuffer, String fileName) {
		byte[] fileNameInByte = destFileName.getBytes();
		int nameLength = fileNameInByte.length;
		byte[] nameLengthInByte = ByteBuffer.allocate(4).putInt(nameLength).array();
		
		System.arraycopy(nameLengthInByte, 0, sendBuffer, 0, 4);
		System.arraycopy(fileNameInByte, 0, sendBuffer, 4, nameLength);
		return nameLength + 4;
	}
}