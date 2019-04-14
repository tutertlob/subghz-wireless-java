package com.github.tutertlob.lazurite;

import com.github.tutertlob.subghz.NoticePacketInterface;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import com.github.tutertlob.subghz.PacketImplementation;

public final class LazNotice extends LazuritePacket implements NoticePacketInterface {

	private final String notice;

	public LazNotice(ByteBuffer packetBytes) {
		super(packetBytes);
		byte[] stringByte = new byte[packetBytes.remaining()];
		packetBytes.get(stringByte, 0, packetBytes.remaining());
		notice = new String(stringByte, StandardCharsets.US_ASCII);
	}

	public LazNotice(String notice) {
		super(false, false);
		this.notice = notice;
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
				.append("Lazurite Notice packet:")
				.append("\n Notice: ").append(notice)
				.append("\n");
		return builder.toString();
	}

}
