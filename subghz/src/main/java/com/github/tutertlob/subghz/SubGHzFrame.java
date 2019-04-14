package com.github.tutertlob.subghz;

public abstract class SubGHzFrame {

	public SubGHzFrame() {

	}

	public abstract byte[] getFrameBytes();

	public abstract int getFrameLength();

	public abstract byte[] getPayloadBytes();

	public abstract int getPayloadLength();

	public abstract PacketImplementation getPacket();

	public abstract String getSender();

	public abstract int getRssi();

	public abstract String toString();

}
