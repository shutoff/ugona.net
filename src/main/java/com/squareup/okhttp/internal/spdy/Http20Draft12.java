/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal.spdy;

import com.squareup.okhttp.Protocol;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Source;
import okio.Timeout;

import static com.squareup.okhttp.internal.spdy.Http20Draft12.FrameLogger.formatHeader;
import static java.lang.String.format;
import static java.util.logging.Level.FINE;
import static okio.ByteString.EMPTY;

/**
 * Read and write HTTP/2 v12 frames.
 * <p>http://tools.ietf.org/html/draft-ietf-httpbis-http2-12
 */
public final class Http20Draft12 implements Variant {
    static final byte TYPE_DATA = 0x0;
    static final byte TYPE_HEADERS = 0x1;
    static final byte TYPE_PRIORITY = 0x2;
    static final byte TYPE_RST_STREAM = 0x3;
    static final byte TYPE_SETTINGS = 0x4;
    static final byte TYPE_PUSH_PROMISE = 0x5;
    static final byte TYPE_PING = 0x6;
    static final byte TYPE_GOAWAY = 0x7;
    static final byte TYPE_WINDOW_UPDATE = 0x8;
    static final byte TYPE_CONTINUATION = 0x9;
    static final byte TYPE_ALTSVC = 0xa;
    static final byte TYPE_BLOCKED = 0xb;
    static final byte FLAG_NONE = 0x0;
    static final byte FLAG_ACK = 0x1; // Used for settings and ping.
    static final byte FLAG_END_STREAM = 0x1; // Used for headers and data.
    static final byte FLAG_END_SEGMENT = 0x2;
    static final byte FLAG_END_HEADERS = 0x4; // Used for headers and continuation.
    static final byte FLAG_END_PUSH_PROMISE = 0x4;
    static final byte FLAG_PAD_LOW = 0x8; // Used for headers, data, and continuation.
    static final byte FLAG_PAD_HIGH = 0x10; // Used for headers, data, and continuation.
    static final byte FLAG_PRIORITY = 0x20; // Used for headers.
    static final byte FLAG_COMPRESSED = 0x20; // Used for data.
    private static final Logger logger = Logger.getLogger(Http20Draft12.class.getName());
    private static final ByteString CONNECTION_PREFACE
            = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");

    private static IllegalArgumentException illegalArgument(String message, Object... args) {
        throw new IllegalArgumentException(format(message, args));
    }

    private static IOException ioException(String message, Object... args) throws IOException {
        throw new IOException(format(message, args));
    }

    private static short readPadding(BufferedSource source, byte flags) throws IOException {
        if ((flags & FLAG_PAD_HIGH) != 0 && (flags & FLAG_PAD_LOW) == 0) {
            throw ioException("PROTOCOL_ERROR FLAG_PAD_HIGH set without FLAG_PAD_LOW");
        }
        int padding = 0;
        if ((flags & FLAG_PAD_HIGH) != 0) {
            padding = source.readShort() & 0xffff;
        } else if ((flags & FLAG_PAD_LOW) != 0) {
            padding = source.readByte() & 0xff;
        }
        if (padding > 16383) throw ioException("PROTOCOL_ERROR padding > 16383: %s", padding);
        return (short) padding;
    }

    private static short lengthWithoutPadding(short length, byte flags, short padding)
            throws IOException {
        if ((flags & FLAG_PAD_HIGH) != 0) { // account for reading the padding length.
            length -= 2;
        } else if ((flags & FLAG_PAD_LOW) != 0) {
            length--;
        }
        if (padding > length) {
            throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", padding, length);
        }
        return (short) (length - padding);
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.HTTP_2;
    }

    /**
     * Creates a frame reader with max header table size of 4096 and data frame
     * compression disabled.
     */
    @Override
    public FrameReader newReader(BufferedSource source, boolean client) {
        return new Reader(source, 4096, client);
    }

    @Override
    public FrameWriter newWriter(BufferedSink sink, boolean client) {
        return new Writer(sink, client);
    }

