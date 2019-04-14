package com.github.tutertlob.lazurite;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.CommandPacketInterface;

import java.nio.ByteBuffer;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;

public final class LazCommand extends LazuritePacket implements CommandPacketInterface {

	static final int COMMAND_SIZE = 1;

	private final byte command;

	private final String commandParam;

	public LazCommand(ByteBuffer packetBytes) {
		super(packetBytes);
		command = packetBytes.get();
		byte[] stringByte = new byte[packetBytes.remaining()];
		packetBytes.get(stringByte, 0, packetBytes.remaining());
		commandParam = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public LazCommand(byte command, String param, boolean responseRequested) {
		super(false, responseRequested);
		this.command = command;
		this.commandParam = param;
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
				.append("Lazurite Command packet:")
				.append("\n Command: ").append(command)
				.append("\n Parameter: ").append(commandParam)
				.append("\n");
		return builder.toString();
	}

}
