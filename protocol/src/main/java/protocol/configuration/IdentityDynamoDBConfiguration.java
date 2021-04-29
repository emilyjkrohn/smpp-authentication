package protocol.configuration;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class IdentityDynamoDBConfiguration {

    public abstract int retries();

    public abstract boolean local();

    public abstract String endpoint();

    public abstract String region();

    public abstract String tableName();
}
