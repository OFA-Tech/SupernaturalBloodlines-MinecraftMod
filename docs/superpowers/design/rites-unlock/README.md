# Rites unlock: hidden DevShots (2026-11-21)

- `grimoire-book_category-witches-s2.png`, `grimoire-book_category-lines-s2.png`: the book's list. Entries the player has learned (here by being a hybrid, or open to everyone) show their name; the others are "???" with a wax icon.
- `grimoire-book_sealed-*.png`: a sealed entry (a hint and nothing of the rite). `grimoire-book_purification-s2.png`: an open rite's own entry.
- `<theme>-home-s2/s4.png`: the hub in all three themes, with no Rites page in the nav.
- `<theme>-home-focus-s2.png`: the E seal for Enchanted, with the book not in the inventory: "You need The Art of Witchcraft".
- Not shown: the list after learning a rite. The dev `learn` flag teaches on the server and the book opened in the same tick, before the client had the advancement; the GameTests cover the learning.

## Panel round (PR #57)

- `r2-book-*category-*-s2.png`: the sealed list with its labels, which say how each rite is learned (for example "??? - Esther's Grimoire"); `r2-book-*sealed-rite_of_the_cure*`: a sealed entry with its full hint.
- `r2-seal-<theme>-home-focus-s2.png`: the blocked E seal, dimmed, with its tooltip, and the new "Open The Art of Witchcraft" line under "What you can do now".
- `r2-guide-<theme>-guide_rites-s2.png`: the rewritten Rites chapter.
