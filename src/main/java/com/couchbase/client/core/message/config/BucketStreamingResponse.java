package com.couchbase.client.core.message.config;

import com.couchbase.client.core.message.AbstractCouchbaseResponse;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.ResponseStatus;
import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class BucketStreamingResponse extends AbstractCouchbaseResponse {

    private final Observable<String> configs;
    private final String host;

    public BucketStreamingResponse(Observable<String> configs, String host, ResponseStatus status,
        CouchbaseRequest request) {
        super(status, request);
        this.configs = configs;
        this.host = host;
    }

    public Observable<String> configs() {
        return configs;
    }

    public String host() {
        return host;
    }
}
