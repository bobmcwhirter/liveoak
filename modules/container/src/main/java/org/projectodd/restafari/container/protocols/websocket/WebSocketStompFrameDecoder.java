package org.projectodd.restafari.container.protocols.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.projectodd.restafari.stomp.common.StompFrameDecoder;

import java.util.List;

/** WebSocket to STOMP frame decoder.
 *
 * This decoder also adds the normal "native" STOMP frame decoder upstream from itself,
 * providing for the pipeline of:
 *
 * WebSocket Frame to bytes to STOMP frame
 *
 * @author Bob McWhirter
 */
public class WebSocketStompFrameDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addAfter( ctx.name(), "stomp-frame-decoder", new StompFrameDecoder() );
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
        if ( msg instanceof TextWebSocketFrame || msg instanceof BinaryWebSocketFrame ) {
            out.add( msg.content().retain() );
        } else {
            out.add( msg.retain() );
        }
    }

}
