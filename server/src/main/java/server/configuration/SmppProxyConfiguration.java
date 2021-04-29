package server.configuration;

import server.smpp.configuration.IdentityConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

/**
 * The {@link SmppProxyConfiguration} is the "POJO" for the yaml file in conf/default-service.yml
 */
public class SmppProxyConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("identityModuleConfiguration")
    private IdentityConfiguration identityModuleConfiguration;


    /**
     * Returns the {@link IdentityConfiguration}
     *
     * @return the {@link IdentityConfiguration}
     */
    public IdentityConfiguration getIdentityModuleConfiguration() {
        return this.identityModuleConfiguration;
    }

}
