"""Merge the morph clips a Blockbench export holds into a boss's game animation file, and touch nothing else.

    python art/creatures/morph/tools/merge_clips.py <model> <exported.json> [--check]

<model> is klaus_hybrid, deucalion_demon_wolf, peter_hale_alpha or beast_of_gevaudan. <exported.json> is what Blockbench's "Export animations" wrote (it may hold
the whole model's clips; only the morph clips in it are used). The game file is src/main/resources/assets/supernaturalbloodlines/animations/entity/<model>.animation.json.

What it does (UI overhaul phase 7, plan T6):
  - replaces `shift_in`, `shift_out` and the re-timed howl clip (`howl` for Klaus, `roar` for the others) by name, keeping their place if they exist and
    appending the new ones after the last clip otherwise;
  - deletes `drop_to_fours` (Deucalion) and `rise_up` (Peter) once `shift_in` exists, because it replaces them;
  - keeps every other clip, key and key order exactly as it was, and writes the file in the style it had (indent 1 or 2, the trailing newline or not);
  - fails, writing nothing, when an animation name is not one of those above, when a bone is not a bone of the model's geo, or when a clip has the wrong length
    (shift_in and shift_out 1.0 s, the howl 2.2 s) or loops.
With --check nothing is written: it only says what it would do.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..', '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'supernaturalbloodlines')

# model -> (the howl clip, the clips this phase removes once shift_in exists, whether the boss has a shift_out)
MODELS = {
    'klaus_hybrid': ('howl', [], True),
    'deucalion_demon_wolf': ('roar', ['drop_to_fours'], True),
    'peter_hale_alpha': ('roar', ['rise_up'], True),
    'beast_of_gevaudan': ('roar', [], False),
}
LENGTHS = {'shift_in': 1.0, 'shift_out': 1.0}
HOWL_LENGTH = 2.2


def bones_of(geo):
    names = set()

    def walk(items):
        for b in items:
            names.add(b['name'])
    for g in geo.get('minecraft:geometry', []):
        walk(g.get('bones', []))
    return names


def style_of(text, data):
    """The indent and trailing newline the file was written with: found by writing it back and comparing."""
    for indent in (1, 2, 4):
        for tail in ('', '\n'):
            if json.dumps(data, indent=indent, ensure_ascii=False) + tail == text:
                return indent, tail
    return 1, ''  # not a style we know: the default of the other files


def clip_name(model, short):
    return 'animation.%s.%s' % (model, short)


def merge(model, exported_path, check=False, game_path=None, geo_path=None):
    if model not in MODELS:
        raise SystemExit('unknown model %r (one of %s)' % (model, ', '.join(MODELS)))
    howl, removed, has_out = MODELS[model]
    game_path = game_path or os.path.join(ASSETS, 'animations', 'entity', model + '.animation.json')
    geo_path = geo_path or os.path.join(ASSETS, 'geo', 'entity', model + '.geo.json')
    with open(game_path, encoding='utf-8', newline='') as f:
        text = f.read()
    game = json.loads(text)
    with open(exported_path, encoding='utf-8') as f:
        exported = json.load(f)
    with open(geo_path, encoding='utf-8') as f:
        bones = bones_of(json.load(f))

    wanted = ['shift_in', howl] + (['shift_out'] if has_out else [])
    allowed = {clip_name(model, w) for w in wanted}
    problems = []
    incoming = {}
    for name, clip in exported.get('animations', {}).items():
        if name not in allowed:
            continue  # the rest of an exported model is not ours: ignored, never merged
        short = name.rsplit('.', 1)[1]
        want = HOWL_LENGTH if short == howl else LENGTHS[short]
        if abs(clip.get('animation_length', -1) - want) > 1e-6:
            problems.append('%s is %s s long, it must be %s s' % (short, clip.get('animation_length'), want))
        if clip.get('loop') not in (None, False, 'false'):
            problems.append('%s loops: a morph clip plays once' % short)
        for bone in clip.get('bones', {}):
            if bone not in bones:
                problems.append('%s: %r is not a bone of %s.geo.json' % (short, bone, model))
        incoming[name] = clip
    for w in wanted:
        if clip_name(model, w) not in incoming:
            problems.append('the export has no %s' % w)
    if problems:
        raise SystemExit('nothing written:\n  ' + '\n  '.join(problems))

    indent, tail = style_of(text, game)
    animations = game['animations']
    merged = {}
    for name, clip in animations.items():
        short = name.rsplit('.', 1)[1]
        if name in incoming:
            merged[name] = incoming.pop(name)  # keeps its place
        elif short in removed:
            continue
        else:
            merged[name] = clip
    for name, clip in incoming.items():  # the new ones, after the last clip
        merged[name] = clip
    game['animations'] = merged
    out = json.dumps(game, indent=indent, ensure_ascii=False) + tail
    kept = [n for n in animations if n in merged and n not in {clip_name(model, w) for w in wanted}]
    print('%s: replaced/added %s; removed %s; kept %d other clips untouched' % (
        model, ', '.join(wanted), ', '.join(removed) or 'nothing', len(kept)))
    if check:
        return
    # written beside the file and moved over it, so that a failure half way leaves the game file as it was
    tmp = game_path + '.tmp'
    try:
        with open(tmp, 'w', encoding='utf-8', newline='') as f:
            f.write(out)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, game_path)
    except BaseException:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if len(args) != 2:
        raise SystemExit(__doc__)
    merge(args[0], args[1], '--check' in sys.argv)
