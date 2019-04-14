package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.SubGHzFrame;
import com.github.tutertlob.subghz.PacketImplementation;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.Objects;

public class Im920Frame extends SubGHzFrame {

	static final int PAYLOAD_MAX_LENGTH = 64;

	private final int nodeId;

	private final int moduleId;

	private final int rssi;

	private final Im920Packet packet;

	private final String txAddr;

	private final byte[] frameBytes;

	public Im920Frame(ByteBuffer macHeader, ByteBuffer frameRaw) {
		super();

		macHeader.rewind();
		nodeId = (int)macHeader.get();
		moduleId = (int)macHeader.getShort();
		rssi = (int)macHeader.get();

		frameBytes = frameRaw.array().clone();

		packet = Im920Packet.newInstance(this, getPayloadBytes());

		txAddr = Integer.toHexString(moduleId);
	}

	public Im920Frame(Im920Packet packet) {
		super();
		nodeId = 0;
		moduleId = 0;
		rssi = 0;
		this.frameBytes = getFrameBytesFrom(packet);
		this.packet = Objects.requireNonNull(packet, "Argument packet is null.");
		this.txAddr = "";
	}

	private byte[] getFrameBytesFrom(Im920Packet packet) {
		// frame byte array is equall with packet(payload) byte array for this version.
		return packet.getPacketBytes();
	}

	@Override
	public final byte[] getFrameBytes() {
		return frameBytes.clone();
	}

	@Override
	public final int getFrameLength() {
		return frameBytes.length;
	}

	@Override
	public final PacketImplementation getPacket() {
		return Objects.requireNonNull(packet, "No packet is included.");
	}

	@Override
	public final byte[] getPayloadBytes() {
		// frame byte array is handled equally with packet(payload) byte array for this version.
		// This means a frame has no header.
		return frameBytes.clone();
	}

	@Override
	public final int getPayloadLength() {
		// frame byte array is handled equally with packet(payload) byte array for this version.
		return frameBytes.length;
	}

	@Override
	public String getSender() {
		return txAddr;
	}

	public final int getModuleId() {
		return moduleId;
	}

	public final int getNodeId() {
		return nodeId;
	}

	@Override
	public final int getRssi() {
		return rssi;
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("MAC Header:")
				.append("\n nodeId=").append(nodeId)
				.append("\n moduleId=").append(moduleId)
				.append("\n rssi=").append(rssi)
				.append("\n");		
		builder.append("Frame:")
				.append("\n payload=").append(Arrays.toString(getPayloadBytes()))
				.append("\n");
		if (Objects.nonNull(packet)) {
			builder.append(packet.toString());
		}

		return builder.toString();
	}

}
