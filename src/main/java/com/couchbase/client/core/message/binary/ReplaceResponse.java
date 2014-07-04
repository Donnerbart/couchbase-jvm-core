package com.couchbase.client.core.message.binary;

import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.document.CoreDocument;

public class ReplaceResponse extends AbstractBinaryResponse {

    public ReplaceResponse(final CoreDocument document, final String bucket, final CouchbaseRequest request) {
        super(document, bucket, request);
    }

}
