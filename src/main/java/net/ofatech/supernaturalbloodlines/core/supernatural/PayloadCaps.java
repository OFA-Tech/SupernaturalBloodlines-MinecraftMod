package net.ofatech.supernaturalbloodlines.core.supernatural;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.Nullable;

/**
 * Checks that data files fit the sync payload that carries them (a file name and body have a length cap in the codec, and a play
 * packet has a size limit), so a datapack with a huge file is skipped at load with one log line instead of disconnecting every player
 * when the table is sent. One instance per payload: it also keeps the total under {@link #TOTAL}. No game classes.
 */
public final class PayloadCaps {
    /** Under the 1 MiB a clientbound custom payload may have, leaving room for the other fields. */
    public static final int TOTAL = 900_000;

    private static final Gson GSON = new Gson();

    private int used;

    /** @return why the file does not fit (the name is in the text), or null and it is counted */
    @Nullable
    public String check(String name, JsonElement body, int maxName, int maxBody) {
        int nameBytes = name.getBytes(StandardCharsets.UTF_8).length;
        if (nameBytes > maxName) {
            return "its name is " + nameBytes + " bytes, the limit is " + maxName;
        }
        int bodyBytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8).length;
        if (bodyBytes > maxBody) {
            return "it is " + bodyBytes + " bytes, the limit is " + maxBody;
        }
        if (used + nameBytes + bodyBytes > TOTAL) {
            return "the files before it already fill the " + TOTAL + " bytes the table may take";
        }
        used += nameBytes + bodyBytes;
        return null;
    }
}