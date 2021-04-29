package authentication;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.immutables.value.Value.Immutable;

@Immutable
public interface Identity {
    String systemId();
    String passwordHash();
    String customerId();
    // optional to have an IP allow-list
    Optional<Set<SubnetInfo>> ipAllowList();
}
