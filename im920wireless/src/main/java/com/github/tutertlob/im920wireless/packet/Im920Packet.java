package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.PacketImplementation;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.lang.StringBuilder;

public abstract class Im920Packet extends PacketImplementation {

	private static final Logger logger = Logger.getLogger(Im920Packet.class.getName());

	public static final int PACKET_HEADER_SIZE = 3;

	public static final int PACKET_MAX_LENGTH = Im920Frame.PAYLOAD_MAX_LENGTH-PACKET_HEADER_SIZE;

	protected static final int PACKET_LENGTH_I = 0;

	protected static final int PACKET_FLAG_I = 1;

	protected static final int PACKET_TYPE_I = 1;

	protected static final int PACKET_SEQ_NUM_I = 2;

	protected static final int PACKET_BODY_I = PACKET_HEADER_SIZE;

	protected static final byte PACKET_LENGTH_MASK = 0x3F;

	protected static final byte PACKET_FLAG_MASK = 0x18;

	protected static final byte PACKET_FLAG_MASK_FRAG = 0x10;

	protected static final byte PACKET_FLAG_MASK_ACK = 0x08;

	protected static final byte PACKET_TYPE_MASK = 0x07;

	private final int length;

	private boolean fragmented = false;

	private boolean acknoledgement = false;

	private byte seq = 0;

	private final Im920Frame frame;

	public Im920Packet(byte[] packetRaw) {
		super();
		length = packetRaw[PACKET_LENGTH_I];
		byte flag = packetRaw[PACKET_FLAG_I];
		fragmented = (flag & PACKET_FLAG_MASK_FRAG) != 0 ? true : false;
		acknoledgement = (flag & PACKET_FLAG_MASK_ACK) != 0 ? true : false;
		seq = packetRaw[PACKET_SEQ_NUM_I];
		frame = null;
		assert length == getPacketLength();
	}

	public Im920Packet(boolean fragmented, boolean responseRequested) {
		super();
		this.fragmented = fragmented;
		this.acknoledgement = responseRequested;
		this.length = getBodyLength();
		frame = null;
	}

	public Im920Packet(Im920Frame frame, byte[] packetRaw) {
		super();
		length = packetRaw[PACKET_LENGTH_I];
		byte flag = packetRaw[PACKET_FLAG_I];
		fragmented = (flag & PACKET_FLAG_MASK_FRAG) != 0 ? true : false;
		acknoledgement = (flag & PACKET_FLAG_MASK_ACK) != 0 ? true : false;
		seq = packetRaw[PACKET_SEQ_NUM_I];
		this.frame = frame;
	}

	public static Im920Packet newInstance(Im920Frame frame, byte[] packetRaw) {
		Type type = Type.valueOf((byte)(packetRaw[PACKET_TYPE_I] & PACKET_TYPE_MASK));
		switch (type) {
			case DATA:
				return new Im920Data(frame, packetRaw);
			case COMMAND:
				return new Im920Command(frame, packetRaw);
			case ACK:
				return new Im920Ack(frame, packetRaw);
			case NOTICE:
				return new Im920Notice(frame, packetRaw);
			default:
				logger.log(Level.WARNING, "Unknown packet type.");
				throw new IllegalArgumentException("Unknown packet type.");
		}
	}

	static int sizeOfHeader() {
		return PACKET_HEADER_SIZE;
	}

	static int capacityOfBody() {
		return PACKET_MAX_LENGTH;
	}

	@Override
	public final byte[] getPacketBytes() {
		int size = sizeOfHeader() + getBodyLength();
		ByteBuffer buf = ByteBuffer.allocate(size);

		byte[] header = new byte[sizeOfHeader()];
		byte length = (byte)getPacketLength();
		header[PACKET_LENGTH_I] = length;

		byte type = getPacketType().id();
		byte flag = 0;
		flag |= fragmented ? PACKET_FLAG_MASK_FRAG : 0;
		flag |= acknoledgement ? PACKET_FLAG_MASK_ACK : 0;
		header[PACKET_FLAG_I] = 0;
		header[PACKET_TYPE_I] = 0;
		header[PACKET_FLAG_I] |= flag;
		header[PACKET_TYPE_I] |= type;

		byte seq = (byte)getSeqNum();
		header[PACKET_SEQ_NUM_I] = seq;

		buf.put(header).put(getBodyBytes());

		return buf.array();
	}

	@Override
	public final int getPacketLength() {
		return sizeOfHeader() + getBodyLength();
	}

	public final Im920Frame getFrame() {
		return frame;
	}

	public final byte getSeqNum() {
		return seq;
	}

	public final boolean isFragmented() {
		return fragmented;
	}

	public final boolean isResponseRequested() {
		return acknoledgement;
	}

	public final void setFragment(boolean fragmented) {
		this.fragmented  = fragmented;
	}

	public final void setResponseRequest(boolean requested) {
		this.acknoledgement = requested;
	}

	public final void setSeqNum(byte seqNo) {
		this.seq = seqNo;
	}

	abstract byte[] getBodyBytes();

	abstract int getBodyLength();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IM920 Packet Header:")
				.append("\n Length: ").append(getPacketLength())
				.append("\n Flag response requested: ").append(acknoledgement)
				.append("\n Flag fragmented: ").append(fragmented)
				.append("\n Sequence Number: ").append(seq)
				.append("\n");
		return builder.toString();
	}

}
