package com.github.tutertlob.im920wireless.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public final class Im920Interface {

	private static final Logger logger = Logger.getLogger(Im920Interface.class.getName());

	public enum BaudRate {
		B_1200(1200), B_2400(2400), B_4800(4800), B_9600(9600), B_19200(19200), B_38400(38400), B_57600(
				57600), B_115200(115200);

		private final int baud;

		private static final Map<Integer, BaudRate> m = new HashMap<>();

		BaudRate(int baud) {
			this.baud = baud;
		}

		static {
			for (BaudRate b : BaudRate.values()) {
				m.put(Integer.valueOf(b.baud), b);
			}
		}

		public int baud() {
			return baud;
		}

		public static BaudRate valueOf(Integer baud) {
			BaudRate b = m.get(baud);
			if (b == null) {
				String msg = String.format("Baud rate %d is not supported. 19200bps is used insted.", baud);
				logger.warning(msg);
				throw new IllegalArgumentException(msg);
			}
			return b;
		}
	}

	private int usTxTimePerByte;

	private OutputStream out;

	private InputStream in;

	private SerialPort serial;

	private BlockingQueue<ByteBuffer[]> incomingFrames = new LinkedBlockingQueue<>();

	private BlockingQueue<String[]> commandResponse = new LinkedBlockingQueue<>();

	private BlockingQueue<Ticket> commandQueue = new LinkedBlockingQueue<>();

	private Thread serialReader;

	private Thread serialWriter;

	public static Im920Interface open(String portName, BaudRate baud)
			throws IOException, NoSuchPortException, PortInUseException {
		Im920Interface im920Interface;
		CommPortIdentifier commPortIdentifier;

		try {
			commPortIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		} catch (NoSuchPortException e) {
			logger.warning(portName + ": No such port.");
			throw e;
		}

		if (commPortIdentifier.isCurrentlyOwned()) {
			logger.warning(portName + " is currently in use.");
			throw new PortInUseException();
		}

		CommPort commPort;
		try {
			commPort = commPortIdentifier.open(Im920Interface.class.getName(), 2000);
		} catch (PortInUseException e) {
			logger.warning(portName + " is currently in use.");
			throw e;
		}

		if (commPort instanceof SerialPort) {
			SerialPort serial = (SerialPort) commPort;
			try {
				serial.setSerialPortParams(baud.baud(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				logger.log(Level.WARNING, "Attemped to do setSerialPortParams with unsupported parameters.", e);
				// Keep running.
			}

			im920Interface = new Im920Interface();
			im920Interface.serial = serial;
			im920Interface.usTxTimePerByte = (int) ((1000000 / baud.baud()) + 1) * 10;

			try {
				im920Interface.in = serial.getInputStream();
				im920Interface.out = serial.getOutputStream();
			} catch (IOException e) {
				im920Interface.serial.close();
				im920Interface.serial = null;
				logger.log(Level.WARNING, "Couldn't get I/O streams for the serial port.", e);
				throw new UncheckedIOException("Couldn't get I/O streams for the serial port.", e);
			}
		} else {
			logger.warning("Only serial ports are handled by this program.");
			throw new IOException("Only serial ports are handled by this program.");
		}

		(im920Interface.serialReader = new Thread(new Im920Interface.SerialReader(im920Interface))).start();
		(im920Interface.serialWriter = new Thread(new Im920Interface.SerialWriter(im920Interface))).start();

		return im920Interface;
	}

	synchronized public void close() {
		try {
			this.in.close();
			this.out.close();
			serialReader.interrupt();
			serialWriter.interrupt();
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "The reader/writer threads don't respond to close", e);
		} catch (IOException e) {
			logger.log(Level.INFO, "Input/Output Serial streams has been closed.", e);
		} finally {
			serial.close();
		}
	}

	private boolean isAvailable() throws IOException {
		return in.available() > 0 ? true : false;
	}

	private int readSerial(byte[] buf) throws IOException {
		return in.read(buf);
	}

	private void writeSerial(String cmd) throws IOException {
		byte[] hex = cmd.getBytes(StandardCharsets.US_ASCII);
		out.write(hex);
		out.flush();
	}

	public ByteBuffer[] takeReceivedFrame() throws InterruptedException {
		return incomingFrames.take();
	}

	private void putReceivedFrame(ByteBuffer[] frame) throws InterruptedException {
		incomingFrames.put(frame);
	}

	private String[] checkinCommand(String cmd) throws InterruptedException {
		Ticket ticket = Ticket.checkin(cmd, true);
		commandQueue.put(ticket);
		return ticket.checkout();
	}

	private void checkinCommandWithoutRes(String cmd) {
		Ticket ticket = Ticket.checkin(cmd, false);
		try {
			commandQueue.put(ticket);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, String.format("The commandQueue must be full (%d/%d), but it is not expected.",
					commandQueue.size(), commandQueue.remainingCapacity()), e);
		}
	}

	private Ticket takeCommand() throws InterruptedException {
		return commandQueue.take();
	}

	private void notifyCmdResponses(String[] responses) throws InterruptedException {
		commandResponse.put(responses);
	}

	private String[] waitCmdResponses() throws InterruptedException {
		return commandResponse.take();
	}

	private boolean execIm920CmdAndMatches(String cmd, String search) {
		assert cmd != null;
		assert search != null;

		try {
			String[] responses = checkinCommand(cmd);
			return responses[0].startsWith(search);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Aborted to execute a command.", e);
			return false;
		}
	}

	private ByteBuffer[] normalizeFrame(StringBuilder rawFrame) {
		try {
			StringBuilder buf = new StringBuilder();

			for (int begin = 0, end = rawFrame.indexOf(",");; end = rawFrame.indexOf(",", begin)) {
				if (end > 0) {
					buf.append(rawFrame.substring(begin, end));
					begin = end + 1;
				} else {
					buf.append(rawFrame.substring(begin, rawFrame.length()));
					break;
				}
			}

			String[] frame = buf.toString().split(":");
			byte[] header = Hex.decodeHex(frame[0]);
			byte[] body = Hex.decodeHex(frame[1]);

			return new ByteBuffer[] { ByteBuffer.wrap(header), ByteBuffer.wrap(body) };
		} catch (IndexOutOfBoundsException e) {
			logger.log(Level.WARNING, "Received frame is collapsed.", e);
			throw e;
		} catch (DecoderException e) {
			logger.log(Level.WARNING, "Decoding HEX string to byte array is failed.", e);
			throw new IllegalArgumentException("Decoding HEX string to byte array is failed.");
		}
	}

	private boolean isIm920Frame(String frame) {
		assert frame != null;

		if (frame.length() < 11)
			return false;

		return frame.matches("^\\p{XDigit}{2},\\p{XDigit}{4},\\p{XDigit}{2}:(\\p{XDigit}{2},){0,}+\\p{XDigit}{2}$");
	}

	public void sendDataAsync(byte[] binaryData) {
		if (binaryData == null)
			throw new NullPointerException("The argument binaryData is null.");

		StringBuilder cmd = new StringBuilder("TXDA").append(Hex.encodeHexString(binaryData))
				.append("\r\n");
		checkinCommandWithoutRes(cmd.toString());
	}

	public void sendData(byte[] binaryData) throws InterruptedException {
		if (binaryData == null)
			throw new NullPointerException("The argument binaryData is null.");

		StringBuilder cmd = new StringBuilder("TXDA").append(Hex.encodeHexString(binaryData));
		execIm920Cmd(cmd.toString());
	}

	public int getTxRxTimePerByte() {
		return usTxTimePerByte;
	}

	public boolean enableSleep() {
		return execIm920CmdAndMatches("DSRX\r\n", "OK");
	}

	public boolean disableSleep() {
		return execIm920CmdAndMatches("?ENRX\r\n", "OK");
	}

	public int getActiveDuration() {
		String response = "NG";
		try {
			response = execIm920Cmd("RWTM\r\n")[0];
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "InterruptedException happened.", e);
		}

		if (response.startsWith("NG")) {
			logger.info("IM920 command 'RWTM' failed.");
			return 0;
		}
		return Integer.parseInt(response.substring(0, 4), 16);
	}

	public boolean setActiveDuration(int activeTime) {
		if (activeTime < 0 || activeTime > 0xFFFF)
			throw new IllegalArgumentException("The specified active duration time is invalid.");

		String swtm = String.format("SWTM%04X\r\n", activeTime);
		return execIm920CmdAndMatches(swtm, "OK");
	}

	public int getSleepDuration() {
		String response = "NG";
		try {
			response = execIm920Cmd("RSTM\r\n")[0];
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "InterruptedException happened", e);
		}
		if (response.startsWith("NG")) {
			logger.info("IM920 command 'RSTM' failed.");
			return 0;
		}

		return Integer.parseInt(response.substring(0, 4), 16);
	}

	public boolean setSleepDuration(int sleepTime) {
		if (sleepTime < 0 || sleepTime > 0xFFFF)
			throw new IllegalArgumentException("The specified sleep duration time is invalid.");

		String sstm = String.format("SSTM%04X\r\n", sleepTime);
		return execIm920CmdAndMatches(sstm, "OK");
	}

	public boolean resetInterface() {
		return execIm920CmdAndMatches("SRST\r\n", "IM920 Ver.");
	}

	public String[] execIm920Cmd(String command) throws InterruptedException {
		if (command == null)
			throw new NullPointerException("The argument command is null.");

		String cmd = new StringBuilder(command).append("\r\n").toString();
		return checkinCommand(cmd);
	}

	private static class Ticket {
		private static final Logger logger = Logger.getLogger(Ticket.class.getName());

		private static List<Ticket> tickets = Collections.synchronizedList(
				new ArrayList<>(Arrays.asList(new Ticket(), new Ticket(), new Ticket(), new Ticket(), new Ticket())));

		String cmd = "";

		BlockingQueue<String[]> responses = new ArrayBlockingQueue<>(1);

		boolean sync = true;

		private Ticket() {
		}

		public static Ticket checkin(String cmd, boolean sync) {
			if (cmd == null) {
				throw new NullPointerException("The string argument cmd is null");
			}

			try {
				Ticket ticket = tickets.remove(0);
				ticket.cmd = cmd;
				ticket.sync = sync;
				ticket.responses.clear();
				return ticket;
			} catch (IndexOutOfBoundsException e) {
				logger.log(Level.INFO, "Creating new tickets.");

				tickets.addAll(Arrays.asList(new Ticket(), new Ticket(), new Ticket(), new Ticket()));

				Ticket ticket = new Ticket();
				ticket.cmd = cmd;
				ticket.sync = sync;
				return ticket;
			}
		}

		public String[] checkout() throws InterruptedException {
			if (!sync)
				throw new IllegalStateException(
						"Waiting for a response on this ticket has not been permitted, which declares no sync.");

			String[] responses = this.responses.take();
			this.cmd = "";

			assert this.responses.size() == 0;

			tickets.add(this);
			return responses;
		}

		public String getCmd() {
			return this.cmd;
		}

		public void notifyResponses(String[] responses) throws InterruptedException {
			assert this.responses.size() == 0;
			if (this.sync)
				this.responses.put(responses);
			else
				tickets.add(this);
		}
	}

	private static class SerialReader implements Runnable {
		private static final Logger logger = Logger.getLogger(SerialReader.class.getName());

		private Im920Interface im920Interface;

		public SerialReader(Im920Interface im920Interface) {
			this.im920Interface = im920Interface;
		}

		public void run() {
			try {
				List<String> responses = new ArrayList<>();
				for (StringBuilder frame = new StringBuilder();;) {
					byte[] buf = new byte[256];
					int readLen = 0;
					try {
						if (responses.size() == 0) {
							readLen = im920Interface.readSerial(buf);
						} else {
							Thread.sleep(im920Interface.getTxRxTimePerByte());
							if (im920Interface.isAvailable()) {
								readLen = im920Interface.readSerial(buf);
							} else {
								im920Interface.notifyCmdResponses(responses.toArray(new String[responses.size()]));
								responses.clear();
								continue;
							}
						}
					} catch (IOException e) {
						logger.log(Level.WARNING, "IOException happened when reading bytes from the serial.", e);
					}

					boolean prev = false;
					for (String s : new String(buf, 0, readLen, StandardCharsets.US_ASCII).split("(\r\n*)|\n", -2)) {
						if (!s.isEmpty()) {
							if (prev) {
								if (im920Interface.isIm920Frame(frame.toString())) {
									im920Interface.putReceivedFrame(im920Interface.normalizeFrame(frame));
								} else if (frame.length() > 2) {
									responses.add(frame.toString());
								} else {
									im920Interface.notifyCmdResponses(new String[] { frame.toString() });
								}
								frame = new StringBuilder();
							}
							frame.append(s);
							prev = true;
						} else {
							if (frame.length() > 0) {
								if (im920Interface.isIm920Frame(frame.toString())) {
									im920Interface.putReceivedFrame(im920Interface.normalizeFrame(frame));
								} else if (frame.length() > 2) {
									responses.add(frame.toString());
								} else {
									im920Interface.notifyCmdResponses(new String[] { frame.toString() });
								}
								frame = new StringBuilder();
							}
						}
					}
				}
			} catch (InterruptedException e) {
				logger.log(Level.INFO,
						"SerialReader thread is going to exit gentely since an interface close request is received.",
						e);
			}
		}
	}

	private static class SerialWriter implements Runnable {
		private static final Logger logger = Logger.getLogger(SerialWriter.class.getName());

		Im920Interface im920Interface;

		public SerialWriter(Im920Interface im920Interface) {
			this.im920Interface = im920Interface;
		}

		public void run() {
			try {
				for (;;) {
					Ticket ticket = im920Interface.takeCommand();
					try {
						String cmd = ticket.getCmd();
						if (cmd.startsWith("?")) {
							im920Interface.writeSerial("?");
							cmd = cmd.substring(1);
							Thread.sleep(im920Interface.getTxRxTimePerByte());
						}
						im920Interface.writeSerial(cmd);
						String[] responses = im920Interface.waitCmdResponses();
						ticket.notifyResponses(responses);
					} catch (IOException e) {
						logger.log(Level.WARNING, "Serial error happened when writing data.", e);
						logger.log(Level.WARNING, String.format("Command %s was discarded.", ticket.getCmd()));
					}
				}
			} catch (InterruptedException e) {
				logger.log(Level.INFO,
						"SerialWriter thread is going to exit gentely since an interface close request is received.",
						e);
			}
		}
	}
}
