package com.github.tutertlob.lazurite;

public class LazuriteParams {

    private final byte ch;

    private final short myPanId;

    private final byte rate;

    private final byte pwr;

    private final byte addrType;

    private final byte txRetry;

    private final short txInterval;

    public static class Builder {

        private final byte ch;

        private final short myPanId;

        private byte rate = 100;

        private byte pwr = 20;

        private byte addrType = 6;

        private byte txRetry = 10;

        private short txInterval = 500;

        public Builder(byte ch, short myPanId) {
            this.ch = ch;
            this.myPanId = myPanId;
        }

        public Builder rate(byte val) {
            if (val == 100 || val == 50) {
                rate = val;
            }
            return this;
        }

        public Builder pwr(byte val) {
            if (val == 20 || val == 1) {
                pwr = val;
            }
            return this;
        }

        public Builder addrType(byte val) {
            if (val <= 6) {
                addrType = val;
            }
            return this;
        }

        public Builder txRetry(int val) {
            if (val <= 255) {
                txRetry = (byte)val;
            }
            return this;
        }

        public Builder txInterval(short val) {
            if (val <= 500) {
                txInterval = val;
            }
            return this;
        }

        public LazuriteParams build() {
            return new LazuriteParams(this);
        }
    }

    private LazuriteParams(Builder builder) {
        ch = builder.ch;
        myPanId = builder.myPanId;
        rate = builder.rate;
        pwr = builder.pwr;
        addrType = builder.addrType;
        txRetry = builder.txRetry;
        txInterval = builder.txInterval;
    }

    public byte ch() {
        return ch;
    }

    public short myPanId() {
        return myPanId;
    }

    public byte rate() {
        return rate;
    }

    public byte pwr() {
        return pwr;
    }

    public byte addrType() {
        return addrType;
    }

    public byte txRetry() {
        return txRetry;
    }

    public short txInterval() {
        return txInterval;
    }
}