package com.couchbase.client.core.node.locate;

import com.couchbase.client.core.config.ClusterConfig;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.config.DefaultPartition;
import com.couchbase.client.core.config.Partition;
import com.couchbase.client.core.message.binary.GetRequest;
import com.couchbase.client.core.node.Node;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link BinaryLocator}.
 */
public class BinaryLocatorTest {

    @Test
    public void shouldLocateGetRequestForCouchbaseBucket() throws Exception {
        Locator locator = new BinaryLocator();

        GetRequest getRequestMock = mock(GetRequest.class);
        ClusterConfig configMock = mock(ClusterConfig.class);
        Set<Node> nodes = new HashSet<Node>();
        Node node1Mock = mock(Node.class);
        when(node1Mock.hostname()).thenReturn(InetAddress.getByName("192.168.56.101"));
        Node node2Mock = mock(Node.class);
        when(node2Mock.hostname()).thenReturn(InetAddress.getByName("192.168.56.102"));
        nodes.addAll(Arrays.asList(node1Mock, node2Mock));
        CouchbaseBucketConfig bucketMock = mock(CouchbaseBucketConfig.class);
        when(getRequestMock.bucket()).thenReturn("bucket");
        when(getRequestMock.key()).thenReturn("key");
        when(configMock.bucketConfig("bucket")).thenReturn(bucketMock);
        when(bucketMock.partitions()).thenReturn(Arrays.asList(
            new DefaultPartition((short) 0, new short[] {1}),
            new DefaultPartition((short) 0, new short[] {1}),
            new DefaultPartition((short) 1, new short[] {0}),
            (Partition) new DefaultPartition((short) 1, new short[] {0})
        ));
        when(bucketMock.partitionHosts()).thenReturn(Arrays.asList(
           "192.168.56.101",
            "192.168.56.102"
        ));

        Node[] foundNodes = locator.locate(getRequestMock, nodes, configMock);
        assertEquals(node1Mock, foundNodes[0]);
    }

    @Test
    public void shouldFanOutBroadcastableRequest() {

    }

}
