package server.smpp.configuration;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
@Accessors(fluent = true)
public class IdentityDynamoDBConfiguration {

    @JsonProperty("enabled")
    @Valid
    private boolean enabled;

    @JsonProperty("local")
    @Valid
    private boolean local;

    @JsonProperty("retries")
    @Valid
    private int retries = 3;

    @JsonProperty("region")
    @Valid
    private String region = "us-east-1";

    @JsonProperty("endpoint")
    @Valid
    private String endpoint = "http://localhost:4569";

    @JsonProperty("tableName")
    @Valid
    private String tableName = "sms.smpp-api-identity";
}
