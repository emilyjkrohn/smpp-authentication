package protocol.authentication;

import protocol.AuthenticationRequest;
import protocol.AuthenticationResponse;
import protocol.UnsuccessfulResponse;

import io.vavr.control.Either;

public interface AuthenticationServer {

    /**
     * Checks user credentials against existing records and returns either successful or unsuccessful response
     * @param authenticationRequest
     * @return either AuthenticationResponse or UnsuccessfulResponse
     */
    Either<UnsuccessfulResponse, AuthenticationResponse> authenticate(final AuthenticationRequest authenticationRequest);
}
