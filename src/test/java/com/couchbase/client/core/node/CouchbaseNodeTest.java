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
package com.couchbase.client.core.node;

import com.couchbase.client.core.env.CouchbaseEnvironment;
import com.couchbase.client.core.env.Environment;
import com.couchbase.client.core.message.internal.AddServiceRequest;
import com.couchbase.client.core.message.internal.RemoveServiceRequest;
import com.couchbase.client.core.service.BinaryService;
import com.couchbase.client.core.service.ConfigService;
import com.couchbase.client.core.service.Service;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.state.LifecycleState;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.net.InetAddress;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of a {@link CouchbaseNode}.
 */
public class CouchbaseNodeTest {

    private static final Environment environment = new CouchbaseEnvironment();


    private static InetAddress host;

    @BeforeClass
    public static void setup() throws Exception {
        host = InetAddress.getLocalHost();
    }

    @Test
    public void shouldBeDisconnectedIfNoServicesRegisteredOnConnect() {
        CouchbaseNode node = new CouchbaseNode(host, environment, null);
        assertEquals(LifecycleState.DISCONNECTED, node.connect().toBlocking().single());
    }

    @Test
    public void shouldBeConnectedIfAllServicesConnectedOnConnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.connect()).thenReturn(Observable.from(LifecycleState.CONNECTED));
        when(service2Mock.connect()).thenReturn(Observable.from(LifecycleState.CONNECTED));
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        assertEquals(LifecycleState.CONNECTED, node.connect().toBlocking().single());
    }

    @Test
    public void shouldBeDegradedIfAtLeastOneServiceConnectedOnConnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.connect()).thenReturn(Observable.from(LifecycleState.CONNECTED));
        when(service2Mock.connect()).thenReturn(Observable.from(LifecycleState.CONNECTING));
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        assertEquals(LifecycleState.DEGRADED, node.connect().toBlocking().single());
    }

    @Test
    public void shouldBeConnectingIfAtLeastOneServiceConnectingOnConnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.connect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));
        when(service2Mock.connect()).thenReturn(Observable.from(LifecycleState.CONNECTING));
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        assertEquals(LifecycleState.CONNECTING, node.connect().toBlocking().single());
    }

    @Test
    public void shouldBeDisconnectedIfNoServiceConnectingOnConnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.connect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));
        when(service2Mock.connect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        assertEquals(LifecycleState.DISCONNECTED, node.connect().toBlocking().single());
    }

    @Test
    public void shouldBeDisconnectingIfServicesDisconnectingOnDisconnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.disconnect()).thenReturn(Observable.from(LifecycleState.DISCONNECTING));
        when(service2Mock.disconnect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));

        BehaviorSubject<LifecycleState> states1 = BehaviorSubject.create();
        BehaviorSubject<LifecycleState> states2 = BehaviorSubject.create();
        when(service1Mock.states()).thenReturn(states1);
        when(service2Mock.states()).thenReturn(states2);
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        Observable<LifecycleState> disconnect = node.disconnect();
        states1.onNext(LifecycleState.DISCONNECTING);
        states2.onNext(LifecycleState.DISCONNECTED);
        assertEquals(LifecycleState.DISCONNECTING, disconnect.toBlocking().single());
    }

    @Test
    public void shouldBeDisconnectedIfServicesDisconnectedOnDisconnect() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service service1Mock = mock(Service.class);
        Service service2Mock = mock(Service.class);
        when(registryMock.services()).thenReturn(Arrays.asList(service1Mock, service2Mock));
        when(service1Mock.disconnect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));
        when(service2Mock.disconnect()).thenReturn(Observable.from(LifecycleState.DISCONNECTED));
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        assertEquals(LifecycleState.DISCONNECTED, node.disconnect().toBlocking().single());
    }

    @Test
    public void shouldRegisterGlobalService() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);
        Service registered = node.addService(new AddServiceRequest(ServiceType.CONFIG, null, null, 0, host))
            .toBlocking().single();

        verify(registryMock).addService(any(ConfigService.class), anyString());
        assertEquals(ServiceType.CONFIG, registered.type());
    }

    @Test
    public void shouldRegisterLocalService() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);
        Service registered = node.addService(new AddServiceRequest(ServiceType.BINARY, "bucket", null, 0, host))
            .toBlocking().single();

        verify(registryMock).addService(any(BinaryService.class), anyString());
        assertEquals(ServiceType.BINARY, registered.type());
    }

    @Test
    public void shouldRemoveGlobalService() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service serviceMock = mock(Service.class);
        when(registryMock.serviceBy(ServiceType.CONFIG, null)).thenReturn(serviceMock);
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        node.removeService(new RemoveServiceRequest(ServiceType.CONFIG, null, host))
            .toBlocking().single();
        verify(registryMock).removeService(any(Service.class), anyString());
    }

    @Test
    public void shouldRemoveLocalService() {
        ServiceRegistry registryMock = mock(ServiceRegistry.class);
        Service serviceMock = mock(Service.class);
        when(registryMock.serviceBy(ServiceType.BINARY, "bucket")).thenReturn(serviceMock);
        when(serviceMock.states()).thenReturn(Observable.<LifecycleState>empty());
        CouchbaseNode node = new CouchbaseNode(host, registryMock, environment, null);

        node.removeService(new RemoveServiceRequest(ServiceType.BINARY, "bucket", host))
            .toBlocking().single();
        verify(registryMock).removeService(any(Service.class), anyString());
    }
}
