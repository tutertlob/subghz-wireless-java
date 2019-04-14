package com.github.tutertlob.lazurite;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.DataPacketInterface;

import java.util.Arrays;
import java.lang.StringBuilder;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

public final class LazData extends LazuritePacket implements DataPacketInterface {

	private final byte[] data;

	public LazData(ByteBuffer packetBytes) {
		super(packetBytes);
		data = new byte[packetBytes.remaining()];
		packetBytes.get(data, 0, packetBytes.remaining());
	}

	public LazData(byte[] data, boolean fragmented) {
		super(fragmented, false);
		this.data = data.clone();
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
				.append("Lazurite Data packet:")
				.append("\n Data size: ").append(getDataSize())
				.append("\n Data: ").append(Hex.encodeHexString(data))
				.append("\n");
		return builder.toString();
	}

}
