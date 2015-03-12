import java.net.InetAddress;

/**
 * @author Diga W
 * This class handles sending and receiving of packets
 *
 */
class PacketHandler {

	private InetAddress ipAddress;
	private int portNo;

	/**
	 * Constructor
	 *
	 * @param ipAddress
	 * @param portNo
	 */
	public PacketHandler(InetAddress ipAddress, int portNo) {
		this.ipAddress = ipAddress;
		this.portNo = portNo;
	}

	/**
	 * Handles sending of data
	 * @param data
	 */
	public void send(byte[] data) {

	}

	/**
	 * Handles receiving of data
	 */
	public void receive() {

	}
}
