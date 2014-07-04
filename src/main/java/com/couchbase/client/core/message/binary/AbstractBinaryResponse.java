package com.couchbase.client.core.message.binary;

import com.couchbase.client.core.message.AbstractCouchbaseResponse;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.document.CoreDocument;

/**
 * @author Michael Nitschinger
 * @author David Sondermann
 * @since 2.0
 */
public abstract class AbstractBinaryResponse extends AbstractCouchbaseResponse implements BinaryResponse {

    private final CoreDocument coreDocument;
    private final String bucket;

    protected AbstractBinaryResponse(final CoreDocument coreDocument, final String bucket, final CouchbaseRequest request) {
        super(null, request);
        this.coreDocument = coreDocument;
        this.bucket = bucket;
    }

    @Override
    public ResponseStatus status() {
        return coreDocument.status();
    }

    @Override
    public CoreDocument document() {
        return coreDocument;
    }

    @Override
    public String bucket() {
        return bucket;
    }

    public long cas() {
        return document().cas();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BinaryResponse{");
        sb.append("bucket='").append(bucket).append('\'');
        sb.append(", status=").append(status());
        sb.append(", request=").append(request());
        sb.append(", coreDocument=").append(coreDocument);
        sb.append('}');
        return sb.toString();
    }
}
