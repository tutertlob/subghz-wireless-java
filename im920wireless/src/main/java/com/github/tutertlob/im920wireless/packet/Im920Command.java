package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.CommandPacketInterface;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;

public final class Im920Command extends Im920Packet implements CommandPacketInterface {

	private static final int COMMAND_I = PACKET_BODY_I;

	private static final int COMMAND_SIZE = 1;

	private final byte command;

	private final String commandParam;

	public Im920Command(byte command, String param, boolean responseRequested) {
		super(false, responseRequested);
		this.command = command;
		this.commandParam = param;
	}

	public Im920Command(byte[] packetRaw) {
		super(packetRaw);
		command = packetRaw[COMMAND_I];
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		commandParam = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public Im920Command(Im920Frame frame, byte[] packetRaw) {
		super(frame, packetRaw);
		command = packetRaw[COMMAND_I];
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		commandParam = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public static int capacityOfParam() {
		return capacityOfBody() - COMMAND_SIZE;
	}

	@Override
	public final PacketImplementation.Type getPacketType() {
		return Type.COMMAND;
	}

	@Override
	public byte getCommand() {
		return command;
	}

	@Override
	public String getCommandParam() {
		return commandParam;
	}

	@Override
	byte[] getBodyBytes() {
		int size = getBodyLength();
		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.put(command).put(commandParam.getBytes(StandardCharsets.US_ASCII)).flip();
		return buf.array();
	}

	@Override
	int getBodyLength() {
		return COMMAND_SIZE + commandParam.length();
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(super.toString())
				.append("IM920 Command packet:")
				.append("\n Command: ").append(command)
				.append("\n Parameter: ").append(commandParam)
				.append("\n");
		return builder.toString();
	}

}
