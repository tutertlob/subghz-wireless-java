package com.github.tutertlob.subghz;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public abstract class PacketImplementation {

	private static final Logger logger = Logger.getLogger(PacketImplementation.class.getName());

	public PacketImplementation() {

	}

	public abstract byte[] getPacketBytes();

	public abstract int getPacketLength();

	public abstract PacketImplementation.Type getPacketType();

	public abstract boolean isFragmented();

	public abstract boolean isResponseRequested();

	public abstract String toString();

	public static enum Type {

		DATA((byte)0),

		COMMAND((byte)1),

		ACK((byte)2),

		NOTICE((byte)3);

		private final byte id;

		private static Map<Byte,Type> m = new HashMap<Byte, Type>();

		static {
			for (Type t : Type.values()) {
				m.put(Byte.valueOf(t.id), t);
			}
		}
		
		private Type(byte id) {
			this.id = id;
		}

		public byte id() {
			return this.id;
		}

		public static boolean isValid(byte i) {
			return Objects.nonNull(m.get(Byte.valueOf(i)));
		}

		public static PacketImplementation.Type valueOf(byte i) {
			if (!isValid(i)) {
				String msg = String.format("Invalid packet type id %d", i);
				logger.log(Level.WARNING, msg);
				throw new IllegalArgumentException(msg);
			}
			return m.get(Byte.valueOf(i));
		}

	}

}
