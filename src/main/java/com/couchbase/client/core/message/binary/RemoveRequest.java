package com.couchbase.client.core.message.binary;

public class RemoveRequest extends AbstractKeyAwareBinaryRequest {

    private final String key;
    private final long cas;

    public RemoveRequest(final String key, final String bucket) {
        this(key, 0, bucket);
    }

    public RemoveRequest(final String key, final long cas, final String bucket) {
        super(bucket, null);
        this.key = key;
        this.cas = cas;
    }

    /**
     * The CAS value of the request.
     *
     * @return the cas value.
     */
    public long cas() {
        return cas;
    }

    @Override
    public String key() {
        return key;
    }

}
