# Klaus's hybrid wolf form: references

Research for the GeckoLib model of Klaus Mikaelson's wolf form. The design is original; nothing was copied.
Fandom answers 403 to scripted fetches, so those entries come from search-result text.

**The show never puts Klaus's full wolf on screen.** In "The Sun Also Rises" (TVD 2x21) the shift starts and the episode cuts away; he
wakes up human two days later. There is no still to match, so the body follows what the wikis say about every werewolf's wolf form and the
face follows Klaus's on-screen hybrid face. The coat colour is a decision, explained at the bottom.

## Brief (first-hand)
- Task brief for this model: "a large dark wolf with golden-yellow hybrid eyes and vampire fangs".

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
| https://mindura.blog/did-klaus-mikaelson-turn-into-a-wolf (2026) | Describes "grayish-brown fur", "deep grays and earthy browns" and "glowing golden-yellow eyes", but says the wolf was shown on screen, which the wiki and the fans contradict. Not trusted; noted only because it lands on the same dark grey-brown. |
| `art/bosses/tools/paint_wolves.py` (`KLAUS`), the flat texture `textures/entity/boss/klaus_hybrid.png` | Colour continuity with the mod's existing Klaus wolf: eye `(255,222,40)` with halo `(214,160,20)`, teeth `(244,238,216)` shaded to `(130,108,86)`, mouth `(130,36,38)`, tongue `(170,62,66)`, claws `(54,44,38)`, veins `(8,6,6)`, dark saddle `(76,52,34)`. |

## Colour decision
No source fixes the coat. The brief says dark and the fans say black; the mod's own flat texture is tan-brown with a dark brown saddle.
The coat takes the **hue of that dark saddle** `(76,52,34)` and brings it down to near-black (`#0b0908` to `#584838`), so it reads as a
black wolf with a brown cast in light: dark as asked, still related to the old texture, and clearly apart from Peter's blue-black.
Every other colour is the old texture's value unchanged. Only the eye's hot centre `(255,244,150)` is new: a highlight of the same yellow.
