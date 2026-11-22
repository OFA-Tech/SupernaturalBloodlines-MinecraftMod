package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import org.junit.jupiter.api.Test;

class PageWidgetsTest {
    @Test
    void aButtonHasTheKitPartOfItsState() {
        assertEquals(Part.BUTTON_NORMAL, PageWidgets.part(true, false, false));
        assertEquals(Part.BUTTON_HOVER, PageWidgets.part(true, false, true));
        assertEquals(Part.BUTTON_PRIMARY_NORMAL, PageWidgets.part(true, true, false));
        assertEquals(Part.BUTTON_PRIMARY_HOVER, PageWidgets.part(true, true, true));
    }

    @Test
    void aButtonThatIsOffIsNeverLitWhateverThePointerDoes() {
        assertEquals(Part.BUTTON_DISABLED, PageWidgets.part(false, false, true));
        assertEquals(Part.BUTTON_PRIMARY_DISABLED, PageWidgets.part(false, true, true));
        assertNotEquals(PageWidgets.part(false, true, false), PageWidgets.part(true, true, false), "off looks different from on");
    }
}
