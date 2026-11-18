import re, json, sys
src = sys.argv[1]; out = sys.argv[2]
lines = open(src, encoding='utf-8', errors='replace').read().split('\n')
# locate createBodyLayer
start = next(i for i, l in enumerate(lines) if 'createBodyLayer()' in l and 'static' in l)
code = []
i = start
while i < len(lines):
    l = lines[i]
    if i > start + 3 and re.match(r'^  \S', l) and 'Code:' not in l:
        break
    code.append(l); i += 1
st = []; loc = {}
parts = {}   # name -> dict
order = []
def num(s): return float(s)
unk = set()
for l in code:
    m = re.match(r'\s+\d+: (\w+)\s*(.*)', l)
    if not m: continue
    op, rest = m.group(1), m.group(2)
    if op.startswith('aload'):
        n = int(op.split('_')[1]) if '_' in op else int(rest.split()[0]); st.append(('v', loc.get(n)))
    elif op.startswith('astore'):
        n = int(op.split('_')[1]) if '_' in op else int(rest.split()[0]); loc[n] = st.pop()[1]
    elif op in ('ldc', 'ldc_w', 'ldc2_w'):
        mm = re.search(r'// (String|float|int|double) (.*)', rest)
        t, v = mm.group(1), mm.group(2)
        st.append(('c', v if t == 'String' else float(v.rstrip('fd'))))
    elif op.startswith('fconst') or op.startswith('iconst') or op.startswith('dconst'):
        v = op.split('_')[1]; st.append(('c', -1.0 if v == 'm1' else float(v)))
    elif op in ('bipush', 'sipush'):
        st.append(('c', float(rest.split()[0])))
    elif op == 'getstatic':
        st.append(('c', 'POSE_ZERO' if 'PartPose.ZERO' in rest else 'DEF'))
    elif op == 'new': st.append(('c', 'NEW'))
    elif op == 'dup': st.append(st[-1])
    elif op == 'pop': st.pop()
    elif op == 'invokestatic':
        if 'CubeListBuilder.create' in rest: st.append(('clb', {'cubes': [], 'tex': [0, 0], 'mirror': False}))
        elif 'PartPose.offsetAndRotation' in rest:
            a = [st.pop()[1] for _ in range(6)][::-1]; st.append(('pose', {'o': a[:3], 'r': a[3:]}))
        elif 'PartPose.offset:' in rest:
            a = [st.pop()[1] for _ in range(3)][::-1]; st.append(('pose', {'o': a, 'r': [0, 0, 0]}))
        elif 'PartPose.rotation' in rest:
            a = [st.pop()[1] for _ in range(3)][::-1]; st.append(('pose', {'o': [0, 0, 0], 'r': a}))
        elif 'createMesh' in rest: st.pop(); st.append(('v', 'MESH'))
        else: unk.add(rest)
    elif op == 'invokespecial':
        if 'CubeDeformation."<init>":(F)' in rest: f=st.pop()[1]; st.pop(); st[-1] = ('c', ('DEF', f))
        elif 'CubeDeformation."<init>":(FF' in rest: pass
        else: unk.add(rest)
    elif op == 'invokevirtual':
        if 'texOffs:(II)' in rest:
            y = st.pop()[1]; x = st.pop()[1]; c = st.pop()[1]; c['tex'] = [x, y]; st.append(('clb', c))
        elif 'mirror:()' in rest or 'mirror:(Z)' in rest:
            if '(Z)' in rest: st.pop()
            c = st.pop()[1]; c['mirror'] = True; st.append(('clb', c))
        elif 'addBox:(FFFFFFLnet' in rest or 'addBox:(FFFFFFL' in rest:
            d = st.pop(); a = [st.pop()[1] for _ in range(6)][::-1]; c = st.pop()[1]
            c['cubes'].append({'tex': list(c['tex']), 'box': a, 'mirror': c['mirror'], 'grow': d[1][1] if isinstance(d[1],tuple) else 0}); st.append(('clb', c))
        elif 'addBox:(FFFFFF)' in rest:
            a = [st.pop()[1] for _ in range(6)][::-1]; c = st.pop()[1]
            c['cubes'].append({'tex': list(c['tex']), 'box': a, 'mirror': c['mirror']}); st.append(('clb', c))
        elif 'getRoot' in rest: st.pop(); st.append(('v', 'root'))
        elif 'addOrReplaceChild' in rest:
            pose = st.pop()[1]; clb = st.pop()[1]; name = st.pop()[1]; par = st.pop()[1]
            if pose == 'POSE_ZERO': pose = {'o': [0, 0, 0], 'r': [0, 0, 0]}
            p = {'name': name, 'parent': par, 'pose': pose, 'cubes': clb['cubes'] if isinstance(clb, dict) else []}
            parts[name] = p; order.append(name); st.append(('v', name))
        else: unk.add(rest)
    elif op in ('return', 'areturn'): pass
    else: unk.add(op + ' ' + rest[:60])
json.dump({'order': order, 'parts': parts}, open(out, 'w'), indent=1)
print(len(order), 'parts', 'unk:', list(unk)[:20])

