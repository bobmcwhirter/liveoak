package org.projectodd.restafari.container;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import org.projectodd.restafari.container.protocols.PipelineConfigurator;

import java.net.InetAddress;

public abstract class AbstractServer {

    public AbstractServer(Container container, InetAddress host, int port, EventLoopGroup group) {
        this.container = container;
        this.host = host;
        this.port = port;
        this.group = group;
        this.pipelineConfigurator = new PipelineConfigurator( this.container );
    }

    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .channel(NioServerSocketChannel.class)
                .group(this.group)
                .localAddress(this.host, this.port)
                .childHandler(createChildHandler());
        ChannelFuture future = serverBootstrap.bind();
        future.sync();
    }

    public void stop() throws InterruptedException {
        System.err.println("stopping");
        Future<?> future = this.group.shutdownGracefully();
        future.sync();
        System.err.println("stopped");
    }

    protected PipelineConfigurator getPipelineConfigurator() {
        return this.pipelineConfigurator;
    }

    protected abstract ChannelHandler createChildHandler();

    private Container container;
    private int port;
    private InetAddress host;
    private EventLoopGroup group;
    private final PipelineConfigurator pipelineConfigurator;

}
