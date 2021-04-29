package protocol;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class UnsuccessfulResponse {

    public abstract SmppError error();
}
