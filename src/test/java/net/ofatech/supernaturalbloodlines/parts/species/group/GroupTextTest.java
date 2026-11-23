package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import org.junit.jupiter.api.Test;

/** R248: the wording of the shared actions names the type. */
public class GroupTextTest {
    public static JsonObject lang() throws IOException {
        try (InputStream in = GroupTextTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everySharedBaseHasAKeyForEveryType() throws IOException {
        JsonObject lang = lang();
        for (String base : GroupText.sharedBases()) {
            for (GroupType type : GroupType.values()) {
                assertTrue(lang.has(GroupText.key(type, base)), "missing " + GroupText.key(type, base));
            }
        }
    }

    @Test
    void aPackKeepsItsOldKeys() {
        for (String base : new String[] {"invited", "invite", "joined", "kicked", "ally_made", "not_leader"}) {
            assertTrue(GroupText.key(GroupType.PACK, base).startsWith("supernaturalbloodlines.pack."), base);
        }
        assertTrue(GroupText.key(GroupType.PACK, "in_group").endsWith("has_pack"));
        assertTrue(GroupText.key(GroupType.PACK, "full").startsWith("supernaturalbloodlines.group."));
    }

    @Test
    void clanCovenAndBrotherhoodAreNotACopyOfThePackText() throws IOException {
        JsonObject lang = lang();
        for (String base : GroupText.sharedBases()) {
            for (GroupType type : new GroupType[] {GroupType.CLAN, GroupType.COVEN, GroupType.BROTHERHOOD}) {
                String text = lang.get(GroupText.key(type, base)).getAsString().toLowerCase();
                assertFalse(text.contains("pack") || text.contains("alpha"), GroupText.key(type, base) + ": " + text);
                assertFalse(text.equals(lang.get(GroupText.key(GroupType.PACK, base)).getAsString().toLowerCase()) && !base.equals("accept") && !base.equals("self")
                        && !base.equals("ally_invited") && !base.equals("expired") && !base.equals("leader_offline") && !base.equals("type_changed")
                        && !base.equals("ally_not_leader") && !base.equals("target_in_group") && !base.equals("in_group") && !base.equals("leader_gone"),
                        GroupText.key(type, base) + " is the pack text");
            }
        }
    }
}
