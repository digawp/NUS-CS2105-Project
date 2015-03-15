import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * This class handles sending and receiving of packets
 * @author Diga W
 *
 */
class PacketHandler {

	public final static int MAX_PAYLOAD_LENGTH = 981;

	private final static int HEADER_LENGTH = 11;
	private final static int OFFSET_SEQ_NO = 0;
	private final static int OFFSET_ACK_NO = 4;
	private final static int OFFSET_TYPES = 8;
	private final static int OFFSET_DATA_LENGTH = 9;
	private final static int OFFSET_DATA = 11;
	private final static int OFFSET_CHECKSUM = 992;

	public byte[] createOutgoingPacket(byte[] data, int argDataLength, int seqNo) {
		return createPacket(data, argDataLength, seqNo, 0, false, false, false);
	}

	public byte[] createSynPacket(byte[] data, int argDataLength, int seqNo) {
		return createPacket(data, argDataLength, seqNo, 0, true, false, false);
	}

	public byte[] createAckPacket(int ackNo) {
		return createPacket(new byte[0], 0, 0, ackNo, false, true, false);
	}

	public byte[] createFinPacket(byte[] data, int argDataLength, int seqNo) {
		return createPacket(data, argDataLength, seqNo, 0, false, false, true);
	}

	/**
	 * Create an outgoing packet with the arguments given
	 * @param data
	 * @param isAck
	 * @return the packet to be sent, of length 1000 bytes
	 */
	private byte[] createPacket(byte[] data, int argDataLength,
			int seqNo, int ackNo,
			boolean isSyn, boolean isAck, boolean isFin) {
		if (data.length > MAX_PAYLOAD_LENGTH || argDataLength > MAX_PAYLOAD_LENGTH) {
			throw new IllegalArgumentException(
					"Data length cannot exceed MAX_PAYLOAD_LENGTH");
		}

		byte types = 0;
		types = (byte) (isSyn ? types | 1 : types | 0);
		types = (byte) (isAck ? types | 2 : types | 0);
		types = (byte) (isFin ? types | 4 : types | 0);

		short dataLength = (short)argDataLength;
		System.out.println("Created data length: " + dataLength);

        byte[] buffer = ByteBuffer.allocate(OFFSET_CHECKSUM)
        				.putInt(seqNo)
        				.putInt(ackNo)
        				.put(types)
        				.putShort(dataLength)
        				.put(data)
        				.array();

        Checksum crc32 = new CRC32();
        crc32.update(buffer, 0, OFFSET_CHECKSUM);
        long rawChecksum = crc32.getValue();
        System.out.println("Created cs: " + rawChecksum);
        byte[] checksum = ByteBuffer.allocate(8).putLong(rawChecksum).array();

        byte[] packet = new byte[1000];
        System.arraycopy(buffer, 0, packet, 0, OFFSET_CHECKSUM);
        System.arraycopy(checksum, 0, packet, OFFSET_CHECKSUM, 8);

        return packet;
	}

	/**
	 * Get the payload of the packet
	 * @param packet
	 * @return the payload
	 */
	public byte[] getPayload(byte[] packet) {
		short dataLength = ByteBuffer.wrap(packet, OFFSET_DATA_LENGTH, 2).getShort();
		System.out.println("Received data length: " + dataLength);
		byte[] data = new byte[dataLength];
		ByteBuffer.wrap(packet, OFFSET_DATA, dataLength).get(data);
		return data;
	}

	public boolean isCorrupted(byte[] packet, int ackNo) {
		return !(isGood(packet) && isCorrectAck(packet, ackNo));
	}

	/**
	 * Checks whether the packet is corrupted or not by checking the checksum
	 * @param packet
	 * @return false if packet is corrupted, true otherwise
	 */
	public boolean isGood(byte[] packet) {
		Checksum crc32 = new CRC32();
        crc32.update(packet, 0, OFFSET_CHECKSUM);
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
	private boolean isCorrectAck(byte[] packet, int ackNo) {
		int rawAckNo = getAckNo(packet);
		return rawAckNo == ackNo;
	}

	/**
	 * Get the sequence number from the header of the packet
	 * @param packet
	 * @return
	 */
	public int getSeqNo(byte[] packet) {
		int seqNo = ByteBuffer.wrap(packet, OFFSET_SEQ_NO, 4).getInt();
		return seqNo;
	}

	/**
	 * Get the ACK number from the header of the packet
	 * @param packet
	 * @return
	 */
	public int getAckNo(byte[] packet) {
		int ackNo = ByteBuffer.wrap(packet, OFFSET_ACK_NO, 4).getInt();
		return ackNo;
	}

	public boolean isSyn(byte[] packet) {
		return getType(packet, 1);
	}

	public boolean isAck(byte[] packet) {
		return getType(packet, 2);
	}

	public boolean isFin(byte[] packet) {
		return getType(packet, 4);
	}

	private boolean getType(byte[] packet, int mask) {
		byte rawTypes = ByteBuffer.wrap(packet, OFFSET_TYPES, 1).get();
		return (rawTypes & mask) == mask;
	}
}
