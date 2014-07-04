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
package com.couchbase.client.core.message.binary;

import com.couchbase.client.core.message.ResponseStatus;
import io.netty.buffer.ByteBuf;

import java.net.InetAddress;

/**
 * Represents a response with a bucket configuration.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class GetBucketConfigResponse extends AbstractBinaryResponse {

    private final ResponseStatus status;
    private final ByteBuf content;
    private final InetAddress hostname;

    public GetBucketConfigResponse(final ResponseStatus status, final String bucket, final ByteBuf content, final InetAddress hostname) {
        super(null, bucket, null);
        this.status = status;
        this.content = content;
        this.hostname = hostname;
    }

    public ResponseStatus status() {
        return status;
    }

    public ByteBuf content() {
        return content;
    }

    public InetAddress hostname() {
        return hostname;
    }
}