    @Override
    public int maxFrameSize() {
        return 16383;
    }

    static final class Reader implements FrameReader {
        // Visible for testing.
        final HpackDraft07.Reader hpackReader;
        private final BufferedSource source;
        private final ContinuationSource continuation;
        private final boolean client;

        Reader(BufferedSource source, int headerTableSize, boolean client) {
            this.source = source;
            this.client = client;
            this.continuation = new ContinuationSource(this.source);
            this.hpackReader = new HpackDraft07.Reader(headerTableSize, continuation);
        }

        @Override
        public void readConnectionPreface() throws IOException {
            if (client) return; // Nothing to read; servers doesn't send a connection preface!
            ByteString connectionPreface = source.readByteString(CONNECTION_PREFACE.size());
            if (logger.isLoggable(FINE))
                logger.fine(format("<< CONNECTION %s", connectionPreface.hex()));
            if (!CONNECTION_PREFACE.equals(connectionPreface)) {
                throw ioException("Expected a connection header but was %s", connectionPreface.utf8());
            }
        }

        @Override
        public boolean nextFrame(Handler handler) throws IOException {
            int w1;
            int w2;
            try {
                w1 = source.readInt();
                w2 = source.readInt();
            } catch (IOException e) {
                return false; // This might be a normal socket close.
            }

            // boolean r = (w1 & 0xc0000000) != 0; // Reserved: Ignore first 2 bits.
            short length = (short) ((w1 & 0x3fff0000) >> 16); // 14-bit unsigned == max 16383
            byte type = (byte) ((w1 & 0xff00) >> 8);
            byte flags = (byte) (w1 & 0xff);
            // boolean r = (w2 & 0x80000000) != 0; // Reserved: Ignore first bit.
            int streamId = (w2 & 0x7fffffff); // 31-bit opaque identifier.
            if (logger.isLoggable(FINE))
                logger.fine(formatHeader(true, streamId, length, type, flags));

            switch (type) {
                case TYPE_DATA:
                    readData(handler, length, flags, streamId);
                    break;

                case TYPE_HEADERS:
                    readHeaders(handler, length, flags, streamId);
                    break;

                case TYPE_PRIORITY:
                    readPriority(handler, length, flags, streamId);
                    break;

                case TYPE_RST_STREAM:
                    readRstStream(handler, length, flags, streamId);
                    break;

                case TYPE_SETTINGS:
                    readSettings(handler, length, flags, streamId);
                    break;

                case TYPE_PUSH_PROMISE:
                    readPushPromise(handler, length, flags, streamId);
                    break;

                case TYPE_PING:
                    readPing(handler, length, flags, streamId);
                    break;

                case TYPE_GOAWAY:
                    readGoAway(handler, length, flags, streamId);
                    break;

                case TYPE_WINDOW_UPDATE:
                    readWindowUpdate(handler, length, flags, streamId);
                    break;

                case TYPE_ALTSVC:
                    readAlternateService(handler, length, flags, streamId);
                    break;

                case TYPE_BLOCKED: // Ignore as this is experimental.
                    if (length != 0) throw ioException("TYPE_BLOCKED length != 0: %s", length);
                    break;

                default:
                    throw ioException("PROTOCOL_ERROR: unknown frame type %s", type);
            }
            return true;
        }

        private void readHeaders(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (streamId == 0) throw ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0");

            boolean endStream = (flags & FLAG_END_STREAM) != 0;

            short padding = readPadding(source, flags);

            if ((flags & FLAG_PRIORITY) != 0) {
                readPriority(handler, streamId);
                length -= 5; // account for above read.
            }

            length = lengthWithoutPadding(length, flags, padding);

            List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);

            handler.headers(false, endStream, streamId, -1, headerBlock, HeadersMode.HTTP_20_HEADERS);
        }

