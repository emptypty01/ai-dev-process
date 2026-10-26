// 固定的只读 Penpot 结构查询，不接受或执行调用方代码。
if (!penpot.currentFile || !penpot.currentPage) throw new Error('No active Penpot file/page');
const maxNodes = 600;
const maxDepth = 12;
let nodeCount = 0;
let truncated = false;
function read(getter) {
  try { return getter(); } catch { return undefined; }
}
function serialize(shape, depth) {
  nodeCount += 1;
  const value = { id: shape.id, name: shape.name, type: shape.type,
    x: shape.x, y: shape.y, width: shape.width, height: shape.height };
  if (shape.type === 'text') value.text = read(() => shape.characters);
  const fills = read(() => shape.fills);
  if (Array.isArray(fills)) value.fills = fills.map(fill => ({ color: fill.fillColor, opacity: fill.fillOpacity }));
  const interactions = read(() => shape.interactions);
  if (Array.isArray(interactions) && interactions.length) {
    value.interactions = interactions.map(interaction => ({
      trigger: read(() => interaction.trigger),
      actionType: read(() => interaction.action.type),
      destinationId: read(() => interaction.action.destination.id),
    }));
  }
  const children = read(() => shape.children) || [];
  if (children.length) {
    value.children = [];
    for (const child of children) {
      if (nodeCount >= maxNodes || depth >= maxDepth) { truncated = true; break; }
      value.children.push(serialize(child, depth + 1));
    }
    if (value.children.length < children.length) value.omittedChildren = children.length - value.children.length;
  }
  return value;
}
const roots = [];
for (const shape of penpot.currentPage.root.children) {
  if (nodeCount >= maxNodes) { truncated = true; break; }
  roots.push(serialize(shape, 0));
}
return {
  file: { id: penpot.currentFile.id, name: penpot.currentFile.name },
  page: { id: penpot.currentPage.id, name: penpot.currentPage.name },
  nodes: roots, nodeCount, truncated, limits: { maxNodes, maxDepth },
};
