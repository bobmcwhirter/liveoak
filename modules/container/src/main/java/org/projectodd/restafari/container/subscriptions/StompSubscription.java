package org.projectodd.restafari.container.subscriptions;

import org.projectodd.restafari.container.ResourcePath;
import org.projectodd.restafari.container.codec.ResourceCodec;
import org.projectodd.restafari.container.codec.ResourceEncoder;
import org.projectodd.restafari.spi.Resource;
import org.projectodd.restafari.spi.state.ResourceState;
import org.projectodd.restafari.stomp.Headers;
import org.projectodd.restafari.stomp.StompMessage;
import org.projectodd.restafari.stomp.common.DefaultStompMessage;
import org.projectodd.restafari.stomp.server.StompConnection;

import java.io.IOException;

/**
 * @author Bob McWhirter
 */
public class StompSubscription implements Subscription {

    public StompSubscription(StompConnection connection, String destination, String subscriptionId, String contentType, ResourceCodec codec) {
        this.connection = connection;
        this.destination = destination;
        this.subscriptionId = subscriptionId;
        this.contentType = contentType;
        this.codec = codec;
        this.resourcePath = new ResourcePath(destination);
    }

    public ResourcePath resourcePath() {
        return this.resourcePath;
    }

    @Override
    public void resourceCreated(Resource resource) throws Exception {
        this.connection.send(createMessage("create", 200, resource));
    }

    @Override
    public void resourceUpdated(Resource resource) throws Exception {
        this.connection.send(createMessage("update", 200, resource));
    }

    @Override
    public void resourceDeleted(Resource resource) throws Exception {
        this.connection.send(createMessage("delete", 200, resource));
    }

    protected StompMessage createMessage(String action, int status, Resource resource) throws Exception {
        StompMessage message = new DefaultStompMessage();
        message.headers().put(Headers.SUBSCRIPTION, this.subscriptionId);
        message.headers().put(Headers.CONTENT_TYPE, this.contentType);
        message.headers().put("action", action);
        message.headers().put("status", "" + status);
        message.headers().put("location", resource.uri().toString() );
        message.content(this.codec.encode(resource));
        return message;
    }

    private StompConnection connection;
    private String destination;
    private String subscriptionId;
    private String contentType;
    private ResourceCodec codec;

    private final ResourcePath resourcePath;

}