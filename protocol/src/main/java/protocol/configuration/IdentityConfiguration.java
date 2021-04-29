package protocol.configuration;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class IdentityConfiguration {

    public abstract IdentityDynamoDBConfiguration dynamoDBConfiguration();

}
