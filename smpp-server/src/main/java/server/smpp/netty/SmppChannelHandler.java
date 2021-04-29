package server.smpp.netty;

import protocol.authentication.AuthenticationClient;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class SmppChannelHandler extends ChannelInitializer<SocketChannel> {

    private final @NonNull AuthenticationClient authenticationClient;

    @Override
    protected void initChannel(final SocketChannel socketChannel) {
        // nothing to initialize
    }
}
