package com.github.tutertlob.lazurite;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.AckPacketInterface;

import java.nio.ByteBuffer;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;

public final class LazAck extends LazuritePacket implements AckPacketInterface {

	static final int COMMAND_SIZE = 1;

	private final byte cmd;

	private final String response;

	public LazAck(ByteBuffer packetBytes) {
		super(packetBytes);
		cmd = packetBytes.get();
		byte[] stringByte = new byte[packetBytes.remaining()];
		packetBytes.get(stringByte, 0, packetBytes.remaining());
		response = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public LazAck(byte command, String response) {
		super(false, false);
		this.cmd = command;
		this.response = response;
	}

	public static int capacityOfResponse() {
		return capacityOfBody() - COMMAND_SIZE;
	}

	@Override
	public final PacketImplementation.Type getPacketType() {
		return Type.ACK;
	}

	@Override
	public byte getCommand() {
		return cmd;
	}

	@Override
	public String getResponse() {
		return response;
	}

	@Override
	byte[] getBodyBytes() {
		int size = getBodyLength();
		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.put(cmd).put(response.getBytes(StandardCharsets.US_ASCII)).flip();
		return buf.array();
	}

	@Override
	int getBodyLength() {
		return COMMAND_SIZE + response.length();
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(super.toString())
				.append("Lazurite Ack packet:")
				.append("\n Command: ").append(cmd)
				.append("\n Response: ").append(response)
				.append("\n");
		return builder.toString();
	}

}
