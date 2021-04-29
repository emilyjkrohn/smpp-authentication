package protocol;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class AuthenticationResponse {

    public abstract String systemId();
    public abstract String sessionId();
    public abstract String customerId();

}
