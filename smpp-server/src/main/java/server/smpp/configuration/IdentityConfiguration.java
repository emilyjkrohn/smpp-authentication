package server.smpp.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
@Accessors(fluent = true)
public class IdentityConfiguration {

    @Valid
    @NotNull
    @JsonProperty("dynamoDbConfiguration")
    private @NonNull IdentityDynamoDBConfiguration dynamoDbConfiguration;

}
