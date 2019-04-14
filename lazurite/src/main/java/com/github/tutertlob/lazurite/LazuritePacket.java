package com.github.tutertlob.lazurite;

import com.github.tutertlob.subghz.PacketImplementation;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.StringBuilder;
import java.nio.ByteBuffer;

public abstract class LazuritePacket extends PacketImplementation {

	private static final Logger logger = Logger.getLogger(LazuritePacket.class.getName());

	static final int PACKET_HEADER_SIZE = 1;

	static final int PACKET_FLAG_I = 0;

	static final int PACKET_TYPE_I = 0;

	static final int PACKET_BODY_I = PACKET_HEADER_SIZE;

	static final byte PACKET_FLAG_MASK = 0x18;

	static final byte PACKET_FLAG_MASK_FRAG = 0x10;

	static final byte PACKET_FLAG_MASK_ACK = 0x08;

	static final byte PACKET_TYPE_MASK = 0x07;

	static final int PACKET_MAX_LENGTH = LazuriteFrame.PAYLOAD_MAX_LENGTH-PACKET_HEADER_SIZE;

	private final boolean fragmented;

	private final boolean responseRequested;

	public LazuritePacket(ByteBuffer packetBytes) {
		byte header = packetBytes.get();
		fragmented = (header & PACKET_FLAG_MASK_FRAG) != 0 ? true : false;
		responseRequested = (header & PACKET_FLAG_MASK_ACK) != 0 ? true : false;
	}

	public LazuritePacket(boolean fragmented, boolean responseRequested) {
		this.fragmented = fragmented;
		this.responseRequested = responseRequested;
	}

	public static LazuritePacket newInstance(byte[] packetRaw) {
		Type type = Type.valueOf((byte)(packetRaw[PACKET_TYPE_I] & PACKET_TYPE_MASK));
		ByteBuffer raw = ByteBuffer.wrap(packetRaw);
		switch (type) {
			case DATA:
				return new LazData(raw);
			case COMMAND:
				return new LazCommand(raw);
			case ACK:
				return new LazAck(raw);
			case NOTICE:
				return new LazNotice(raw);
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
		byte type = getPacketType().id();
		byte flag = 0;
		flag |= fragmented ? PACKET_FLAG_MASK_FRAG : 0;
		flag |= responseRequested ? PACKET_FLAG_MASK_ACK : 0;
		header[PACKET_FLAG_I] = 0;
		header[PACKET_TYPE_I] = 0;
		header[PACKET_FLAG_I] |= flag;
		header[PACKET_TYPE_I] |= type;

		buf.put(header).put(getBodyBytes());

		return buf.array();
	}

	@Override
	public final int getPacketLength() {
		return sizeOfHeader() + getBodyLength();
	}

	public final boolean isFragmented() {
		return fragmented;
	}

	public final boolean isResponseRequested() {
		return responseRequested;
	}

	abstract byte[] getBodyBytes();

	abstract int getBodyLength();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Lazurite Packet Header:")
				.append("\n Flag response requested: ").append(responseRequested)
				.append("\n Flag fragmented:").append(fragmented)
				.append("\n");
		return builder.toString();
	}

}
