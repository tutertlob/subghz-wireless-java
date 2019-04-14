package com.github.tutertlob.im920wireless.util;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.tutertlob.im920wireless.packet.Im920Ack;
import com.github.tutertlob.im920wireless.packet.Im920Command;
import com.github.tutertlob.im920wireless.packet.Im920Data;
import com.github.tutertlob.im920wireless.packet.Im920Frame;
import com.github.tutertlob.im920wireless.packet.Im920Notice;
import com.github.tutertlob.im920wireless.packet.Im920Packet;

public final class Im920 {

	private static final Logger logger = Logger.getLogger(Im920.class.getName());

	private Im920Interface im920Interface;

	public static final byte IM920_MODULE_COMMAND = 1;

	private byte sequence = 0;

	public Im920(Im920Interface im920interface) {
		this.im920Interface = im920interface;
	}

	public Im920Frame readFrame() throws InterruptedException {
		ByteBuffer[] macFrame = this.im920Interface.takeReceivedFrame();
		Im920Frame frame = new Im920Frame(macFrame[0], macFrame[1]);
		return frame;
	}

	public void send(Im920Packet packet) {
		packet.setSeqNum(getNextFrameID());
		Im920Frame frame = new Im920Frame(packet);
		im920Interface.sendDataAsync(frame.getFrameBytes());
	}

	public void sendData(byte[] data, boolean fragment) {
		if (data == null) {
			String msg = "Argument data is null.";
			logger.log(Level.WARNING, msg);
			throw new NullPointerException(msg);
		}

		ByteBuffer buf;
		for (buf = ByteBuffer.wrap(data); buf.remaining() > Im920Data.capacityOfData();) {
			byte[] chopped = new byte[Im920Data.capacityOfData()];
			buf.get(chopped, 0, chopped.length);
			Im920Data packet = new Im920Data(chopped, true);

			send(packet);
		}
		byte[] chopped = new byte[buf.remaining()];
		buf.get(chopped, 0, chopped.length);
		Im920Data packet = new Im920Data(chopped, false);
		send(packet);
	}

	public void sendCommand(byte cmd, String param) {
		Im920Command packet = new Im920Command(cmd, param, false);
		send(packet);
	}

	public void sendCommandWithAck(byte cmd, String param) {
		Im920Command packet = new Im920Command(cmd, param, true);
		send(packet);
	}

	public void sendAck(byte cmd, String response) {
		Im920Ack packet = new Im920Ack(cmd, response);
		send(packet);
	}

	public void sendNotice(String notice) {
		Im920Notice packet = new Im920Notice(notice);
		send(packet);
	}

	private synchronized byte getNextFrameID() {
		return (byte) sequence++;
	}

}
