package com.couchbase.client.core.message.binary;

import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.document.CoreDocument;

public class RemoveResponse extends AbstractBinaryResponse {

    public RemoveResponse(final CoreDocument document, final String bucket, final CouchbaseRequest request) {
        super(document, bucket, request);
    }

}
