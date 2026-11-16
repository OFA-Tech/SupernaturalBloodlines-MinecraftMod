package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import org.junit.jupiter.api.Test;

/** A theme with no views of its own still gets a working page for every tab, and a locked page is the placeholder. */
class HubPagesTest {
    @Test
    void everyPageHasASharedFallbackView() {
        for (HubPage p : HubPage.values()) {
            assertNotNull(HubPages.shared(p), p.name());
            assertNotSame(HubPages.locked(), HubPages.shared(p), p.name() + " must not be the placeholder");
        }
    }

    @Test
    void aThemeWithNoViewsGetsTheSharedOnes() {
        HubThemeViews none = new HubThemeViews() { };
        for (HubPage p : HubPage.values()) {
            assertSame(HubPages.shared(p), none.view(p), p.name());
        }
    }
}
