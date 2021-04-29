package authentication.utils;

import protocol.configuration.IdentityConfiguration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;

public final class DynamoDBUtils {

    private DynamoDBUtils() {
    }

    /**
     * Build ClientConfiguration based on configuration settings.
     */
    public static ClientConfiguration getClientConfiguration(final IdentityConfiguration configuration) {
        return new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.getDynamoDBDefaultRetryPolicyWithCustomMaxRetries(
                configuration.dynamoDBConfiguration().retries()));
    }

    /**
     * Build EndpointConfiguration based on endpoint and region settings.
     */
    public static EndpointConfiguration getEndpointConfiguration(final IdentityConfiguration configuration) {
        return new EndpointConfiguration(configuration.dynamoDBConfiguration().endpoint(),
                configuration.dynamoDBConfiguration().region());
    }

    /**
     * Return value of local parameter from configuration
     */
    public static boolean isLocal(final IdentityConfiguration configuration) {
        return configuration.dynamoDBConfiguration().local();
    }

    /**
     * Return value of region parameter from configuration
     */
    public static String getRegion(final IdentityConfiguration configuration) {
        return configuration.dynamoDBConfiguration().region();
    }
}
