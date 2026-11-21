const dump = (o) => { const r = {}; for (const k in o) r[k] = hex6(o[k]); return r; };
return { note: 'Colours of the three kits as drawn in the style guide (phase 1). Names match the kit files in src/.', grimoire: dump(GP), bloodmoon: dump(MP), tensura: dump(TP) };
