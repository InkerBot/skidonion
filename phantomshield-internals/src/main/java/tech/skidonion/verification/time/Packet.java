package tech.skidonion.verification.time;

import java.nio.ByteBuffer;

public class Packet {

	public static class Timestamp {
		private final long seconds;
		private final long fraction;

		public Timestamp(long time) {
			seconds = time / 1000 + 2208988800L;
			long milli = time % 1000;
			fraction = (milli * 0x100000000L + 500) / 1000;
		}

		public Timestamp(long seconds, long fraction) {
			if (Integer.toUnsignedLong((int) seconds) != seconds)
				throw new IllegalArgumentException(
						"Seconds outside valid range");
			if (Integer.toUnsignedLong((int) fraction) != fraction)
				throw new IllegalArgumentException(
						"Fraction outside valid range");
			this.seconds = seconds;
			this.fraction = fraction;
		}

		public long getSeconds() {
			return seconds;
		}

		public long getFraction() {
			return fraction;
		}

		public long getTimeMillis() {
			long time = (seconds - 2208988800L) * 1000;
			long milli = (fraction * 1000 + 0x80000000L) / 0x100000000L;
			return time + milli;
		}

	}

	ByteBuffer buffer;

	public Packet() {
		buffer = ByteBuffer.allocate(48);

		setLeapIndicator(3);
		setVersion(4);
		setMode(3);
	}

	public Packet(ByteBuffer buffer) {
		if (buffer.limit() < 48)
			throw new IllegalArgumentException("Buffer size is less than 48");
		this.buffer = buffer;
	}

	public void setLeapIndicator(int leapIndicator) {
		if (leapIndicator < 0 || leapIndicator >= 4)
			throw new IllegalArgumentException(
					"Leap Indicator outside valid range");
		byte b = buffer.get(0);
		b &= ~0b11000000;
		b |= leapIndicator << 6;
		buffer.put(0, b);
	}

	public void setVersion(int version) {
		if (version < 0 || version >= 8)
			throw new IllegalArgumentException("Version outside valid range");
		byte b = buffer.get(0);
		b &= ~0b00111000;
		b |= version << 3;
		buffer.put(0, b);
	}

	public void setMode(int mode) {
		if (mode < 0 || mode >= 8)
			throw new IllegalArgumentException("Mode outside valid range");
		byte b = buffer.get(0);
		b &= ~0b0000111;
		b |= mode;
		buffer.put(0, b);
	}

	public void setTransmitTimestamp(Timestamp t) {
		buffer.putInt(40, (int) t.getSeconds());
		buffer.putInt(44, (int) t.getFraction());
	}

	public Timestamp getReceiveTimestamp() {
		long seconds = Integer.toUnsignedLong(buffer.getInt(32));
		long fraction = Integer.toUnsignedLong(buffer.getInt(36));
		return new Timestamp(seconds, fraction);
	}
	
	public Timestamp getTransmitTimestamp() {
		long seconds = Integer.toUnsignedLong(buffer.getInt(40));
		long fraction = Integer.toUnsignedLong(buffer.getInt(44));
		return new Timestamp(seconds, fraction);
	}

	public byte[] asByteArray() {
		return buffer.array();
	}

}