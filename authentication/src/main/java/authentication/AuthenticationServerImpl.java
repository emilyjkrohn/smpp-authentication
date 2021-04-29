package authentication;

import protocol.AuthenticationRequest;
import protocol.AuthenticationResponse;
import protocol.ImmutableAuthenticationResponse;
import protocol.ImmutableUnsuccessfulResponse;
import protocol.SmppError;
import protocol.UnsuccessfulResponse;
import protocol.authentication.AuthenticationServer;
import protocol.configuration.IdentityConfiguration;
import authentication.utils.DynamoDBUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.vavr.control.Either;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.mindrot.jbcrypt.BCrypt;

import static protocol.SmppError.SMPP_3001;
import static protocol.SmppError.SMPP_3002;
import static protocol.SmppError.SMPP_3003;
import static protocol.SmppError.SMPP_3004;
import static protocol.SmppError.SMPP_3005;

@Slf4j
public class AuthenticationServerImpl implements AuthenticationServer {

    private final DynamoDB dynamoDBClient;
    private final String tableName;
    private static final String STATUS = "status";
    private static final String UNSUCCESSFUL = "unsuccessful";
    private static final String ERROR = "error";
    private static final String SYSTEM_ID_ATTRIBUTE = "system_id";
    private static final String PASSWORD_HASH_ATTRIBUTE = "password_hash";
    private static final String CUSTOMER_ID_ATTRIBUTE = "customer_id";
    private static final String IP_ALLOW_LIST_ATTRIBUTE = "ip_allow_list";
    private static final String AUTHENTICATION_CALLS = "authentication.calls";
    private final Map<SmppError, Counter> errorCounterMap = new ConcurrentHashMap<>();

    private final Counter successfulAuthenticationCounter =
            Counter.builder(AUTHENTICATION_CALLS).tag(STATUS, "successful").register(Metrics.globalRegistry);

    @Builder
    public AuthenticationServerImpl(final @NonNull IdentityConfiguration config) {
        this.dynamoDBClient = createDynamoDBClient(config);
        this.tableName = config.dynamoDBConfiguration().tableName();
    }

