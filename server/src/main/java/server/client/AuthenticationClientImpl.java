package server.client;

import protocol.ImmutableAuthenticationRequest;
import protocol.authentication.AuthenticationClient;
import protocol.authentication.AuthenticationServer;
import protocol.AuthenticationResponse;
import protocol.UnsuccessfulResponse;
import protocol.configuration.IdentityConfiguration;

import authentication.AuthenticationServerImpl;
import io.vavr.control.Either;
import lombok.Builder;
import lombok.NonNull;

public class AuthenticationClientImpl implements AuthenticationClient {

    private final AuthenticationServer identityServer;

    @Builder
    public AuthenticationClientImpl(final @NonNull IdentityConfiguration config) {
        this.identityServer = AuthenticationServerImpl.builder().config(config).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Either<UnsuccessfulResponse, AuthenticationResponse> authenticate(final String systemId,
            final String passwordHash,
            final String remoteIp) {
        final var authenticationRequest =
                ImmutableAuthenticationRequest.builder().systemId(systemId).password(passwordHash).ip(remoteIp).build();

        return this.identityServer.authenticate(authenticationRequest);
    }
}
