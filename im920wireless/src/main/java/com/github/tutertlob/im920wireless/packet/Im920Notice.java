package com.github.tutertlob.im920wireless.packet;

import com.github.tutertlob.subghz.PacketImplementation;
import com.github.tutertlob.subghz.NoticePacketInterface;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Im920Notice extends Im920Packet implements NoticePacketInterface {

	private final String notice;

	public Im920Notice(String notice) {
		super(false, false);
		this.notice = notice;
	}

	public Im920Notice(byte[] packetRaw) {
		super(packetRaw);
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		notice = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public Im920Notice(Im920Frame frame, byte[] packetRaw) {
		super(frame, packetRaw);
		byte[] stringByte = Arrays.copyOfRange(packetRaw, Im920Packet.PACKET_BODY_I, packetRaw.length);
		notice = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public static int capacityOfNotice() {
		return capacityOfBody();
	}

	@Override
	public final PacketImplementation.Type getPacketType() {
		return Type.NOTICE;
	}

	@Override
	public String getNotice() {
		return notice;
	}

	@Override
	byte[] getBodyBytes() {
		return notice.getBytes(StandardCharsets.US_ASCII);
	}

	@Override
	int getBodyLength() {
		return notice.length();
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString())
				.append("IM920 Notice packet:")
				.append("\n Notice: ").append(notice)
				.append("\n");
		return builder.toString();
	}

}
