"""Placeholder morph clips, so the code of phase 7 runs and is tested before Fable's art lands.

    python art/creatures/morph/tools/make_placeholders.py            (all four bosses)
    python art/creatures/morph/tools/make_placeholders.py klaus_hybrid

For each boss it writes the clips the contract asks for, by the one tool the art uses (merge_clips.py), so the game files and the tool are exercised together:
  shift_in   1.0 s: the first `idle` pose, held (so it already ends on the idle pose, as the real clip must)
  shift_out  1.0 s: the same (Klaus, Deucalion, Peter)
  the howl   2.2 s: the existing howl (`howl` for Klaus, `roar` for the others) with its keys spread over 2.2 s. The real one is re-timed by hand, not stretched.
These are placeholders: Fable replaces all of them (plan A2 to A5), and nothing here is meant to look good.
"""
import copy
import json
import os
import sys
import tempfile

import merge_clips as mc


def first_key(channel):
    """The value at the first keyframe of a channel (a vector, or a keyframe object), as a plain vector or a keyframe object."""
    if isinstance(channel, dict) and not ('vector' in channel or 'post' in channel or 'pre' in channel):
        return channel[min(channel, key=float)]
    return channel


def hold(idle, length):
    bones = {}
    for bone, channels in idle.get('bones', {}).items():
        out = {}
        for name, channel in channels.items():
            value = first_key(channel)
            out[name] = {'0.0': copy.deepcopy(value), '%s' % float(length): copy.deepcopy(value)}
        bones[bone] = out
    return {'animation_length': float(length), 'bones': bones}


def stretch(clip, length):
    """The same clip with every keyframe time multiplied so that it lasts `length` seconds."""
    k = length / clip['animation_length']
    out = copy.deepcopy(clip)
    out['animation_length'] = float(length)
    out['loop'] = False
    for channels in out.get('bones', {}).values():
        for name, channel in list(channels.items()):
            if isinstance(channel, dict) and not ('vector' in channel or 'post' in channel or 'pre' in channel):
                channels[name] = {'%s' % round(float(t) * k, 4): v for t, v in channel.items()}
    return out


def make(model):
    howl, _removed, has_out = mc.MODELS[model]
    game_path = os.path.join(mc.ASSETS, 'animations', 'entity', model + '.animation.json')
    with open(game_path, encoding='utf-8') as f:
        animations = json.load(f)['animations']
    idle = animations[mc.clip_name(model, 'idle')]
    source = animations[mc.clip_name(model, howl)]
    if abs(source['animation_length'] - mc.HOWL_LENGTH) < 1e-6:
        howl_clip = copy.deepcopy(source)  # already re-timed (a second run): keep it
    else:
        howl_clip = stretch(source, mc.HOWL_LENGTH)
    clips = {mc.clip_name(model, 'shift_in'): hold(idle, 1.0), mc.clip_name(model, howl): howl_clip}
    if has_out:
        clips[mc.clip_name(model, 'shift_out')] = hold(idle, 1.0)
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, model + '.json')
        with open(path, 'w', encoding='utf-8') as f:
            json.dump({'format_version': '1.8.0', 'animations': clips}, f)
        mc.merge(model, path, allow_linear=True)


if __name__ == '__main__':
    for name in (sys.argv[1:] or list(mc.MODELS)):
        make(name)