        private List<Header> readHeaderBlock(short length, short padding, byte flags, int streamId)
                throws IOException {
            continuation.length = continuation.left = length;
            continuation.padding = padding;
            continuation.flags = flags;
            continuation.streamId = streamId;

            hpackReader.readHeaders();
            hpackReader.emitReferenceSet();
            // TODO: Concat multi-value headers with 0x0, except COOKIE, which uses 0x3B, 0x20.
            // http://tools.ietf.org/html/draft-ietf-httpbis-http2-09#section-8.1.3
            return hpackReader.getAndReset();
        }

        private void readData(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            // TODO: checkState open or half-closed (local) or raise STREAM_CLOSED
            boolean inFinished = (flags & FLAG_END_STREAM) != 0;
            boolean gzipped = (flags & FLAG_COMPRESSED) != 0;
            if (gzipped) {
                throw ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA");
            }

            short padding = readPadding(source, flags);
            length = lengthWithoutPadding(length, flags, padding);

            handler.data(inFinished, streamId, source, length);
            source.skip(padding);
        }

        private void readPriority(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (length != 5) throw ioException("TYPE_PRIORITY length: %d != 5", length);
            if (streamId == 0) throw ioException("TYPE_PRIORITY streamId == 0");
            readPriority(handler, streamId);
        }

        private void readPriority(Handler handler, int streamId) throws IOException {
            int w1 = source.readInt();
            boolean exclusive = (w1 & 0x80000000) != 0;
            int streamDependency = (w1 & 0x7fffffff);
            int weight = (source.readByte() & 0xff) + 1;
            handler.priority(streamId, streamDependency, weight, exclusive);
        }

