"""Checks of the placeholder tool on copies of a real file: python -m unittest art/creatures/morph/tools/test_make_placeholders.py (from the repo root)."""
import json
import os
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_placeholders as mp  # noqa: E402
import merge_clips as mc  # noqa: E402

MODEL = 'deucalion_demon_wolf'


class PlaceholderTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmp)
        self.game = os.path.join(self.tmp, 'game.json')
        self.geo = os.path.join(mc.ASSETS, 'geo', 'entity', MODEL + '.geo.json')
        shutil.copy(os.path.join(mc.ASSETS, 'animations', 'entity', MODEL + '.animation.json'), self.game)
        with open(self.game, encoding='utf-8', newline='') as f:
            text = f.read()
        data = json.loads(text)
        for name in ('shift_in', 'shift_out'):  # a file as main had it: no morph clips yet
            data['animations'].pop('animation.%s.%s' % (MODEL, name), None)
        with open(self.game, 'w', encoding='utf-8', newline='') as f:
            f.write(json.dumps(data, indent=1, ensure_ascii=False))

    def text(self):
        with open(self.game, encoding='utf-8', newline='') as f:
            return f.read()

    def test_it_writes_the_clips_into_a_file_that_has_none(self):
        mp.make(MODEL, game_path=self.game, geo_path=self.geo)
        names = json.loads(self.text())['animations']
        self.assertIn('animation.%s.shift_in' % MODEL, names)
        self.assertIn('animation.%s.shift_out' % MODEL, names)

    def test_it_refuses_a_file_that_already_has_a_shift_in_and_writes_nothing(self):
        mp.make(MODEL, game_path=self.game, geo_path=self.geo)
        before = self.text()
        with self.assertRaises(SystemExit) as why:
            mp.make(MODEL, game_path=self.game, geo_path=self.geo)
        self.assertIn('--force', str(why.exception))
        self.assertEqual(before, self.text())

    def test_force_overwrites(self):
        mp.make(MODEL, game_path=self.game, geo_path=self.geo)
        data = json.loads(self.text())
        data['animations']['animation.%s.shift_in' % MODEL]['animation_length'] = 1.0
        data['animations']['animation.%s.shift_in' % MODEL]['bones'] = {}
        with open(self.game, 'w', encoding='utf-8', newline='') as f:
            f.write(json.dumps(data, indent=1, ensure_ascii=False))
        mp.make(MODEL, force=True, game_path=self.game, geo_path=self.geo)
        self.assertTrue(json.loads(self.text())['animations']['animation.%s.shift_in' % MODEL]['bones'], 'force did not write the placeholder over the empty clip')


if __name__ == '__main__':
    unittest.main()