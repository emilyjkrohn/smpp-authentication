package protocol;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class AuthenticationRequest {

    public abstract String systemId();
    public abstract String password();
    public abstract String ip();

}
