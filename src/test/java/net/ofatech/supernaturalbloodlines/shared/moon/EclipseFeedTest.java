package net.ofatech.supernaturalbloodlines.shared.moon;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class EclipseFeedTest {
    /** With the species part off nothing installs the feed: a moon rite of another part then needs the real full moon. */
    @Test
    void withNothingInstalledThereIsNoEclipse() {
        assertFalse(EclipseFeed.NONE.test(null));
        assertFalse(EclipseFeed.active(null));
    }
}