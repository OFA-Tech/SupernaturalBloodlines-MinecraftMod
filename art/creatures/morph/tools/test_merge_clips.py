"""Checks of the merge tool on copies of the real files: python -m unittest art/creatures/morph/tools/test_merge_clips.py (from the repo root)."""
import copy
import json
import os
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import merge_clips as mc  # noqa: E402


def real(model, kind):
    folder = 'animations' if kind == 'animation' else 'geo'
    return os.path.join(mc.ASSETS, folder, 'entity', '%s.%s.json' % (model, kind))


class MergeTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmp)
        self.game = os.path.join(self.tmp, 'game.json')
        shutil.copy(real('deucalion_demon_wolf', 'animation'), self.game)
        self.geo = real('deucalion_demon_wolf', 'geo')
        with open(self.game, encoding='utf-8') as f:
            self.before = json.load(f)['animations']
        with open(self.geo, encoding='utf-8') as f:
            self.bone = json.load(f)['minecraft:geometry'][0]['bones'][0]['name']

    def export(self, **changes):
        name = lambda s: 'animation.deucalion_demon_wolf.' + s  # noqa: E731
        clip = lambda length: {'animation_length': length, 'bones': {self.bone: {'rotation': {'0.0': [0, 0, 0]}}}}  # noqa: E731
        clips = {name('shift_in'): clip(1.0), name('shift_out'): clip(1.0), name('roar'): clip(2.2)}
        clips.update({name(k): v for k, v in changes.items()})
        path = os.path.join(self.tmp, 'export.json')
        with open(path, 'w', encoding='utf-8') as f:
            json.dump({'animations': clips}, f)
        return path

    def merged(self):
        with open(self.game, encoding='utf-8') as f:
            return json.load(f)['animations']

    def test_the_other_clips_are_kept_exactly_and_in_order(self):
        mc.merge('deucalion_demon_wolf', self.export(), game_path=self.game, geo_path=self.geo)
        after = self.merged()
        gone = {'animation.deucalion_demon_wolf.drop_to_fours'}
        own = {'animation.deucalion_demon_wolf.' + s for s in ('shift_in', 'shift_out', 'roar')}
        kept = [n for n in self.before if n not in gone | own]
        self.assertEqual(kept, [n for n in after if n not in own])
        for n in kept:
            self.assertEqual(self.before[n], after[n], n)

    def test_the_new_clips_are_there_and_the_replaced_ones_are_gone(self):
        mc.merge('deucalion_demon_wolf', self.export(), game_path=self.game, geo_path=self.geo)
        after = self.merged()
        self.assertEqual(2.2, after['animation.deucalion_demon_wolf.roar']['animation_length'])
        self.assertIn('animation.deucalion_demon_wolf.shift_in', after)
        self.assertIn('animation.deucalion_demon_wolf.shift_out', after)
        self.assertNotIn('animation.deucalion_demon_wolf.drop_to_fours', after)

    def test_the_file_style_is_kept(self):
        with open(self.game, encoding='utf-8', newline='') as f:
            text = f.read()
        mc.merge('deucalion_demon_wolf', self.export(), game_path=self.game, geo_path=self.geo)
        with open(self.game, encoding='utf-8', newline='') as f:
            out = f.read()
        self.assertEqual(mc.style_of(text, json.loads(text)), mc.style_of(out, json.loads(out)))

    def test_an_unknown_bone_writes_nothing(self):
        bad = {'animation_length': 1.0, 'bones': {'no_such_bone': {'rotation': {'0.0': [0, 0, 0]}}}}
        with open(self.game, encoding='utf-8', newline='') as f:
            before = f.read()
        with self.assertRaises(SystemExit):
            mc.merge('deucalion_demon_wolf', self.export(shift_in=bad), game_path=self.game, geo_path=self.geo)
        with open(self.game, encoding='utf-8', newline='') as f:
            self.assertEqual(before, f.read())

    def test_a_wrong_length_or_a_loop_is_refused(self):
        clip = {'animation_length': 1.5, 'bones': {self.bone: {'rotation': {'0.0': [0, 0, 0]}}}}
        with self.assertRaises(SystemExit):
            mc.merge('deucalion_demon_wolf', self.export(shift_in=clip), game_path=self.game, geo_path=self.geo)
        looping = copy.deepcopy(clip)
        looping.update({'animation_length': 1.0, 'loop': True})
        with self.assertRaises(SystemExit):
            mc.merge('deucalion_demon_wolf', self.export(shift_in=looping), game_path=self.game, geo_path=self.geo)

    def test_other_clips_of_an_exported_model_are_ignored(self):
        path = self.export(**{'idle': {'animation_length': 9.0, 'bones': {}}})
        mc.merge('deucalion_demon_wolf', path, game_path=self.game, geo_path=self.geo)
        self.assertEqual(self.before['animation.deucalion_demon_wolf.idle'], self.merged()['animation.deucalion_demon_wolf.idle'])

    def test_the_file_is_replaced_in_one_move_from_a_temporary_one(self):
        from unittest import mock
        with open(self.game, encoding='utf-8', newline='') as f:
            before = f.read()
        real = os.replace
        seen = {}

        def spy(src, dst):
            with open(dst, encoding='utf-8', newline='') as f:
                seen['old'] = f.read()
            with open(src, encoding='utf-8', newline='') as f:
                seen['new'] = f.read()
            seen['src'] = src
            real(src, dst)

        with mock.patch('os.replace', spy):
            mc.merge('deucalion_demon_wolf', self.export(), game_path=self.game, geo_path=self.geo)
        self.assertEqual(before, seen['old'], 'the game file was already changed when it was replaced')
        self.assertNotEqual(before, seen['new'])
        self.assertTrue(seen['src'].endswith('.tmp'))
        self.assertFalse(os.path.exists(seen['src']), 'the temporary file was left behind')

    def test_a_failure_while_replacing_leaves_the_file_and_no_temporary_one(self):
        from unittest import mock
        with open(self.game, encoding='utf-8', newline='') as f:
            before = f.read()
        with mock.patch('os.replace', side_effect=OSError('disk')):
            with self.assertRaises(OSError):
                mc.merge('deucalion_demon_wolf', self.export(), game_path=self.game, geo_path=self.geo)
        with open(self.game, encoding='utf-8', newline='') as f:
            self.assertEqual(before, f.read())
        self.assertFalse(os.path.exists(self.game + '.tmp'))

    def test_a_missing_clip_is_refused(self):
        path = os.path.join(self.tmp, 'export.json')
        with open(path, 'w', encoding='utf-8') as f:
            json.dump({'animations': {}}, f)
        with self.assertRaises(SystemExit):
            mc.merge('deucalion_demon_wolf', path, game_path=self.game, geo_path=self.geo)


if __name__ == '__main__':
    unittest.main()
