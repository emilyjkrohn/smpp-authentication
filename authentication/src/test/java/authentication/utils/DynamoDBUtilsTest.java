package authentication.utils;

import protocol.configuration.IdentityConfiguration;
import protocol.configuration.ImmutableIdentityConfiguration;
import protocol.configuration.ImmutableIdentityDynamoDBConfiguration;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class DynamoDBUtilsTest {
    private final IdentityConfiguration identityConfiguration = ImmutableIdentityConfiguration.builder()
            .dynamoDBConfiguration(ImmutableIdentityDynamoDBConfiguration.builder()
                    .endpoint("endpoint")
                    .local(false)
                    .region("region")
                    .retries(1)
                    .tableName("tableName")
                    .build())
            .build();

    @Test
    public void testGetClientConfiguration() {
        final var clientConfiguration = DynamoDBUtils.getClientConfiguration(this.identityConfiguration);

        assertThat(clientConfiguration.getRetryPolicy().getMaxErrorRetry(),
                is(equalTo(this.identityConfiguration.dynamoDBConfiguration().retries())));
    }

    @Test
    public void testGetEndpointConfiguration() {
        final var endpointConfiguration = DynamoDBUtils.getEndpointConfiguration(this.identityConfiguration);

        assertThat(endpointConfiguration.getServiceEndpoint(),
                is(equalTo(this.identityConfiguration.dynamoDBConfiguration().endpoint())));

        assertThat(endpointConfiguration.getSigningRegion(),
                is(equalTo(this.identityConfiguration.dynamoDBConfiguration().region())));
    }

    @Test
    public void testIsLocal() {
        final var isLocal = DynamoDBUtils.isLocal(this.identityConfiguration);

        assertThat(isLocal, is(equalTo(this.identityConfiguration.dynamoDBConfiguration().local())));
    }

    @Test
    public void testGetRegion() {
        final var region = DynamoDBUtils.getRegion(this.identityConfiguration);

        assertThat(region, is(equalTo(this.identityConfiguration.dynamoDBConfiguration().region())));
    }
}
