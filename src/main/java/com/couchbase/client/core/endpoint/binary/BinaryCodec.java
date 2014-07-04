/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.core.endpoint.binary;

import com.couchbase.client.core.env.Environment;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.AbstractCoreDocumentBinaryRequest;
import com.couchbase.client.core.message.binary.AbstractKeyAwareBinaryRequest;
import com.couchbase.client.core.message.binary.BinaryRequest;
import com.couchbase.client.core.message.binary.GetBucketConfigRequest;
import com.couchbase.client.core.message.binary.GetBucketConfigResponse;
import com.couchbase.client.core.message.binary.GetRequest;
import com.couchbase.client.core.message.binary.GetResponse;
import com.couchbase.client.core.message.binary.InsertRequest;
import com.couchbase.client.core.message.binary.InsertResponse;
import com.couchbase.client.core.message.binary.RemoveRequest;
import com.couchbase.client.core.message.binary.RemoveResponse;
import com.couchbase.client.core.message.binary.ReplaceRequest;
import com.couchbase.client.core.message.binary.ReplaceResponse;
import com.couchbase.client.core.message.binary.UpsertRequest;
import com.couchbase.client.core.message.binary.UpsertResponse;
import com.couchbase.client.core.message.document.CoreDocument;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.compression.Snappy;
import io.netty.handler.codec.memcache.binary.BinaryMemcacheOpcodes;
import io.netty.handler.codec.memcache.binary.BinaryMemcacheRequest;
import io.netty.handler.codec.memcache.binary.BinaryMemcacheResponseStatus;
import io.netty.handler.codec.memcache.binary.DefaultBinaryMemcacheRequest;
import io.netty.handler.codec.memcache.binary.DefaultFullBinaryMemcacheRequest;
import io.netty.handler.codec.memcache.binary.FullBinaryMemcacheRequest;
import io.netty.handler.codec.memcache.binary.FullBinaryMemcacheResponse;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Codec that handles encoding of binary memcache requests and decoding of binary memcache responses.
 *
 * @author Michael Nitschinger
 * @author David Sondermann
 * @since 1.0
 */
public class BinaryCodec extends MessageToMessageCodec<FullBinaryMemcacheResponse, BinaryRequest> {

    /**
     * The Queue which holds the request types so that proper decoding can happen async.
     */
    private final Queue<BinaryRequest> queue;

    private final Snappy snappy = new Snappy();

    private final Environment env;

    /**
     * The bucket used.
     */
    private String bucket;

    /**
     * Default the datatypes to non-support.
     */
    private SupportedDatatypes datatypes = new SupportedDatatypes(false, false);

    /**
     * Creates a new {@link BinaryCodec} with the default dequeue.
     */
    public BinaryCodec(final Environment env) {
        this(env, new ArrayDeque<BinaryRequest>());
    }

