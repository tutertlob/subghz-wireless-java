package com.github.tutertlob.lazurite;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

import com.lapis_semi.lazurite.io.Liblazurite;
import com.lapis_semi.lazurite.io.SUBGHZ_MAC;
import com.sun.jna.Pointer;

public final class LazuriteUtils {

	private static final Logger logger = Logger.getLogger(LazuriteUtils.class.getName());

	private static final Liblazurite liblazurite = newInstanceOfLiblazurite();

	private static Liblazurite newInstanceOfLiblazurite() {
		Liblazurite lib = null;
		try {
			lib = new Liblazurite();
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Couldn't setup the lazurite wireless module.", e);
			System.exit(-1);
		}
		return lib;
	}

	private LazuriteUtils() {
		// Abstract class
	}

	public static void begin(LazuriteParams params) {
		logger.info("LazuriteUtils.begin");
		try {
			liblazurite.init();
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Couldn't load the lazurite driver.", e);
			System.exit(-1);
		}

		try {
			liblazurite.setAddrType(params.addrType());
			liblazurite.setTxRetry(params.txRetry());
			liblazurite.setTxInterval(params.txInterval());
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		try {
			liblazurite.begin(
				params.ch(),
				params.myPanId(),
				params.rate(),
				params.pwr()
			);
			liblazurite.rxEnable();
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Couldn't setup the lazurite interface.", e);
			System.exit(-1);
		}
	}

	public static void close() {
		try {
			liblazurite.rxDisable();
			liblazurite.close();
			liblazurite.remove();
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Couldn't close the lazurite interface.", e);
			System.exit(-1);
		}
	}

	public static LazuriteFrame readFrame() {
		byte[] raw = new byte[256];
		short[] size = new short[1];
		int length;
		LazuriteFrame frame = null;

		try {
			while (liblazurite.available() == 0);
			length = liblazurite.read(raw, size);
			logger.log(Level.INFO, String.format("Read a frame: length=%d, size=%d", length, size[0]));
			assert length == (int)size[0] : "Read size is different from length.";
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Reading data from the Lazurite wireless module failed.", e);
			throw new IllegalArgumentException("Reading data from the Lazurite wireless module failed.");
		}

		try {
			SUBGHZ_MAC mac = new SUBGHZ_MAC();
			liblazurite.decMac(mac, raw, size[0]);
			frame = new LazuriteFrame(mac, raw);
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Decoding MAC flame header failed. This frame data may be broken.", e);
			throw new IllegalArgumentException("Frame couldn't be re-constructed from raw byte array.");
		}

		return frame;
	}

	public static void sendFrame(short rxPanId, short rxAddr, LazuriteFrame frame) {
		try {
			int ret;
			ret = liblazurite.send(rxPanId, rxAddr, frame.getFrameBytes(), (short)frame.getFrameLength());
			logger.log(Level.INFO, String.format("Sending Frame to rxPanid=%x rxAddr=%x resulted in ret=%d", rxPanId, rxAddr, ret));
		} catch (IOException e ) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Sending data over the Lazurite wireless module failed.", e);
			if (e.getMessage().contains("error=-110")) {
				logger.log(Level.WARNING, String.format("The sensor %x %x didn't responde.", rxPanId, rxAddr), e);
			} else {
				throw new IllegalArgumentException("Sending data over the Lazurite wireless module failed.");
			}
		}
	}

	public static void sendData(short rxPanId, short rxAddr, byte[] data) {
		int remaining = data.length;
		int pos = 0;
		while (remaining > 0) {
			boolean fragmented = remaining > LazData.capacityOfData();
			int size = fragmented ? LazData.capacityOfData() : remaining;
			byte[] part = Arrays.copyOfRange(data, pos, pos + size);
			LazData packet = new LazData(part, fragmented);
			LazuriteFrame frame = new LazuriteFrame(rxPanId, rxAddr, packet);
			sendFrame(rxPanId, rxAddr, frame);
			remaining -= size;
		}
	}

	public static void sendCommand(short rxPanId, short rxAddr, byte cmd, String param) {
		LazCommand packet = new LazCommand(cmd, param, false);
		LazuriteFrame frame = new LazuriteFrame(rxPanId, rxAddr, packet);
		sendFrame(rxPanId, rxAddr, frame);
	}

	public static void sendCommandWithAck(short rxPanId, short rxAddr, byte cmd, String param) {
		LazCommand packet = new LazCommand(cmd, param, true);
		LazuriteFrame frame = new LazuriteFrame(rxPanId, rxAddr, packet);
		sendFrame(rxPanId, rxAddr, frame);
	}

	public static void sendAck(short rxPanId, short rxAddr, byte cmd, String response) {
		LazAck packet = new LazAck(cmd, response);
		LazuriteFrame frame = new LazuriteFrame(rxPanId, rxAddr, packet);
		sendFrame(rxPanId, rxAddr, frame);
	}

	public static void sendNotice(short rxPanId, short rxAddr, String notice) {
		LazNotice packet = new LazNotice(notice);
		LazuriteFrame frame = new LazuriteFrame(rxPanId, rxAddr, packet);
		sendFrame(rxPanId, rxAddr, frame);
	}

}
