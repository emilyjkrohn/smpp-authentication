package protocol.authentication;

import protocol.AuthenticationResponse;
import protocol.UnsuccessfulResponse;

import io.vavr.control.Either;

public interface AuthenticationClient {

    /**
     * Checks user credentials against existing records and returns either successful or unsuccessful response
     * @param systemId
     * @param passwordHash
     * @param remoteIp
     * @return either AuthenticationResponse or UnsuccessfulResponse
     */
    Either<UnsuccessfulResponse, AuthenticationResponse> authenticate(final String systemId,
            final String passwordHash,
            final String remoteIp);
}