    /**
     * Creates a new {@link BinaryCodec} with a custom dequeue.
     *
     * @param queue a custom queue to test encoding/decoding.
     */
    public BinaryCodec(final Environment env, final Queue<BinaryRequest> queue) {
        this.queue = queue;
        this.env = env;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final BinaryRequest msg, final List<Object> out) throws Exception {
        if (bucket == null) {
            bucket = msg.bucket();
        }

        if (msg instanceof GetBucketConfigRequest) {
            out.add(handleGetBucketConfigRequest());
        } else if (msg instanceof GetRequest) {
            out.add(handleGetRequest((GetRequest) msg));
        } else if (msg instanceof UpsertRequest) {
            out.add(handleUpsertRequest((UpsertRequest) msg, ctx));
        } else if (msg instanceof InsertRequest) {
            out.add(handleInsertRequest((InsertRequest) msg, ctx));
        } else if (msg instanceof ReplaceRequest) {
            out.add(handleReplaceRequest((ReplaceRequest) msg, ctx));
        } else if (msg instanceof RemoveRequest) {
            out.add(handleRemoveRequest((RemoveRequest) msg));
        } else {
            throw new IllegalArgumentException("Unknown message to encode: " + msg);
        }

        queue.offer(msg);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final FullBinaryMemcacheResponse msg, final List<Object> in) throws Exception {
        final BinaryRequest current = queue.poll();

        ByteBuf content = msg.content().copy();
        if (msg.getDataType() == 2 || msg.getDataType() == 3) {
            ByteBuf compressed = ctx.alloc().buffer();
            snappy.decode(content, compressed);
            content.release();
            content = compressed;
        }

        int flags = 0;
        int expiration = 0;
        if (msg.getExtrasLength() > 0) {
            // TODO get rid of superfluous allocation (at the moment msg.getExtras() is released somewhere before)
            final ByteBuf extrasReleased = msg.getExtras();
            final ByteBuf extras = ctx.alloc().buffer(msg.getExtrasLength());
            extras.writeBytes(extrasReleased, extrasReleased.readerIndex(), extrasReleased.readableBytes());
            flags = extras.getInt(0);
            if (msg.getExtrasLength() > 4) {
                expiration = extras.getInt(1);
            }
            extras.release();
        }

        final String key = (current instanceof AbstractKeyAwareBinaryRequest) ? ((AbstractKeyAwareBinaryRequest) current).key() : null;
        final boolean isJson = (msg.getDataType() == 1 || msg.getDataType() == 3);
        final ResponseStatus status = convertStatus(msg.getStatus());

        final CoreDocument document = new CoreDocument(key, content, flags, expiration, msg.getCAS(), isJson, status);
        final CouchbaseRequest currentRequest = (status == ResponseStatus.RETRY) ? current : null;

        if (current instanceof GetBucketConfigRequest) {
            final InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            in.add(new GetBucketConfigResponse(status, bucket, document.content(), InetAddress.getByName(address.getHostName())));
        } else if (current instanceof GetRequest) {
            in.add(new GetResponse(document, bucket, currentRequest));
        } else if (current instanceof InsertRequest) {
            in.add(new InsertResponse(document, bucket, currentRequest));
        } else if (current instanceof UpsertRequest) {
            in.add(new UpsertResponse(document, bucket, currentRequest));
        } else if (current instanceof ReplaceRequest) {
            in.add(new ReplaceResponse(document, bucket, currentRequest));
        } else if (current instanceof RemoveRequest) {
            in.add(new RemoveResponse(document, bucket, currentRequest));
        } else {
            throw new IllegalStateException("Got a response message for a request that was not sent: " + msg);
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof SupportedDatatypes) {
            datatypes = (SupportedDatatypes) evt;
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * Convert the binary protocol status in a type safe enum that can be acted upon later.
     *
     * @param status the status to convert.
     * @return the converted response status.
     */
    private ResponseStatus convertStatus(final short status) {
        switch (status) {
            case BinaryMemcacheResponseStatus.SUCCESS:
                return ResponseStatus.SUCCESS;
            case BinaryMemcacheResponseStatus.KEY_EEXISTS:
                return ResponseStatus.EXISTS;
            case BinaryMemcacheResponseStatus.KEY_ENOENT:
                return ResponseStatus.NOT_EXISTS;
            case 0x07: // Represents NOT_MY_VBUCKET
                return ResponseStatus.RETRY;
            default:
                return ResponseStatus.FAILURE;
        }
    }

    /**
     * Creates the actual protocol level request for an incoming get request.
     *
     * @param request the incoming get request.
     * @return the built protocol request.
     */
    private BinaryMemcacheRequest handleGetRequest(final GetRequest request) {
        final short keyLength = (short) request.key().length();

        final BinaryMemcacheRequest msg = new DefaultBinaryMemcacheRequest(request.key());
        msg.setOpcode(BinaryMemcacheOpcodes.GET);
        msg.setKeyLength(keyLength);
        msg.setTotalBodyLength(keyLength);
        msg.setReserved(request.partition());

        return msg;
    }

    /**
     * Creates the actual protocol level request for an incoming upsert request.
     *
     * @param request the incoming upsert request.
     * @param ctx     the channel handler context for buffer allocations.
     * @return the built protocol request.
     */
    private BinaryMemcacheRequest handleUpsertRequest(final UpsertRequest request, final ChannelHandlerContext ctx) {
        return createFullBinaryMemcacheRequest(BinaryMemcacheOpcodes.SET, request, ctx);
    }

    /**
     * Creates the actual protocol level request for an incoming replacer request.
     *
     * @param request the incoming replace request.
     * @param ctx     the channel handler context for buffer allocations.
     * @return the built protocol request.
     */
    private BinaryMemcacheRequest handleReplaceRequest(final ReplaceRequest request, final ChannelHandlerContext ctx) {
        return createFullBinaryMemcacheRequest(BinaryMemcacheOpcodes.REPLACE, request, ctx);
    }

    /**
     * Creates the actual protocol level request for an incoming insert request.
     *
     * @param request the incoming insert request.
     * @param ctx     the channel handler context for buffer allocations.
     * @return the built protocol request.
     */
    private BinaryMemcacheRequest handleInsertRequest(final InsertRequest request, final ChannelHandlerContext ctx) {
        return createFullBinaryMemcacheRequest(BinaryMemcacheOpcodes.ADD, request, ctx);
    }

    private BinaryMemcacheRequest handleRemoveRequest(final RemoveRequest request) {
        final BinaryMemcacheRequest msg = new DefaultBinaryMemcacheRequest(request.key());
        msg.setOpcode(BinaryMemcacheOpcodes.DELETE);
        msg.setCAS(request.cas());
        msg.setKeyLength((short) request.key().length());
        msg.setTotalBodyLength((short) request.key().length());
        msg.setReserved(request.partition());

        return msg;
    }

    /**
     * Creates the actual protocol level request for an incoming bucket config request.
     *
     * @return the built protocol request.
     */
    private BinaryMemcacheRequest handleGetBucketConfigRequest() {
        final BinaryMemcacheRequest msg = new DefaultBinaryMemcacheRequest();
        msg.setOpcode((byte) 0xb5);

        return msg;
    }

    private FullBinaryMemcacheRequest createFullBinaryMemcacheRequest(final byte opCode, final AbstractCoreDocumentBinaryRequest request, final ChannelHandlerContext ctx) {
        final CoreDocument document = request.document();
        final short keyLength = (short) document.id().length();

        final ByteBuf extras = ctx.alloc().buffer(8);
        extras.writeInt(document.flags());
        extras.writeInt(document.expiration());

        ByteBuf content = document.content();
        boolean compress = datatypes.compression() && env.compressionEnabled()
                && content.readableBytes() >= env.compressionLowerLimit();
        if (compress) {
            final ByteBuf compressed = ctx.alloc().buffer();
            snappy.encode(content, compressed, content.readableBytes());
            content.release();
            content = compressed;
        }

        final FullBinaryMemcacheRequest msg = new DefaultFullBinaryMemcacheRequest(document.id(), extras, content);
        msg.setOpcode(opCode);
        msg.setCAS(document.cas());
        msg.setKeyLength(keyLength);
        msg.setTotalBodyLength(keyLength + content.readableBytes() + extras.readableBytes());
        msg.setReserved(request.partition());
        msg.setExtrasLength((byte) extras.readableBytes());
        if (datatypes.json() && document.isJson()) {
            if (compress) {
                msg.setDataType((byte) 0x03);
            } else {
                msg.setDataType((byte) 0x01);
            }
        } else if (compress) {
            msg.setDataType((byte) 0x02);
        }

        return msg;
    }
}