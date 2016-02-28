package org.murinrad.android.musicmultiply.tags;

/**
 * Created by Radovan Murin on 11.4.2015.
 */
public class Constants {

    public static final int DEVICE_ADVERTISEMENT_PORT = 14777;

    public static final int QoS_PORT = 14778;

    public static enum QoS_PACKET_TYPE {
        STATISTICS_REPORT {
            @Override
            public byte getByteRepresentation() {
                return STATISTICS_REPORT_BYTE;
            }
        }, PACKET_RESEND_REQUEST {
            @Override
            public byte getByteRepresentation() {
                return PACKET_RESEND_BYTE;
            }
        };
        private static final byte STATISTICS_REPORT_BYTE = 1;
        private static final byte PACKET_RESEND_BYTE = 2;

        public static QoS_PACKET_TYPE getDatagramType(byte type) {
            switch (type) {
                case STATISTICS_REPORT_BYTE:
                    return STATISTICS_REPORT;
                case PACKET_RESEND_BYTE:
                    return PACKET_RESEND_REQUEST;
                default:
                    return null;
            }
        }

        public abstract byte getByteRepresentation();
    }

}
