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

import com.couchbase.client.core.message.AbstractCouchbaseResponse;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.document.CoreDocument;
import io.netty.util.CharsetUtil;

/**
 * Represents a response to a {@link com.couchbase.client.core.message.binary.AbstractCoreDocumentBinaryRequest}.
 *
 * @author David Sondermann
 * @since 2.0
 */
public abstract class AbstractCoreDocumentBinaryResponse extends AbstractCouchbaseResponse implements BinaryResponse {

    private final CoreDocument document;
    private final String bucket;

    public AbstractCoreDocumentBinaryResponse(final CoreDocument document, final String bucket, final CouchbaseRequest request) {
        super(document.status(), request);
        this.document = document;
        this.bucket = bucket;
    }

    @Override
    public CoreDocument document() {
        return document;
    }

    @Override
    public String bucket() {
        return bucket;
    }

    protected String toStringInternal(final String className) {
        return className + '{' + "bucket='" + bucket() + '\'' + ", status=" + status() + ", cas=" + document.cas()
                + ", flags=" + document.flags() + ", request=" + request()
                + ", content=" + document.content().toString(CharsetUtil.UTF_8) + '}';
    }
}
