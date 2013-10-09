package org.projectodd.restafari.stomp.server.protocol;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.projectodd.restafari.stomp.Headers;
import org.projectodd.restafari.stomp.Stomp;
import org.projectodd.restafari.stomp.common.AbstractControlFrameHandler;
import org.projectodd.restafari.stomp.common.StompControlFrame;
import org.projectodd.restafari.stomp.server.ServerContext;
import org.projectodd.restafari.stomp.server.StompConnection;

/**
 * @author Bob McWhirter
 */
public class DisconnectHandler extends AbstractControlFrameHandler {

    public DisconnectHandler(ServerContext serverContext) {
        super(Stomp.Command.DISCONNECT);
        this.serverContext = serverContext;
    }

    @Override
    protected void handleControlFrame(ChannelHandlerContext ctx, StompControlFrame frame) throws Exception {
        System.err.println( "handling disconnect" );
        StompConnection stompConnection = ctx.attr(ConnectHandler.CONNECTION).get();
        this.serverContext.handleDisconnect(stompConnection);
        String receiptId = frame.getHeader(Headers.RECEIPT);
        ChannelFuture future = ctx.write(StompControlFrame.newReceiptFrame(receiptId));
        ctx.flush();
        future.addListener((f) -> {
            System.err.println( "server close" );
            ctx.close();
        });
    }

    private ServerContext serverContext;
}
