package com.github.tutertlob.lazurite;

import com.lapis_semi.lazurite.io.SUBGHZ_MAC;
import com.github.tutertlob.subghz.SubGHzFrame;

import java.util.Arrays;
import java.util.Objects;
import java.lang.StringBuilder;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

public final class LazuriteFrame extends SubGHzFrame {

	private static final int MAC_HEADER_SIZE = 11;

	static final int PAYLOAD_HEADER_SIZE = 1;

	static final int PAYLOAD_MAX_LENGTH = 250 - MAC_HEADER_SIZE;

	private final byte[] frameBuffer;

	private final Short panId;

	private final Short addr;

	private final SUBGHZ_MAC macHeader;

	private final LazuritePacket packet;

	private final String srcAddr;

	private final String dstAddr;

	public LazuriteFrame(SUBGHZ_MAC mac, byte[] raw) {
		super();

		macHeader = mac;
		frameBuffer = Arrays.copyOfRange(raw, mac.payload, mac.payload + mac.payload_len);
		panId = null;
		addr = null;
		packet = LazuritePacket.newInstance(getPayloadBytes());

		ByteBuffer little = ByteBuffer.wrap(macHeader.tx_addr);
		little.order(ByteOrder.LITTLE_ENDIAN);
		srcAddr = Long.toHexString(little.getLong());

		little = ByteBuffer.wrap(macHeader.rx_addr);
		little.order(ByteOrder.LITTLE_ENDIAN);
		dstAddr = Long.toHexString(little.getLong());
	}

	public LazuriteFrame(short panId, short addr, LazuritePacket packet) {
		super();

		this.frameBuffer = getFrameBytesFrom(packet);
		this.panId = Short.valueOf(panId);
		this.addr = Short.valueOf(addr);
		this.macHeader = null;
		this.packet = Objects.requireNonNull(packet, "Argument packet is null.");
		this.srcAddr = "";
		this.dstAddr = "";
	}

	private byte[] getFrameBytesFrom(LazuritePacket packet) {
		// frame byte array is equall with packet(payload) byte array for this version.
		return packet.getPacketBytes();
	}

	@Override
	public byte[] getFrameBytes() {
		return frameBuffer.clone();
	}

	@Override
	public int getFrameLength() {
		return frameBuffer.length;
	}

	public SUBGHZ_MAC getMacHeader() {
		return Objects.requireNonNull(macHeader, "This frame has no MAC header.");
	}

	@Override
	public LazuritePacket getPacket() {
		return Objects.requireNonNull(packet, "No packet is included.");
	}

	@Override
	public byte[] getPayloadBytes() {
		// frame byte array is handled equally with packet(payload) byte array for this version.
		// This means a frame has no header.
		return frameBuffer.clone();
	}

	@Override
	public int getPayloadLength() {
		// frame byte array is handled equally with packet(payload) byte array for this version.
		return frameBuffer.length;
	}

	@Override
	public String getSender() {
		return srcAddr;
	}

	@Override
	public final int getRssi() {
		if (Objects.nonNull(macHeader)) {
			return macHeader.rssi;
		} else {
			throw new NullPointerException("This frame must be not a received frame so that this doesn't have a MAC header.");
		}
	}

	public short getDestinationPanId() {
		return Objects.requireNonNull(panId, "PanId is not set.").shortValue();
	}

	public short getDestinationAddr() {
		return Objects.requireNonNull(addr, "Addr is not set.").shortValue();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		if (Objects.nonNull(macHeader)) {
			builder.append("SUBGHZ_MAC:")
				// .append("\n tv_sec=").append(macHeader.tv_sec.longValue())
				// .append("\n tv_nsec=").append(macHeader.tv_nsec.longValue())
				.append("\n header=").append(macHeader.header)
				.append("\n frame_type=").append(macHeader.frame_type)
				.append("\n sec_enb=").append(macHeader.sec_enb)
				.append("\n pending=").append(macHeader.pending)
				.append("\n ack_req=").append(macHeader.ack_req)
				.append("\n panid_comp=").append(macHeader.panid_comp)
				.append("\n seq_comp=").append(macHeader.seq_comp)
				.append("\n ielist=").append(macHeader.ielist)
				.append("\n tx_addr_type=").append(macHeader.tx_addr_type)
				.append("\n frame_ver=").append(macHeader.frame_ver)
				.append("\n rx_addr_type=").append(macHeader.rx_addr_type)
				.append("\n seq_num=").append(macHeader.seq_num)
				.append("\n addr_type=").append(macHeader.addr_type)
				.append("\n dst_panid=").append(String.format("0x%x", macHeader.rx_panid))
				.append("\n dst_addr=").append("0x").append(dstAddr)
				.append("\n src_panid=").append(String.format("0x%x", macHeader.tx_panid))
				.append("\n src_addr=").append("0x").append(srcAddr)
				// .append("\n raw=").append(Arrays.toString(macHeader.raw.getByteArray(0, (int)macHeader.raw_len)))
				// .append("\n raw_len=").append(macHeader.raw_len)
				.append("\n payload=").append(macHeader.payload)
				.append("\n payload_len=").append(macHeader.payload_len)
				.append("\n rssi=").append(macHeader.rssi)
				.append("\n");
		}
		
		builder.append("Frame:")
				.append("\n Payload length=").append(getPayloadLength())
				.append("\n Payload=").append(Hex.encodeHexString(getPayloadBytes()))
				.append("\n");
		if (Objects.nonNull(packet)) {
			builder.append(packet.toString());
		}

		return builder.toString();
	}

}
