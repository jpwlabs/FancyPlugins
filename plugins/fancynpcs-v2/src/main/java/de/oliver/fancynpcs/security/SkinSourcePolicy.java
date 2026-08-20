package de.oliver.fancynpcs.security;

import java.util.Locale;
import java.util.Set;

/** Closed allowlist for JPW-owned, pre-cached lobby skin profiles. */
public final class SkinSourcePolicy {

    private static final Set<String> APPROVED = Set.of("default.png", "mega-guide.png");

    private SkinSourcePolicy() {
    }

    public static boolean isApproved(String identifier) {
        if (identifier == null) {
            return false;
        }
        return APPROVED.contains(identifier.trim().toLowerCase(Locale.ROOT));
    }
}
