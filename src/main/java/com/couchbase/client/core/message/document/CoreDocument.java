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
package com.couchbase.client.core.message.document;

import com.couchbase.client.core.message.ResponseStatus;
import io.netty.buffer.ByteBuf;

/**
 * Core document to transfer content and flags between Couchbase client and Couchbase core io.
 *
 * @author David Sondermann
 * @since 2.0
 */
public class CoreDocument {
    private final String id;
    private final ByteBuf content;
    private final int flags;
    private final int expiration;
    private final long cas;
    private final boolean json;
    private final ResponseStatus status;

    public CoreDocument(final String id, final ByteBuf content, final int flags, final int expiration, final long cas,
                        final boolean json, final ResponseStatus status) {
        this.id = id;
        this.content = content;
        this.flags = flags;
        this.expiration = expiration;
        this.cas = cas;
        this.json = json;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public ByteBuf content() {
        return content;
    }

    public int flags() {
        return flags;
    }

    public int expiration() {
        return expiration;
    }

    public long cas() {
        return cas;
    }

    public boolean isJson() {
        return json;
    }

    public ResponseStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return "CoreDocument{" +
                "id='" + id + '\'' +
                ", content=" + content +
                ", flags=" + flags +
                ", expiration=" + expiration +
                ", cas=" + cas +
                ", json=" + json +
                ", status=" + status +
                '}';
    }
}
