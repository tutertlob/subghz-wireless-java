package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.AckPacketInterface;
import com.github.tutertlob.subghz.PacketImplementation;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;

public final class Im920Ack extends Im920Packet implements AckPacketInterface {

	private static final int COMMAND_I = PACKET_BODY_I;

	private static final int COMMAND_SIZE = 1;

	private final byte cmd;

	private final String response;

	public Im920Ack(byte command, String response) {
		super(false, false);
		this.cmd = command;
		this.response = response;
	}

	public Im920Ack(byte[] packetRaw) {
		super(packetRaw);
		cmd = packetRaw[COMMAND_I];
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		response = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public Im920Ack(Im920Frame frame, byte[] packetRaw) {
		super(frame, packetRaw);
		cmd = packetRaw[COMMAND_I];
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		response = new String(stringByte, StandardCharsets.US_ASCII);
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
				.append("IM920 Ack packet:")
				.append("\n Command: ").append(cmd)
				.append("\n Response: ").append(response)
				.append("\n");
		return builder.toString();
	}

}
