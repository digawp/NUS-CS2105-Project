import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * This class handles packets management
 * @author Diga W
 *
 */
class PacketHandler {

	public final static int MAX_PAYLOAD_LENGTH = 981;

	private final static int HEADER_LENGTH = 11;
	private final static int OFFSET_SEQ_NO = 0;
	private final static int OFFSET_ACK_NO = 4;
	private final static int OFFSET_FLAGS = 8;
	private final static int OFFSET_DATA_LENGTH = 9;
	private final static int OFFSET_DATA = 11;
	private final static int OFFSET_CHECKSUM = 992;

	/**
	 * Create a general outgoing packet
	 * @param data
	 * @param argDataLength
	 * @param seqNo
	 * @return
	 */
	public byte[] createOutgoingPacket(
			byte[] data, int argDataLength, int seqNo) {
		return createPacket(data, argDataLength, seqNo, 0, false, false, false);
	}

	/**
	 * Create a SYN-flagged packet
	 * @param data
	 * @param argDataLength
	 * @param seqNo
	 * @return
	 */
	public byte[] createSynPacket(byte[] data, int argDataLength, int seqNo) {
		return createPacket(data, argDataLength, seqNo, 0, true, false, false);
	}

	/**
	 * Create a ACK-flagged packet
	 * @param ackNo
	 * @return
	 */
	public byte[] createAckPacket(int ackNo) {
		return createPacket(new byte[0], 0, 0, ackNo, false, true, false);
	}

	/**
	 * Create a FIN-flagged packet
	 * @param data
	 * @param argDataLength
	 * @param seqNo
	 * @return
	 */
	public byte[] createFinPacket(int seqNo) {
		return createPacket(new byte[0], 0, seqNo, 0, false, false, true);
	}

	public byte[] createAckFinPacket(int ackNo) {
		return createPacket(new byte[0], 0, 0, ackNo, false, true, true);
	}
	/**
	 * The generic method to create an outgoing packet with the arguments given
	 * @param data
	 * @param argDataLength
	 * @param seqNo
	 * @param ackNo
	 * @param isSyn
	 * @param isAck
	 * @param isFin
	 * @return
	 */
	private byte[] createPacket(byte[] data, int argDataLength,
			int seqNo, int ackNo,
			boolean isSyn, boolean isAck, boolean isFin) {
		if (data.length > MAX_PAYLOAD_LENGTH ||
				argDataLength > MAX_PAYLOAD_LENGTH) {
			throw new IllegalArgumentException(
					"Data length cannot exceed MAX_PAYLOAD_LENGTH");
		}

		byte flags = 0;
		flags = (byte) (isSyn ? flags | 1 : flags | 0);
		flags = (byte) (isAck ? flags | 2 : flags | 0);
		flags = (byte) (isFin ? flags | 4 : flags | 0);

		short dataLength = (short)argDataLength;

        byte[] buffer = ByteBuffer.allocate(OFFSET_CHECKSUM)
        				.putInt(seqNo)
        				.putInt(ackNo)
        				.put(flags)
        				.putShort(dataLength)
        				.put(data)
        				.array();

        Checksum crc32 = new CRC32();
        crc32.update(buffer, 0, OFFSET_CHECKSUM);
        long rawChecksum = crc32.getValue();
        byte[] checksum = ByteBuffer.allocate(8).putLong(rawChecksum).array();

        byte[] packet = new byte[1000];
        System.arraycopy(buffer, 0, packet, 0, OFFSET_CHECKSUM);
        System.arraycopy(checksum, 0, packet, OFFSET_CHECKSUM, 8);

        return packet;
	}

	/**
	 * Get the payload of the packet
	 * @param packet
	 * @return
	 */
	public byte[] getPayload(byte[] packet) {
		short dataLength =
				ByteBuffer.wrap(packet, OFFSET_DATA_LENGTH, 2).getShort();
		System.out.println("Received data length: " + dataLength);
		byte[] data = new byte[dataLength];
		ByteBuffer.wrap(packet, OFFSET_DATA, dataLength).get(data);
		return data;
	}

	/**
	 * Checks if the packet's checksum matches the content and if
	 * the acknowledgement number is correct
	 * @param packet
	 * @param ackNo
	 * @return
	 */
	public boolean isCorruptedReply(byte[] packet, int ackNo) {
		return !(isGood(packet) && isCorrectAck(packet, ackNo));
	}

	/**
	 * Checks if the packet's checksum matches the content and if
	 * the sequence number is not duplicate of previously received packet
	 * @param packet
	 * @param seqNo
	 * @return
	 */
	public boolean isCorruptedOrDuplicate(byte[] packet, int seqNo) {
		return !(isGood(packet) && isCorrectSeq(packet, seqNo));
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
	 * Checks if the sequence number on the packet is the same as supplied
	 * @param packet
	 * @param seqNo
	 * @return
	 */
	private boolean isCorrectSeq(byte[] packet, int seqNo) {
		int rawSeqNo = getSeqNo(packet);
		return rawSeqNo == seqNo;
	}

	/**
	 * Checks if the ACK number on the packet is the same as supplied
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

	/**
	 * Check if the SYN flag is set
	 * @param packet
	 * @return
	 */
	public boolean isSyn(byte[] packet) {
		return getFlag(packet, 1);
	}

	/**
	 * Check if the ACK flag is set
	 * @param packet
	 * @return
	 */
	public boolean isAck(byte[] packet) {
		return getFlag(packet, 2);
	}

	/**
	 * Check if the FIN flag is set
	 * @param packet
	 * @return
	 */
	public boolean isFin(byte[] packet) {
		return getFlag(packet, 4);
	}

	/**
	 * Check if the flag is true by masking the flag of the packet with
	 * the supplied mask
	 * @param packet
	 * @param mask
	 * @return
	 */
	private boolean getFlag(byte[] packet, int mask) {
		byte rawTypes = ByteBuffer.wrap(packet, OFFSET_FLAGS, 1).get();
		return (rawTypes & mask) == mask;
	}
}
