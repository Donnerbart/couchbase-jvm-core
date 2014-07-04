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
package com.couchbase.client.core.cluster;

import com.couchbase.client.core.message.ResponseStatus;
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
import com.couchbase.client.core.util.ClusterDependentTest;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import static org.junit.Assert.assertEquals;

/**
 * Verifies basic functionality of binary operations.
 *
 * @author Michael Nitschinger
 * @author David Sondermann
 * @since 1.0
 */
public class BinaryMessageTest extends ClusterDependentTest {

    @BeforeClass
    public static void setup() {
        flush();
    }

    @AfterClass
    public static void teardown() {
        flush();
    }

    @Test
    public void shouldUpsertAndGetDocument() throws Exception {
        final String key = "upsert-key";
        final String content = "Hello World!";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final UpsertRequest upsert = new UpsertRequest(document, bucket());
        cluster().<UpsertResponse>send(upsert).toBlocking().single();

        final GetRequest request = new GetRequest(key, bucket());
        assertEquals(content, cluster().<GetResponse>send(request).toBlocking().single().document().content().toString(CharsetUtil.UTF_8));
    }

    @Test
    public void shouldUpsertWithExpiration() throws Exception {
        final String key = "upsert-key-vanish";
        final String content = "Hello World!";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 1, 0, false, null);

        final UpsertRequest upsert = new UpsertRequest(document, bucket());
        cluster().<UpsertResponse>send(upsert).toBlocking().single();

        Thread.sleep(2000);

        final GetRequest request = new GetRequest(key, bucket());
        assertEquals(ResponseStatus.NOT_EXISTS, cluster().<GetResponse>send(request).toBlocking().single().status());
    }

    @Test
    public void shouldHandleDoubleInsert() {
        final String key = "insert-key";
        final String content = "Hello World!";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final InsertRequest firstInsert = new InsertRequest(document, bucket());
        final InsertResponse firstResponse = cluster().<InsertResponse>send(firstInsert).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, firstResponse.status());

        final InsertRequest secondInsert = new InsertRequest(firstResponse.document(), bucket());
        assertEquals(ResponseStatus.EXISTS, cluster().<InsertResponse>send(secondInsert).toBlocking().single().status());
    }

    @Test
    public void shouldReplaceWithoutCAS() {
        final String key = "replace-key";
        final String content = "replace content";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final ReplaceRequest insert = new ReplaceRequest(document, bucket());
        final ReplaceResponse firstReplace = cluster().<ReplaceResponse>send(insert).toBlocking().single();
        assertEquals(ResponseStatus.NOT_EXISTS, firstReplace.status());

        final UpsertRequest upsert = new UpsertRequest(firstReplace.document(), bucket());
        final ReplaceResponse secondReplace = cluster().<UpsertResponse>send(upsert)
                .flatMap(new Func1<UpsertResponse, Observable<ReplaceResponse>>() {
                             @Override
                             public Observable<ReplaceResponse> call(UpsertResponse response) {
                                 return cluster().send(new ReplaceRequest(response.document(), bucket()));
                             }
                         }
                ).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, secondReplace.status());
    }

    @Test
    public void shouldReplaceWithFailingCAS() {
        final String key = "replace-key-cas-fail";
        final String content = "replace content";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final ReplaceRequest insert = new ReplaceRequest(document, bucket());
        final ReplaceResponse firstReplace = cluster().<ReplaceResponse>send(insert).toBlocking().single();
        assertEquals(ResponseStatus.NOT_EXISTS, firstReplace.status());

        final UpsertRequest upsert = new UpsertRequest(firstReplace.document(), bucket());
        final ReplaceResponse secondReplace = cluster().<UpsertResponse>send(upsert)
                .flatMap(new Func1<UpsertResponse, Observable<ReplaceResponse>>() {
                    @Override
                    public Observable<ReplaceResponse> call(UpsertResponse response) {
                        final CoreDocument upsertDocument = new CoreDocument(
                                response.document().id(),
                                response.document().content(),
                                response.document().flags(),
                                response.document().expiration(),
                                24234234L,
                                response.document().isJson(),
                                response.status()
                        );
                        return cluster().send(new ReplaceRequest(upsertDocument, bucket()));
                    }
                }).toBlocking().single();
        assertEquals(ResponseStatus.EXISTS, secondReplace.status());
    }

    @Test
    public void shouldReplaceWithMatchingCAS() {
        final String key = "replace-key-cas-match";
        final String content = "replace content";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final ReplaceRequest insert = new ReplaceRequest(document, bucket());
        final ReplaceResponse firstResponse = cluster().<ReplaceResponse>send(insert).toBlocking().single();
        assertEquals(ResponseStatus.NOT_EXISTS, firstResponse.status());

        final UpsertRequest upsert = new UpsertRequest(firstResponse.document(), bucket());
        final ReplaceResponse secondResponse = cluster().<UpsertResponse>send(upsert)
                .flatMap(new Func1<UpsertResponse, Observable<ReplaceResponse>>() {
                    @Override
                    public Observable<ReplaceResponse> call(final UpsertResponse response) {
                        final CoreDocument replaceDocument = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, response.cas(), false, null);
                        return cluster().send(new ReplaceRequest(replaceDocument, bucket()));
                    }
                }).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, secondResponse.status());
    }

    @Test
    public void shouldRemoveDocumentWithoutCAS() {
        final String key = "remove-key";
        final String content = "Hello World!";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final UpsertRequest upsert = new UpsertRequest(document, bucket());
        assertEquals(ResponseStatus.SUCCESS, cluster().<UpsertResponse>send(upsert).toBlocking().single().status());

        final RemoveRequest remove = new RemoveRequest(key, bucket());
        assertEquals(ResponseStatus.SUCCESS, cluster().<RemoveResponse>send(remove).toBlocking().single().status());

        final GetRequest get = new GetRequest(key, bucket());
        assertEquals(ResponseStatus.NOT_EXISTS, cluster().<GetResponse>send(get).toBlocking().single().status());
    }

    @Test
    public void shouldRemoveDocumentWithCAS() {
        final String key = "remove-key-cas";
        final String content = "Hello World!";
        final CoreDocument document = new CoreDocument(key, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), 0, 0, 0, false, null);

        final UpsertRequest upsert = new UpsertRequest(document, bucket());
        final UpsertResponse upsertResponse = cluster().<UpsertResponse>send(upsert).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsertResponse.status());

        final RemoveRequest firstRemove = new RemoveRequest(key, 1233443, bucket());
        assertEquals(ResponseStatus.EXISTS, cluster().<RemoveResponse>send(firstRemove).toBlocking().single().status());

        final RemoveRequest secondRemove = new RemoveRequest(key, upsertResponse.cas(), bucket());
        assertEquals(ResponseStatus.SUCCESS, cluster().<RemoveResponse>send(secondRemove).toBlocking().single().status());
    }

}
