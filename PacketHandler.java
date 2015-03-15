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

		BitSet types = new BitSet(3);
		types.set(0, isSyn);
		types.set(1, isAck);
		types.set(2, isFin);
		byte[] pktTypes = types.toByteArray();

		short dataLength = (short)argDataLength;

        byte[] buffer = ByteBuffer.allocate(OFFSET_CHECKSUM)
        				.putInt(seqNo)
        				.putInt(ackNo)
        				.put(pktTypes)
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
		Checksum crc32 = new CRC32();
        crc32.update(packet, 0, OFFSET_CHECKSUM);
        long rawChecksum = crc32.getValue();
        long checksum = ByteBuffer.wrap(packet, OFFSET_CHECKSUM, 8).getLong();
        System.out.println("Received cs: " + checksum);
        System.out.println("Calculated cs: " + rawChecksum);
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

	public boolean isSyn(byte[] packet) {
		return getType(packet, 0);
	}

	public boolean isAck(byte[] packet) {
		return getType(packet, 1);
	}

	public boolean isFin(byte[] packet) {
		return getType(packet, 2);
	}

	private boolean getType(byte[] packet, int index) {
		byte[] header = ByteBuffer.wrap(packet, 0, HEADER_LENGTH).array();
		byte[] rawTypes = ByteBuffer.wrap(header, OFFSET_TYPES, 1).array();
		BitSet types = BitSet.valueOf(rawTypes);
		return types.get(index);
	}
}