    /**
     * Create the DynamoDB client based on configuration
     */
    private static DynamoDB createDynamoDBClient(final IdentityConfiguration identityConfiguration) {
        final var clientBuilder = AmazonDynamoDBClientBuilder.standard()
                .withClientConfiguration(DynamoDBUtils.getClientConfiguration(identityConfiguration));

        if (DynamoDBUtils.isLocal(identityConfiguration)) {
            clientBuilder.setEndpointConfiguration(DynamoDBUtils.getEndpointConfiguration(identityConfiguration));
        } else {
            clientBuilder.setRegion(DynamoDBUtils.getRegion(identityConfiguration));
        }
        return new DynamoDB(clientBuilder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Either<UnsuccessfulResponse, AuthenticationResponse> authenticate(final AuthenticationRequest authenticationRequest) {
        final var identity = this.getCredentials(authenticationRequest.systemId());

        // DynamoDB error/incorrect or missing system ID/missing credentials
        if (identity.isLeft()) {
            this.incrementErrorCounter(identity.getLeft().error());
            return Either.left(identity.getLeft());
        }

        // incorrect IP
        if (!checkIpAddress(identity.get(), authenticationRequest)) {
            this.incrementErrorCounter(SMPP_3002);
            final var response = ImmutableUnsuccessfulResponse.builder().error(SMPP_3002).build();
            log.info("IP is not allow-listed for the session - Response: {}", response);
            return Either.left(response);
        }

        // incorrect password
        if (!checkPassword(identity.get(), authenticationRequest)) {
            this.incrementErrorCounter(SMPP_3003);
            final var response = ImmutableUnsuccessfulResponse.builder().error(SMPP_3003).build();
            log.info("Password is incorrect - Response: {}", response);
            return Either.left(response);
        }

        // successful authentication
        this.successfulAuthenticationCounter.increment();
        final var response = ImmutableAuthenticationResponse.builder()
                .systemId(identity.get().systemId())
                .sessionId(UUID.randomUUID().toString())
                .customerId(identity.get().customerId())
                .build();
        log.debug("Account {} successfully authenticated - Response: {}", identity.get().customerId(), response);
        return Either.right(response);

    }

    /**
     * Get the authentication information from DynamoDB
     *
     * @param systemId - provided systemID from client
     *
     * @return Item from DynamoDB or exception
     */
    Either<UnsuccessfulResponse, Identity> getCredentials(final String systemId) {
        try {
            final var spec = new GetItemSpec().withPrimaryKey(SYSTEM_ID_ATTRIBUTE, systemId);
            final var item = this.dynamoDBClient.getTable(this.tableName).getItem(spec);
            if (item != null) {
                // identity item
                final var fromDynamo = fromDynamoDb(item);
                if (fromDynamo.isPresent()) {
                    return Either.right(fromDynamo.get());
                } else {
                    final var response = ImmutableUnsuccessfulResponse.builder().error(SMPP_3005).build();
                    log.info("Missing necessary credentials - Response {}", response);
                    return Either.left(response);
                }
            }
            // system_id is not present
            final var response = ImmutableUnsuccessfulResponse.builder().error(SMPP_3001).build();
            log.info("System ID is incorrect or not present - Response: {}", response);
            return Either.left(response);
        } catch (final RuntimeException e) {
            // unable to connect to dynamo
            final var response = ImmutableUnsuccessfulResponse.builder().error(SMPP_3004).build();
            log.warn("Unable to connect to DynamoDB {} - Response: {}", e, response);
            return Either.left(response);
        }
    }

    /**
     * Check whether the given password matches the correct password
     *
     * @param identity              - Identity item
     * @param authenticationRequest - Authentication request from client
     *
     * @return whether the given password matches the stored BCrypt hash
     */
    private static boolean checkPassword(final Identity identity, final AuthenticationRequest authenticationRequest) {
        final var passwordHash = identity.passwordHash();

        return BCrypt.checkpw(authenticationRequest.password(), passwordHash);
    }

    /**
     * Check if given IP address is valid against known IP address
     *
     * @param identity              Identity item
     * @param authenticationRequest Authentication request from client
     *
     * @return whether IP is valid or not
     */
    static boolean checkIpAddress(final Identity identity, final AuthenticationRequest authenticationRequest) {
        final var ipAllowList = identity.ipAllowList();

        if (ipAllowList.isEmpty()) {
            return true;
        }

        final var requestIp = authenticationRequest.ip();
        try {
            return ipAllowList.get().stream().anyMatch(cidr -> cidr.isInRange(requestIp));
        } catch (final IllegalArgumentException e) {
            log.error("invalid IP Address supplied in the AuthenticationRequest", e);
            return false;
        }
    }

    /**
     * Get the Identity item from DynamoDB item
     *
     * @param item - the DynamoDB Item
     *
     * @return the Identity item
     */

    static Optional<Identity> fromDynamoDb(final Item item) {
        final var systemId = item.getString(SYSTEM_ID_ATTRIBUTE);
        final var customerId = item.getString(CUSTOMER_ID_ATTRIBUTE);
        final var passwordHash = item.getString(PASSWORD_HASH_ATTRIBUTE);
        if ((null == customerId) || (null == passwordHash)) {
            return Optional.empty();
        }

        final var builder =
                ImmutableIdentity.builder().systemId(systemId).customerId(customerId).passwordHash(passwordHash);

        getIpAllowList(item).ifPresent(builder::ipAllowList);

        return Optional.of(builder.build());
    }

    /**
     * Get the allowed ip list from DynamoDB item
     *
     * @param item DynamoDB item
     *
     * @return allowed ip list
     */
    static Optional<Set<SubnetInfo>> getIpAllowList(final Item item) {
        final var cidrString = item.getString(IP_ALLOW_LIST_ATTRIBUTE);

        if (null == cidrString) {
            return Optional.empty();
        }

        final var cidrSet = Arrays.asList(cidrString.split(","));

        final Set<SubnetInfo> ipAllowList = new HashSet<>();

        cidrSet.forEach(rawCidr -> {
            try {
                final var trimmedCidr = rawCidr.contains("/") ? rawCidr.trim() : (rawCidr.trim() + "/32");
                final var subnet = new SubnetUtils(trimmedCidr);
                subnet.setInclusiveHostCount(true);
                ipAllowList.add(subnet.getInfo());
            } catch (final IllegalArgumentException e) {
                log.warn("invalid CIDR. skipping", e);
            }
        });

        return Optional.of(ipAllowList);
    }

    /**
     * Increment the error counter in the map for various errors
     *
     * @param error SmppError
     */
    void incrementErrorCounter(final SmppError error) {
        if (this.errorCounterMap.containsKey(error)) {
            this.errorCounterMap.get(error).increment();
        } else {
            final var counter = createErrorCounter(error);
            this.errorCounterMap.put(error, counter);
            counter.increment();
        }
    }

    /**
     * Create a counter for each error
     *
     * @param error SmppError
     *
     * @return
     */
    private static Counter createErrorCounter(final SmppError error) {
        return Counter.builder(AUTHENTICATION_CALLS)
                .tag(STATUS, UNSUCCESSFUL)
                .tag(ERROR, error.code)
                .register(Metrics.globalRegistry);
    }
}
