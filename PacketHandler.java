import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * This class handles sending and receiving of packets
 * @author Diga W
 *
 */
class PacketHandler {

	public final static int MAX_PAYLOAD_LENGTH = 982;

	private final static int HEADER_LENGTH = 10;
	private final static int OFFSET_SEQ_NO = 0;
	private final static int OFFSET_ACK_NO = 4;
	private final static int OFFSET_DATA_LENGTH = 8;
	private final static int OFFSET_DATA = 10;
	private final static int OFFSET_CHECKSUM = 992;

	/**
	 * Create an outgoing packet with the arguments given
	 * @param data
	 * @param isAck
	 * @return the packet to be sent, of length 1000 bytes
	 */
	public byte[] createOutgoingPacket(byte[] data, int argDataLength,
			int seqNo, int ackNo) {
		if (data.length > MAX_PAYLOAD_LENGTH || argDataLength > MAX_PAYLOAD_LENGTH) {
			throw new IllegalArgumentException(
					"Data length cannot exceed MAX_PAYLOAD_LENGTH");
		}

		short dataLength = (short)argDataLength;

		byte[] buffer = new byte[992];

        byte[] header = ByteBuffer.allocate(HEADER_LENGTH)
        				.putInt(seqNo)
        				.putInt(ackNo)
        				.putShort(dataLength)
        				.array();

        Checksum crc32 = new CRC32();
        crc32.update(buffer, 0, OFFSET_CHECKSUM);
        long rawChecksum = crc32.getValue();
        byte[] checksum = ByteBuffer.allocate(8).putLong(rawChecksum).array();

        byte[] packet = new byte[1000];
        System.arraycopy(header, 0, packet, 0, HEADER_LENGTH);
        System.arraycopy(data, 0, packet, HEADER_LENGTH, dataLength);
        System.arraycopy(checksum, 0, packet, OFFSET_CHECKSUM, 8);
        return packet;
	}

	/**
	 * Get the payload of the packet
	 * @param packet
	 * @return the payload
	 */
	public byte[] getPayload(byte[] packet) {
		byte[] header = ByteBuffer.wrap(packet, 0, HEADER_LENGTH).array();
		short dataLength = ByteBuffer.wrap(header, OFFSET_DATA_LENGTH, 2).getShort();
		byte[] data = ByteBuffer.wrap(packet, OFFSET_DATA, dataLength).array();
		return data;
	}

	/**
	 * Checks whether the packet is corrupted or not by checking the checksum
	 * @param packet
	 * @return false if packet is corrupted, true otherwise
	 */
	public boolean isGood(byte[] packet) {
		byte[] pktWithoutChecksum = ByteBuffer.wrap(packet, 0, OFFSET_CHECKSUM).array();
		Checksum crc32 = new CRC32();
        crc32.update(pktWithoutChecksum, 0, OFFSET_CHECKSUM);
        long rawChecksum = crc32.getValue();
        long checksum = ByteBuffer.wrap(packet, OFFSET_CHECKSUM, 8).getLong();
        return rawChecksum == checksum;
	}

	/**
	 * Checks if the ACK number on the packet is the same as the one supplied
	 * @param packet
	 * @param ackNo
	 * @return
	 */
	public boolean isCorrectAck(byte[] packet, int ackNo) {
		int rawAckNo = getAckNo(packet);
		return rawAckNo == ackNo;
	}

	/**
	 * Get the sequence number from the header of the packet
	 * @param packet
	 * @return
	 */
	public int getSeqNo(byte[] packet) {
		byte[] header = ByteBuffer.wrap(packet, 0, HEADER_LENGTH).array();
		int seqNo = ByteBuffer.wrap(header, OFFSET_SEQ_NO, 4).getInt();
		return seqNo;
	}

	/**
	 * Get the ACK number from the header of the packet
	 * @param packet
	 * @return
	 */
	public int getAckNo(byte[] packet) {
		byte[] header = ByteBuffer.wrap(packet, 0, HEADER_LENGTH).array();
		int ackNo = ByteBuffer.wrap(header, OFFSET_ACK_NO, 4).getInt();
		return ackNo;
	}
}
