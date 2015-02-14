// Author: 

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
    	
    	socket.receive(pkt);
		
		ByteBuffer wrapper = ByteBuffer.wrap(pkt.getData(), 0, 4);
		int nameLength = wrapper.getInt();
		String fileName = new String(pkt.getData(), 4, nameLength + 4);
		
		FileOutputStream fos = new FileOutputStream(fileName);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
    	while (true) {
    		socket.receive(pkt);
    		wrapper = ByteBuffer.wrap(pkt.getData(), 0, 4);
    		nameLength = wrapper.getInt();
    		fileName = new String(pkt.getData(), 4, nameLength + 4);
    		
    		int dataOffset = nameLength + 4;
    		
    		wrapper = ByteBuffer.wrap(pkt.getData(), dataOffset, dataOffset + 4);
    		int dataLength = wrapper.getInt();
    		
    		wrapper = ByteBuffer.wrap(pkt.getData(), dataOffset + 4, dataOffset + 4 + dataLength); 
    		byte[] data = new byte[dataLength];
    		wrapper.get(data);
    		bos.write(data);
    		if (nameLength + dataLength + 8 < 1000) {
				break;
			}
    	}
    	bos.close();
    }
    
}
