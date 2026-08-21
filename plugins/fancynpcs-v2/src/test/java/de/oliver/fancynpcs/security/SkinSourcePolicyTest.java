package de.oliver.fancynpcs.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkinSourcePolicyTest {

    @Test
    void permitsOnlyApprovedStaticProfileNames() {
        assertTrue(SkinSourcePolicy.isApproved("mega-girl.png"));
        assertTrue(SkinSourcePolicy.isApproved("atlas.png"));
        assertTrue(SkinSourcePolicy.isApproved("default.png"));

        assertFalse(SkinSourcePolicy.isApproved("https://example.com/skin.png"));
        assertFalse(SkinSourcePolicy.isApproved("Notch"));
        assertFalse(SkinSourcePolicy.isApproved("../outside.png"));
        assertFalse(SkinSourcePolicy.isApproved("nested/profile.png"));
    }

    @Test
    void failedOrUnapprovedAcquisitionRetainsSafeDefault() {
        assertEquals("default.png", SkinSourcePolicy.safeIdentifier("https://example.com/skin.png"));
        assertEquals("default.png", SkinSourcePolicy.safeIdentifier(null));
        assertEquals("mega-girl.png", SkinSourcePolicy.safeIdentifier(" MEGA-GIRL.PNG "));
        assertEquals("atlas.png", SkinSourcePolicy.safeIdentifier(" ATLAS.PNG "));
    }
}
