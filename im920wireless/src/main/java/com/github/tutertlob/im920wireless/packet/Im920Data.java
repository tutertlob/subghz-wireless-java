package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.DataPacketInterface;

import java.util.Arrays;
import java.lang.StringBuilder;

public final class Im920Data extends Im920Packet implements DataPacketInterface {

	private final byte[] data;

	public Im920Data(byte[] data, boolean fragmented) {
		super(fragmented, false);
		this.data = data.clone();
	}

	public Im920Data(byte[] packetRaw) {
		super(packetRaw);
		byte[] data = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		this.data = data;
	}

	public Im920Data(Im920Frame frame, byte[] packetRaw) {
		super(frame, packetRaw);
		byte[] data = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		this.data = data;
	}

	public static int capacityOfData() {
		return capacityOfBody();
	}

	@Override
	public final PacketImplementation.Type getPacketType() {
		return Type.DATA;
	}

	@Override
	public byte[] getData() {
		return data.clone();
	}

	@Override
	public int getDataSize() {
		return data.length;
	}

	@Override
	byte[] getBodyBytes() {
		return data.clone();
	}

	@Override
	int getBodyLength() {
		return data.length;
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString())
				.append("IM920 Data packet:")
				.append("\n Data: ").append(Arrays.toString(data))
				.append("\n");
		return builder.toString();
	}

}
