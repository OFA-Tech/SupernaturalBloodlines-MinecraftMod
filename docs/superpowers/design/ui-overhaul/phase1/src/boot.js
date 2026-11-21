const PAGE = figma.root.children.find((p) => p.name === 'UI Overhaul — Style Guide');
await figma.setCurrentPageAsync(PAGE);
await loadFonts(); await initInter();
const t0 = Date.now();
const done = (extra) => ({ ms: Date.now() - t0, ...extra });
