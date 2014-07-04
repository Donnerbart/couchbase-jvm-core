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
package com.couchbase.client.core;

import com.couchbase.client.core.config.ConfigurationException;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.OpenBucketResponse;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.core.util.TestProperties;
import org.junit.Test;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Verifies basic bucket lifecycles like opening and closing of valid and invalid buckets.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class BucketLifecycleTest {

    @Test
    public void shouldSuccessfullyOpenBucket() {
        final CouchbaseCore core = new CouchbaseCore();
        core.send(new SeedNodesRequest(Arrays.asList(TestProperties.seedNode())));

        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket(), TestProperties.password());
        final Observable<OpenBucketResponse> response = core.send(request);

        assertEquals(ResponseStatus.SUCCESS, response.toBlocking().single().status());
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailWithNoSeedNodeList() {
        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket(), TestProperties.password());
        new CouchbaseCore().send(request).toBlocking().single();
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailWithEmptySeedNodeList() {
        final CouchbaseCore core = new CouchbaseCore();
        core.send(new SeedNodesRequest(Collections.<String>emptyList()));

        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket(), TestProperties.password());
        core.send(request).toBlocking().single();
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailOpeningNonExistentBucket() {
        final CouchbaseCore core = new CouchbaseCore();
        core.send(new SeedNodesRequest(Arrays.asList(TestProperties.seedNode())));

        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket() + "asd", TestProperties.password());
        core.send(request).toBlocking().single();
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailOpeningBucketWithWrongPassword() {
        final CouchbaseCore core = new CouchbaseCore();
        core.send(new SeedNodesRequest(Arrays.asList(TestProperties.seedNode())));

        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket(), TestProperties.password() + "asd");
        core.send(request).toBlocking().single();
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailOpeningWithWrongHost() {
        final CouchbaseCore core = new CouchbaseCore();
        core.send(new SeedNodesRequest(Arrays.asList("certainlyInvalidHostname")));

        final OpenBucketRequest request = new OpenBucketRequest(TestProperties.bucket(), TestProperties.password() + "asd");
        core.send(request).toBlocking().single();
    }

}
