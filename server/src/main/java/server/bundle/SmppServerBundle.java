package server.bundle;

import server.smpp.netty.SmppChannelHandler;

import io.netty.bootstrap.ServerBootstrap;
import protocol.authentication.AuthenticationClient;
import protocol.configuration.IdentityConfiguration;
import protocol.configuration.ImmutableIdentityConfiguration;
import protocol.configuration.ImmutableIdentityDynamoDBConfiguration;
import server.client.AuthenticationClientImpl;
import server.configuration.SmppProxyConfiguration;

public class SmppServerBundle {

    private static ServerBootstrap createServerBootstrap(final SmppProxyConfiguration smppProxyConfiguration) {
        final var identityConfig = createIdentityConfig(smppProxyConfiguration);
        final var identityClient = createIdentityClient(identityConfig);
        final SmppChannelHandler smppChannelHandler = createSmppChannelHandler(identityClient);
        return new ServerBootstrap().childHandler(smppChannelHandler);
    }

    private static SmppChannelHandler createSmppChannelHandler(final AuthenticationClient identityClient) {
        return SmppChannelHandler.builder().authenticationClient(identityClient).build();
    }

    private static IdentityConfiguration createIdentityConfig(final SmppProxyConfiguration configuration) {
        final var identityModuleConfig = configuration.getIdentityModuleConfiguration();
        final var identityDynamoConfig = identityModuleConfig.dynamoDbConfiguration();

        return ImmutableIdentityConfiguration.builder()
                .dynamoDBConfiguration(ImmutableIdentityDynamoDBConfiguration.builder()
                        .local(identityDynamoConfig.local())
                        .retries(identityDynamoConfig.retries())
                        .endpoint(identityDynamoConfig.endpoint())
                        .region(identityDynamoConfig.region())
                        .tableName(identityDynamoConfig.tableName())
                        .build())
                .build();
    }

    private static AuthenticationClient createIdentityClient(final IdentityConfiguration identityConfig) {
        return AuthenticationClientImpl.builder().config(identityConfig).build();
    }
}
