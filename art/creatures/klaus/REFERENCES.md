# Klaus's hybrid wolf form: references

Research for the GeckoLib model of Klaus Mikaelson's wolf form. The design is original; nothing was copied.
Fandom answers 403 to scripted fetches, so those entries come from search-result text.

The web research below found no still of the full wolf: in "The Sun Also Rises" (TVD 2x21) the shift starts and the episode cuts away,
and the wikis and fans say the wolf is never shown. **The owner then supplied a still from The Originals, and it overrides that
research for the coat and the silhouette.** The wikis still give the build rules and the hybrid face.

## Owner's reference (first-hand, 2026-11-19): a show still of Klaus's wolf, The Originals
Described in words by the owner's side; the image itself is not in the repo and the art agent did not see it.
- His verdict on the colour: "pretty much like his hair colour, but brownish". Klaus's hair is dark-blond to light-brown.
- A realistic grey wolf, dimly lit at night. The coat is a cool ash-grey with a brownish tint.
- Lighter fur on the face, the cheeks and the thick neck ruff; a darker top of the muzzle and a darker back.
- Bright golden-yellow eyes and upright pointed ears.
- A natural, thick, fluffy wolf shape: no spikes, nothing monstrous.

Taken from it: the ash-brown coat and where it is light and dark, the pelt of long fur along the spine in place of spiked hackles, the
thicker neck ruff and cheek tufts, the bushier tail.

## Earlier brief (superseded for the colour)
- First task brief: "a large dark wolf with golden-yellow hybrid eyes and vampire fangs". The first version was near-black because of it.

## Sources
| Source | Taken from it |
|---|---|
| https://vampirediaries.fandom.com/wiki/Werewolf (Appearance, Werewolf Transformation) | Wolf forms are "quite similar to natural wolves", but with "glowing yellow or orange eyes, wicked fangs, a larger size, and a much more feral and predatory appearance", "size ... comparable to fully grown humans": a natural wolf build, about 1.5 blocks at the shoulder, not a monster like Peter's Alpha. "Their irises turn golden-yellow, their sclera turns black": golden iris on a black sclera. Coat colour differs per wolf (Tyler dark brown, Mason white and grey, Jules light brown, Hayley tan and brown), so there is no species colour. |
| https://vampirediaries.fandom.com/wiki/Hybrid (The Original Hybrid, Lycanthrope Enhancement) | Klaus "can display both sets of vampire and werewolf fangs": two upper fangs a side, a long vampire fang with a shorter wolf fang behind it. Dark veins under the eyes of the vampire face: the vein patch under each eye. His strength peaks "in his werewolf form". |
| https://vampirediaries.fandom.com/wiki/Shapeshifting (Hybrids) | Image caption "Klaus shows his werewolf like fangs and yellow irises": the eye colour is yellow, not orange or red. Hybrids turn at will and keep the form "unusually long". |
| https://vampirediaries.fandom.com/wiki/Transformation_Control | Klaus "remained a wolf for 2 days" after breaking the curse and killed throughout: a predator, hence the permanent snarl (jaw rests 6 degrees open) and the bristling hackles. |
| https://vampirediaries.fandom.com/wiki/The_Sun_Also_Rises and https://en.wikipedia.org/wiki/The_Sun_Also_Rises_(The_Vampire_Diaries) | The episode: the transformation begins at the ritual; the wolf itself is not shown. |
| https://www.reddit.com/r/TheOriginals/comments/11jys95/ (2023) | Fans confirm the wolf is never shown ("We really seen just about everyone else's wolf form but Klaus") and guess the coat: "I assumed he would have had black fur". |
| https://www.reddit.com/r/TheOriginals/comments/enbrt7/klaus_mikaelson_in_his_werewolf_form/ (2020) | Same question, same guess: "a huge pure black wolf with dark amber eyes", "bigger and larger than the average wolf". |
| https://vampirediaries.fandom.com/f/p/3300297438920796007 (wiki discussion "Klaus's Wolf Form") | "I wish we would've gotten a chance to see his actual wolf form"; "Julie said it was too expensive". Other guesses there: brown (from his dirty-blond hair) or auburn. |
| https://mindura.blog/did-klaus-mikaelson-turn-into-a-wolf (2026) | Describes "grayish-brown fur", "deep grays and earthy browns" and "glowing golden-yellow eyes", but says the wolf was shown on screen, which the wiki and the fans contradict. Not trusted as a source; its grey-brown does agree with the owner's still. |
| `art/bosses/tools/paint_wolves.py` (`KLAUS`), the flat texture `textures/entity/boss/klaus_hybrid.png` | Colour continuity with the mod's existing Klaus wolf: eye `(255,222,40)` with halo `(214,160,20)`, teeth `(244,238,216)` shaded to `(130,108,86)`, mouth `(130,36,38)`, tongue `(170,62,66)`, claws `(54,44,38)`, veins `(8,6,6)`, dark saddle `(76,52,34)`, cream fur `(214,184,138)`. |
| `art/bosses/tools/paint_skins.py` (`klaus()`) | The mod's own colours for Klaus's hair `(164,136,92)` and brow `(108,84,56)`: the starting point for "like his hair colour". |

## Colour
The coat is one ramp, `COAT` in `tools/klaus.py`. The owner's still reads as ash-grey with a brown tint, and he ties it to the hair,
so each anchor is **a colour the mod already uses for Klaus, mixed half and half with its own grey**:

| Anchor | From | Ashed | Where |
|---|---|---|---|
| saddle | old flat texture's dark saddle `(76,52,34)` | `#42362d` | back, hackles, top of the muzzle, brow, ear tips, tail tip |
| brow | hair brow `(108,84,56)` | `#615547` | crown, upper flank, tail |
| hair | hair `(164,136,92)` | `#978973` | flank and legs: the body colour |
| cream | old flat texture's cream fur `(214,184,138)` | `#c8b9a2` | face, cheeks, neck ruff, chest, belly, chin |

The steps between are in-betweens, with one shadow below the saddle (`#2e2620`) and one highlight above the cream (`#d9cdb8`).
Eyes, teeth, mouth, tongue, claws and veins are the old texture's values unchanged. Only the eye's hot centre `(255,244,150)` is new:
a highlight of the same yellow. The first version's near-black coat is gone.