        private void readRstStream(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (length != 4) throw ioException("TYPE_RST_STREAM length: %d != 4", length);
            if (streamId == 0) throw ioException("TYPE_RST_STREAM streamId == 0");
            int errorCodeInt = source.readInt();
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw ioException("TYPE_RST_STREAM unexpected error code: %d", errorCodeInt);
            }
            handler.rstStream(streamId, errorCode);
        }

        private void readSettings(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (streamId != 0) throw ioException("TYPE_SETTINGS streamId != 0");
            if ((flags & FLAG_ACK) != 0) {
                if (length != 0) throw ioException("FRAME_SIZE_ERROR ack frame should be empty!");
                handler.ackSettings();
                return;
            }

            if (length % 5 != 0) throw ioException("TYPE_SETTINGS length %% 5 != 0: %s", length);
            Settings settings = new Settings();
            for (int i = 0; i < length; i += 5) {
                int id = source.readByte();
                int value = source.readInt();

                switch (id) {
                    case 1: // SETTINGS_HEADER_TABLE_SIZE
                        break;
                    case 2: // SETTINGS_ENABLE_PUSH
                        if (value != 0 && value != 1) {
                            throw ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1");
                        }
                        break;
                    case 3: // SETTINGS_MAX_CONCURRENT_STREAMS
                        id = 4; // Renumbered in draft 10.
                        break;
                    case 4: // SETTINGS_INITIAL_WINDOW_SIZE
                        id = 7; // Renumbered in draft 10.
                        if (value < 0) {
                            throw ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1");
                        }
                        break;
                    case 5: // SETTINGS_COMPRESS_DATA
                        break;
                    default:
                        throw ioException("PROTOCOL_ERROR invalid settings id: %s", id);
                }
                settings.set(id, 0, value);
            }
            handler.settings(false, settings);
            if (settings.getHeaderTableSize() >= 0) {
                hpackReader.maxHeaderTableByteCountSetting(settings.getHeaderTableSize());
            }
        }

        private void readPushPromise(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (streamId == 0) {
                throw ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0");
            }
            short padding = readPadding(source, flags);
            int promisedStreamId = source.readInt() & 0x7fffffff;
            length -= 4; // account for above read.
            List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);
            handler.pushPromise(streamId, promisedStreamId, headerBlock);
        }

        private void readPing(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (length != 8) throw ioException("TYPE_PING length != 8: %s", length);
            if (streamId != 0) throw ioException("TYPE_PING streamId != 0");
            int payload1 = source.readInt();
            int payload2 = source.readInt();
            boolean ack = (flags & FLAG_ACK) != 0;
            handler.ping(ack, payload1, payload2);
        }

        private void readGoAway(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (length < 8) throw ioException("TYPE_GOAWAY length < 8: %s", length);
            if (streamId != 0) throw ioException("TYPE_GOAWAY streamId != 0");
            int lastStreamId = source.readInt();
            int errorCodeInt = source.readInt();
            int opaqueDataLength = length - 8;
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw ioException("TYPE_GOAWAY unexpected error code: %d", errorCodeInt);
            }
            ByteString debugData = EMPTY;
            if (opaqueDataLength > 0) { // Must read debug data in order to not corrupt the connection.
                debugData = source.readByteString(opaqueDataLength);
            }
            handler.goAway(lastStreamId, errorCode, debugData);
        }

        private void readWindowUpdate(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            if (length != 4) throw ioException("TYPE_WINDOW_UPDATE length !=4: %s", length);
            long increment = (source.readInt() & 0x7fffffffL);
            if (increment == 0) throw ioException("windowSizeIncrement was 0", increment);
            handler.windowUpdate(streamId, increment);
        }

        private void readAlternateService(Handler handler, short length, byte flags, int streamId)
                throws IOException {
            long maxAge = source.readInt() & 0xffffffffL;
            int port = source.readShort() & 0xffff;
            source.readByte(); // Reserved.
            int protocolLength = source.readByte() & 0xff;
            ByteString protocol = source.readByteString(protocolLength);
            int hostLength = source.readByte() & 0xff;
            String host = source.readUtf8(hostLength);
            int originLength = length - 9 - protocolLength - hostLength;
            String origin = source.readUtf8(originLength);
            handler.alternateService(streamId, origin, protocol, host, port, maxAge);
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    static final class Writer implements FrameWriter {
        private final BufferedSink sink;
        private final boolean client;
        private final Buffer hpackBuffer;
        private final HpackDraft07.Writer hpackWriter;
        private boolean closed;

        Writer(BufferedSink sink, boolean client) {
            this.sink = sink;
            this.client = client;
            this.hpackBuffer = new Buffer();
            this.hpackWriter = new HpackDraft07.Writer(hpackBuffer);
        }

        @Override
        public synchronized void flush() throws IOException {
            if (closed) throw new IOException("closed");
            sink.flush();
        }

        @Override
        public synchronized void ackSettings() throws IOException {
            if (closed) throw new IOException("closed");
            int length = 0;
            byte type = TYPE_SETTINGS;
            byte flags = FLAG_ACK;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            sink.flush();
        }

        @Override
        public synchronized void connectionPreface() throws IOException {
            if (closed) throw new IOException("closed");
            if (!client) return; // Nothing to write; servers don't send connection headers!
            if (logger.isLoggable(FINE)) {
                logger.fine(format(">> CONNECTION %s", CONNECTION_PREFACE.hex()));
            }
            sink.write(CONNECTION_PREFACE.toByteArray());
            sink.flush();
        }

        @Override
        public synchronized void synStream(boolean outFinished, boolean inFinished,
                                           int streamId, int associatedStreamId, List<Header> headerBlock)
                throws IOException {
            if (inFinished) throw new UnsupportedOperationException();
            if (closed) throw new IOException("closed");
            headers(outFinished, streamId, headerBlock);
        }

        @Override
        public synchronized void synReply(boolean outFinished, int streamId,
                                          List<Header> headerBlock) throws IOException {
            if (closed) throw new IOException("closed");
            headers(outFinished, streamId, headerBlock);
        }

        @Override
        public synchronized void headers(int streamId, List<Header> headerBlock)
                throws IOException {
            if (closed) throw new IOException("closed");
            headers(false, streamId, headerBlock);
        }

        @Override
        public synchronized void pushPromise(int streamId, int promisedStreamId,
                                             List<Header> requestHeaders) throws IOException {
            if (closed) throw new IOException("closed");
            if (hpackBuffer.size() != 0) throw new IllegalStateException();
            hpackWriter.writeHeaders(requestHeaders);

            int length = (int) (4 + hpackBuffer.size());
            byte type = TYPE_PUSH_PROMISE;
            byte flags = FLAG_END_HEADERS;
            frameHeader(streamId, length, type, flags); // TODO: CONTINUATION
            sink.writeInt(promisedStreamId & 0x7fffffff);
            sink.writeAll(hpackBuffer);
        }

        private void headers(boolean outFinished, int streamId, List<Header> headerBlock)
                throws IOException {
            if (closed) throw new IOException("closed");
            if (hpackBuffer.size() != 0) throw new IllegalStateException();
            hpackWriter.writeHeaders(headerBlock);

            int length = (int) hpackBuffer.size();
            byte type = TYPE_HEADERS;
            byte flags = FLAG_END_HEADERS;
            if (outFinished) flags |= FLAG_END_STREAM;
            frameHeader(streamId, length, type, flags); // TODO: CONTINUATION
            sink.writeAll(hpackBuffer);
        }

        @Override
        public synchronized void rstStream(int streamId, ErrorCode errorCode)
                throws IOException {
            if (closed) throw new IOException("closed");
            if (errorCode.spdyRstCode == -1) throw new IllegalArgumentException();

            int length = 4;
            byte type = TYPE_RST_STREAM;
            byte flags = FLAG_NONE;
            frameHeader(streamId, length, type, flags);
            sink.writeInt(errorCode.httpCode);
            sink.flush();
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, Buffer source)
                throws IOException {
            data(outFinished, streamId, source, (int) source.size());
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, Buffer source,
                                      int byteCount) throws IOException {
            if (closed) throw new IOException("closed");
            byte flags = FLAG_NONE;
            if (outFinished) flags |= FLAG_END_STREAM;
            dataFrame(streamId, flags, source, byteCount);
        }

        void dataFrame(int streamId, byte flags, Buffer buffer, int byteCount) throws IOException {
            byte type = TYPE_DATA;
            frameHeader(streamId, byteCount, type, flags);
            if (byteCount > 0) {
                sink.write(buffer, byteCount);
            }
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            if (closed) throw new IOException("closed");
            int length = settings.size() * 5;
            byte type = TYPE_SETTINGS;
            byte flags = FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            for (int i = 0; i < Settings.COUNT; i++) {
                if (!settings.isSet(i)) continue;
                int id = i;
                if (id == 4) id = 3; // SETTINGS_MAX_CONCURRENT_STREAMS renumbered.
                else if (id == 7) id = 4; // SETTINGS_INITIAL_WINDOW_SIZE renumbered.
                sink.writeByte(id);
                sink.writeInt(settings.get(i));
            }
            sink.flush();
        }

        @Override
        public synchronized void ping(boolean ack, int payload1, int payload2)
                throws IOException {
            if (closed) throw new IOException("closed");
            int length = 8;
            byte type = TYPE_PING;
            byte flags = ack ? FLAG_ACK : FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            sink.writeInt(payload1);
            sink.writeInt(payload2);
            sink.flush();
        }

        @Override
        public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode,
                                        byte[] debugData) throws IOException {
            if (closed) throw new IOException("closed");
            if (errorCode.httpCode == -1) throw illegalArgument("errorCode.httpCode == -1");
            int length = 8 + debugData.length;
            byte type = TYPE_GOAWAY;
            byte flags = FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            sink.writeInt(lastGoodStreamId);
            sink.writeInt(errorCode.httpCode);
            if (debugData.length > 0) {
                sink.write(debugData);
            }
            sink.flush();
        }

        @Override
        public synchronized void windowUpdate(int streamId, long windowSizeIncrement)
                throws IOException {
            if (closed) throw new IOException("closed");
            if (windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL) {
                throw illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s",
                        windowSizeIncrement);
            }
            int length = 4;
            byte type = TYPE_WINDOW_UPDATE;
            byte flags = FLAG_NONE;
            frameHeader(streamId, length, type, flags);
            sink.writeInt((int) windowSizeIncrement);
            sink.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
            sink.close();
        }

        void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
            if (logger.isLoggable(FINE))
                logger.fine(formatHeader(false, streamId, length, type, flags));
            if (length > 16383)
                throw illegalArgument("FRAME_SIZE_ERROR length > 16383: %s", length);
            if ((streamId & 0x80000000) != 0)
                throw illegalArgument("reserved bit set: %s", streamId);
            sink.writeInt((length & 0x3fff) << 16 | (type & 0xff) << 8 | (flags & 0xff));
            sink.writeInt(streamId & 0x7fffffff);
        }
    }

    /**
     * Decompression of the header block occurs above the framing layer. This
     * class lazily reads continuation frames as they are needed by {@link
     * HpackDraft07.Reader#readHeaders()}.
     */
    static final class ContinuationSource implements Source {
        private final BufferedSource source;

        short length;
        byte flags;
        int streamId;

        short left;
        short padding;

        public ContinuationSource(BufferedSource source) {
            this.source = source;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            while (left == 0) {
                source.skip(padding);
                padding = 0;
                if ((flags & FLAG_END_HEADERS) != 0) return -1;
                readContinuationHeader();
                // TODO: test case for empty continuation header?
            }

            long read = source.read(sink, Math.min(byteCount, left));
            if (read == -1) return -1;
            left -= read;
            return read;
        }

        @Override
        public Timeout timeout() {
            return source.timeout();
        }

        @Override
        public void close() throws IOException {
        }

        private void readContinuationHeader() throws IOException {
            int previousStreamId = streamId;
            int w1 = source.readInt();
            int w2 = source.readInt();
            length = (short) ((w1 & 0x3fff0000) >> 16);
            byte type = (byte) ((w1 & 0xff00) >> 8);
            flags = (byte) (w1 & 0xff);
            if (logger.isLoggable(FINE))
                logger.fine(formatHeader(true, streamId, length, type, flags));
            padding = readPadding(source, flags);
            length = left = lengthWithoutPadding(length, flags, padding);
            streamId = (w2 & 0x7fffffff);
            if (type != TYPE_CONTINUATION) throw ioException("%s != TYPE_CONTINUATION", type);
            if (streamId != previousStreamId)
                throw ioException("TYPE_CONTINUATION streamId changed");
        }
    }

    /**
     * Logs a human-readable representation of HTTP/2 frame headers.
     * <p/>
     * <p>The format is:
     * <p/>
     * <pre>
     *   direction streamID length type flags
     * </pre>
     * Where direction is {@code <<} for inbound and {@code >>} for outbound.
     * <p/>
     * <p> For example, the following would indicate a HEAD request sent from
     * the client.
     * <pre>
     * {@code
     *   << 0x0000000f    12 HEADERS       END_HEADERS|END_STREAM
     * }
     * </pre>
     */
    static final class FrameLogger {

        /**
         * Lookup table for valid frame types.
         */
        private static final String[] TYPES = new String[]{
                "DATA",
                "HEADERS",
                "PRIORITY",
                "RST_STREAM",
                "SETTINGS",
                "PUSH_PROMISE",
                "PING",
                "GOAWAY",
                "WINDOW_UPDATE",
                "CONTINUATION",
                "ALTSVC",
                "BLOCKED"
        };
        /**
         * Lookup table for valid flags for DATA, HEADERS, CONTINUATION. Invalid
         * combinations are represented in binary.
         */
        private static final String[] FLAGS = new String[0x40]; // Highest bit flag is 0x20.
        private static final String[] BINARY = new String[256];

        static {
            for (int i = 0; i < BINARY.length; i++) {
                BINARY[i] = format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            }

            FLAGS[FLAG_NONE] = "";
            FLAGS[FLAG_END_STREAM] = "END_STREAM";
            FLAGS[FLAG_END_SEGMENT] = "END_SEGMENT";
            FLAGS[FLAG_END_STREAM | FLAG_END_SEGMENT] = "END_STREAM|END_SEGMENT";
            int[] prefixFlags =
                    new int[]{FLAG_END_STREAM, FLAG_END_SEGMENT, FLAG_END_SEGMENT | FLAG_END_STREAM};

            FLAGS[FLAG_PAD_LOW] = "PAD_LOW";
            FLAGS[FLAG_PAD_LOW | FLAG_PAD_HIGH] = "PAD_LOW|PAD_HIGH";
            int[] suffixFlags = new int[]{FLAG_PAD_LOW, FLAG_PAD_LOW | FLAG_PAD_HIGH};

            for (int prefixFlag : prefixFlags) {
                for (int suffixFlag : suffixFlags) {
                    FLAGS[prefixFlag | suffixFlag] = FLAGS[prefixFlag] + '|' + FLAGS[suffixFlag];
                }
            }

            FLAGS[FLAG_END_HEADERS] = "END_HEADERS"; // Same as END_PUSH_PROMISE.
            FLAGS[FLAG_PRIORITY] = "PRIORITY"; // Same as FLAG_COMPRESSED.
            FLAGS[FLAG_END_HEADERS | FLAG_PRIORITY] = "END_HEADERS|PRIORITY"; // Only valid on HEADERS.
            int[] frameFlags =
                    new int[]{FLAG_END_HEADERS, FLAG_PRIORITY, FLAG_END_HEADERS | FLAG_PRIORITY};

            for (int frameFlag : frameFlags) {
                for (int prefixFlag : prefixFlags) {
                    FLAGS[prefixFlag | frameFlag] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag];
                    for (int suffixFlag : suffixFlags) {
                        FLAGS[prefixFlag | frameFlag | suffixFlag] =
                                FLAGS[prefixFlag] + '|' + FLAGS[frameFlag] + '|' + FLAGS[suffixFlag];
                    }
                }
            }

            for (int i = 0; i < FLAGS.length; i++) { // Fill in holes with binary representation.
                if (FLAGS[i] == null) FLAGS[i] = BINARY[i];
            }
        }

        static String formatHeader(boolean inbound, int streamId, int length, byte type, byte flags) {
            String formattedType = type < TYPES.length ? TYPES[type] : format("0x%02x", type);
            String formattedFlags = formatFlags(type, flags);
            return format("%s 0x%08x %5d %-13s %s", inbound ? "<<" : ">>", streamId, length,
                    formattedType, formattedFlags);
        }

        /**
         * Looks up valid string representing flags from the table. Invalid
         * combinations are represented in binary.
         */
        // Visible for testing.
        static String formatFlags(byte type, byte flags) {
            if (flags == 0) return "";
            switch (type) { // Special case types that have 0 or 1 flag.
                case TYPE_SETTINGS:
                case TYPE_PING:
                    return flags == FLAG_ACK ? "ACK" : BINARY[flags];
                case TYPE_PRIORITY:
                case TYPE_RST_STREAM:
                case TYPE_GOAWAY:
                case TYPE_WINDOW_UPDATE:
                case TYPE_ALTSVC:
                case TYPE_BLOCKED:
                    return BINARY[flags];
            }
            String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
            // Special case types that have overlap flag values.
            if (type == TYPE_PUSH_PROMISE && (flags & FLAG_END_PUSH_PROMISE) != 0) {
                return result.replace("HEADERS", "PUSH_PROMISE"); // TODO: Avoid allocation.
            } else if (type == TYPE_DATA && (flags & FLAG_COMPRESSED) != 0) {
                return result.replace("PRIORITY", "COMPRESSED"); // TODO: Avoid allocation.
            }
            return result;
        }
    }
}
