package protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * This ENUM is used for working with, and translating internal SMPP Errors.
 * As a user you will be able to have a consistent store of code-to-description mappings.
 */
public enum SmppError {

    // authentication
    SMPP_3001("SMPP-3001", SmppError.AUTHENTICATION, "system_id does not exist"),
    SMPP_3002("SMPP-3002", SmppError.AUTHENTICATION, "ip address does not match with ip-allow-list"),
    SMPP_3003("SMPP-3003", SmppError.AUTHENTICATION, "invalid password"),
    SMPP_3004("SMPP-3004", SmppError.AUTHENTICATION, "unable to connect to identity datastore"),
    SMPP_3005("SMPP-3005", SmppError.AUTHENTICATION, "necessary credentials are missing");

    private static final Map<String, SmppError> BY_CODE = new HashMap<>();
    private static final String AUTHENTICATION = "authentication";

    static {
        for (final SmppError smppError : values()) {
            BY_CODE.put(smppError.code, smppError);
        }
    }

    // External Error Code (e.g. SMPP_3000)
    public final String code;
    // Which module the error originates from (e.g. authentication)
    public final String module;
    // External Error Description (e.g. invalid password)
    public final String description;

    /**
     * ENUM constructor.
     * @param code The code to represent.
     * @param module Which module that is the owner of the error.
     * @param description The description to represent.
     */
    SmppError(final String code, final String module, final String description) {
        this.code = code;
        this.module = module;
        this.description = description;
    }

    /**
     * Return the SmppError associated with the Error Code.
     * @param code the error code to do the lookup from.
     * @return The full SmppError.
     */
    public static SmppError smppErrorFromCode(final String code) {
        return BY_CODE.get(code);
    }
}