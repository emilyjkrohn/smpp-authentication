package authentication;

import protocol.AuthenticationRequest;
import protocol.ImmutableAuthenticationRequest;
import protocol.ImmutableAuthenticationResponse;
import protocol.ImmutableUnsuccessfulResponse;
import protocol.configuration.IdentityConfiguration;
import protocol.configuration.ImmutableIdentityConfiguration;
import protocol.configuration.ImmutableIdentityDynamoDBConfiguration;

import java.util.Optional;

import com.amazonaws.services.dynamodbv2.document.Item;
import io.vavr.control.Either;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static protocol.SmppError.SMPP_3001;
import static protocol.SmppError.SMPP_3002;
import static protocol.SmppError.SMPP_3003;
import static protocol.SmppError.SMPP_3004;
import static protocol.SmppError.SMPP_3005;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AuthenticationServerImplTest {
    private final IdentityConfiguration identityConfiguration = ImmutableIdentityConfiguration.builder()
            .dynamoDBConfiguration(ImmutableIdentityDynamoDBConfiguration.builder()
                    .endpoint("endpoint")
                    .local(false)
                    .region("region")
                    .retries(1)
                    .tableName("tableName")
                    .build())
            .build();

    private final AuthenticationServerImpl authenticationServer =
            Mockito.spy(AuthenticationServerImpl.builder().config(this.identityConfiguration).build());

    private final AuthenticationRequest authenticationRequest = ImmutableAuthenticationRequest.builder()
            .ip("1.2.3.4")
            .password("password")
            .systemId("system_id")
            .build();
    private final AuthenticationRequest incorrectIpRequest =
            ImmutableAuthenticationRequest.builder().ip("incorrect").password("password").systemId("system_id").build();
    private final AuthenticationRequest incorrectPasswordRequest = ImmutableAuthenticationRequest.builder()
            .ip("1.2.3.4")
            .password("incorrect")
            .systemId("system_id")
            .build();

    private final String passwordHash = BCrypt.hashpw(this.authenticationRequest.password(), BCrypt.gensalt(10));

    final Item item = new Item().withPrimaryKey("system_id", "system_id")
            .withString("password_hash", this.passwordHash)
            .withString("customer_id", "customer_id")
            .withString("ip_allow_list", "1.2.3.4/32,1.2.3.5");

    final Identity identity = this.authenticationServer.fromDynamoDb(this.item).get();

    final Item nullIpItem = new Item().withPrimaryKey("system_id", "system_id")
            .withString("password_hash", this.passwordHash)
            .withString("customer_id", "customer_id");

    @Test
    public void testSuccessfulAuthentication() {
        Mockito.doReturn(Either.right(this.identity))
                .when(this.authenticationServer)
                .getCredentials(this.authenticationRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.authenticationRequest);

        final var successfulResponse = ImmutableAuthenticationResponse.builder()
                .systemId(this.authenticationRequest.systemId())
                .sessionId(authentication.get().sessionId())
                .customerId(this.item.getString("customer_id"))
                .build();
        assertThat(authentication, is(equalTo(Either.right(successfulResponse))));
    }

    @Test
    public void testIncorrectSystemId() {
        Mockito.doReturn(Either.left(ImmutableUnsuccessfulResponse.builder().error(SMPP_3001).build())).when(this.authenticationServer).getCredentials(this.authenticationRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.authenticationRequest);

        final var incorrectSystemIdResponse = ImmutableUnsuccessfulResponse.builder().error(SMPP_3001).build();

        assertThat(authentication, is(equalTo(Either.left(incorrectSystemIdResponse))));
        verify(this.authenticationServer, times(1)).incrementErrorCounter(SMPP_3001);
    }

    @Test
    public void testIncorrectPassword() {
        Mockito.doReturn(Either.right(this.identity))
                .when(this.authenticationServer)
                .getCredentials(this.incorrectPasswordRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.incorrectPasswordRequest);

        final var incorrectPasswordResponse = ImmutableUnsuccessfulResponse.builder().error(SMPP_3003).build();

        assertThat(authentication, is(equalTo(Either.left(incorrectPasswordResponse))));
        verify(this.authenticationServer, times(1)).incrementErrorCounter(SMPP_3003);
    }

    @Test
    public void testIncorrectIp() {
        Mockito.doReturn(Either.right(this.identity))
                .when(this.authenticationServer)
                .getCredentials(this.incorrectIpRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.incorrectIpRequest);

        final var incorrectIpResponse = ImmutableUnsuccessfulResponse.builder().error(SMPP_3002).build();

        assertThat(authentication, is(equalTo(Either.left(incorrectIpResponse))));
        verify(this.authenticationServer, times(1)).incrementErrorCounter(SMPP_3002);
    }

    @Test
    public void testDynamoDbFailure() {
        Mockito.doReturn(Either.left(ImmutableUnsuccessfulResponse.builder().error(SMPP_3004).build()))
                .when(this.authenticationServer)
                .getCredentials(this.authenticationRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.authenticationRequest);

        final var dynamoDbFailureResponse = ImmutableUnsuccessfulResponse.builder().error(SMPP_3004).build();

        assertThat(authentication, is(equalTo(Either.left(dynamoDbFailureResponse))));
        verify(this.authenticationServer, times(1)).incrementErrorCounter(SMPP_3004);
    }

    @Test
    public void testMissingCredentials() {
        Mockito.doReturn(Either.left(ImmutableUnsuccessfulResponse.builder().error(SMPP_3005).build()))
                .when(this.authenticationServer)
                .getCredentials(this.authenticationRequest.systemId());

        final var authentication = this.authenticationServer.authenticate(this.authenticationRequest);

        final var missingCredentialsResponse = ImmutableUnsuccessfulResponse.builder().error(SMPP_3005).build();

        assertThat(authentication, is(equalTo(Either.left(missingCredentialsResponse))));
        verify(this.authenticationServer, times(1)).incrementErrorCounter(SMPP_3005);
    }

    @Test
    public void testGetNoIpInDynamo() {
        final var allowList = this.authenticationServer.getIpAllowList(this.nullIpItem);

        assertThat(allowList, is(equalTo(Optional.empty())));
    }

    @Test
    public void testGetInvalidIp() {
        final var invalidIpItem = new Item().withPrimaryKey("system_id", "system_id")
                .withString("password_hash", this.passwordHash)
                .withString("customer_id", "customer_id")
                .withString("ip_allow_list", "invalid_ip");
        final var allowList = this.authenticationServer.getIpAllowList(invalidIpItem);

        assertThat(allowList.get().size(), is(0));
    }

    @Test
    public void testGetAllowedIpList() {
        final var allowList = this.authenticationServer.getIpAllowList(this.item);

        assertThat(allowList.get().size(), is(2));
    }

    @Test
    public void testNoIpInDynamo() {
        final var falseIp =
                this.authenticationServer.checkIpAddress(this.authenticationServer.fromDynamoDb(this.nullIpItem).get(),
                        this.authenticationRequest);

        assertThat(falseIp, is(true));
    }

    @Test
    public void testNoItemInDynamo() {
        final var nullSystemIdItem = new Item().withPrimaryKey("system_id", null);

        final var nullSystemIdIdentity = this.authenticationServer.fromDynamoDb(nullSystemIdItem);

        assertThat(nullSystemIdIdentity, is(Optional.empty()));
    }
}

