/**
 * Read the approved dashboard from Figma MCP. Append
 * `return await snapshotDashboard(config);` in use_figma. Pass figmaFileKey,
 * approvedPage, frameIds, componentIds and tokenIds from design/dashboard.json.
 */
async function snapshotDashboard({ figmaFileKey, approvedPage, frameIds, componentIds = [], tokenIds }) {
  const page = await figma.getNodeByIdAsync(approvedPage)
  if (page?.type !== 'PAGE') throw Error('The approved dashboard page could not be read.')
  await figma.setCurrentPageAsync(page)

  const components = {}
  const referencedVariables = new Set(tokenIds)
  const frames = {}
  const collections = new Map()
  const variables = {}
  const fields = [
    'visible', 'opacity', 'blendMode', 'clipsContent', 'isMask', 'layoutMode',
    'layoutPositioning', 'layoutSizingHorizontal', 'layoutSizingVertical',
    'primaryAxisAlignItems', 'counterAxisAlignItems', 'itemSpacing',
    'counterAxisSpacing', 'paddingTop', 'paddingRight', 'paddingBottom',
    'paddingLeft', 'topLeftRadius', 'topRightRadius', 'bottomRightRadius',
    'bottomLeftRadius', 'strokeWeight', 'strokeAlign', 'strokeCap', 'strokeJoin',
    'layoutWrap', 'constraints', 'fontName', 'fontSize', 'lineHeight',
    'letterSpacing', 'textAlignHorizontal', 'textAlignVertical', 'textAutoResize',
    'textTruncation', 'maxLines', 'overflowDirection', 'expanded',
  ]

  function addAliases(value, aliases) {
    if (!value || typeof value !== 'object') return
    if (value.type === 'VARIABLE_ALIAS') aliases.add(value.id)
    else for (const nested of Object.values(value)) addAliases(nested, aliases)
  }

  function hash(value) {
    const input = JSON.stringify(value)
    let first = 2166136261
    let second = 0
    for (let i = 0; i < input.length; i++) {
      const char = input.charCodeAt(i)
      first = Math.imul(first ^ char, 16777619)
      second = Math.imul(second ^ char, 2246822519) + i | 0
    }
    return [first >>> 0, second >>> 0].map(n => n.toString(16).padStart(8, '0')).join('')
  }

  function canonical(value) {
    if (typeof value === 'number') return Math.round(value * 10000) / 10000
    if (Array.isArray(value)) return value.map(canonical)
    if (value && typeof value === 'object') {
      return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])]))
    }
    return typeof value === 'symbol' ? String(value) : value
  }

  function recordComponent(owner) {
    if (owner.id in components) return
    components[owner.id] = null
    const nested = { components: new Set(), variables: new Set() }
    components[owner.id] = {
      properties: hash(canonical(owner.componentPropertyDefinitions)),
      layout: hash(canonical(inspect(owner, nested, true))),
      components: [...nested.components].sort(),
      variables: [...nested.variables].sort(),
    }
    for (const id of nested.variables) referencedVariables.add(id)
  }

  function inspect(node, dependencies, isRoot = false) {
    const layout = { type: node.type }
    if (!isRoot) layout.transform = node.relativeTransform
    if (node.type !== 'TEXT' || node.textAutoResize === 'NONE' || node.textAutoResize === 'TRUNCATE') {
      layout.width = node.width
      layout.height = node.height
    }
    if (node.type === 'TEXT' && node.textAutoResize === 'HEIGHT') layout.width = node.width
    for (const field of fields) {
      if (field in node) layout[field] = node[field]
    }
    for (const field of ['fills', 'strokes', 'effects']) {
      if (!Array.isArray(node[field])) continue
      // Image bytes and crops are sample content. Their bounds remain in the layout.
      layout[field] = node[field].map(p => p.type === 'IMAGE'
        ? { type: p.type, visible: p.visible, opacity: p.opacity, scaleMode: p.scaleMode }
        : p)
    }
    if (node.type === 'VECTOR' && !node.name.includes('Avatar')) layout.paths = node.vectorPaths
    layout.bindings = node.boundVariables
    layout.modes = node.explicitVariableModes
    addAliases(layout, dependencies.variables)
    if (node.type === 'INSTANCE' && node.mainComponent) {
      const component = node.mainComponent
      const owner = component.parent?.type === 'COMPONENT_SET' ? component.parent : component
      layout.component = component.id
      dependencies.components.add(owner.id)
      recordComponent(owner)
    }
    if (node.children) layout.children = node.children.map(child => inspect(child, dependencies))
    return layout
  }

  const roots = await Promise.all(frameIds.map(id => figma.getNodeByIdAsync(id)))
  for (let index = 0; index < roots.length; index++) {
    const root = roots[index]
    if (root?.type !== 'FRAME') throw Error(`Approved frame ${frameIds[index]} could not be read.`)
    const dependencies = { components: new Set(), variables: new Set() }
    const layout = inspect(root, dependencies, true)
    frames[root.id] = {
      layout: hash(canonical(layout)),
      components: [...dependencies.components].sort(),
      variables: [...dependencies.variables].sort(),
    }
    for (const id of dependencies.variables) referencedVariables.add(id)
  }

  const libraryComponents = await Promise.all(componentIds.map(id => figma.getNodeByIdAsync(id)))
  for (let index = 0; index < libraryComponents.length; index++) {
    const component = libraryComponents[index]
    if (component?.type !== 'COMPONENT_SET' && (component?.type !== 'COMPONENT' || component.parent?.type === 'COMPONENT_SET')) {
      throw Error(`Library component ${componentIds[index]} could not be read.`)
    }
    recordComponent(component)
  }

  // Reading an alias target may add another ID to the set during iteration.
  for (const id of referencedVariables) {
    const variable = await figma.variables.getVariableByIdAsync(id)
    if (!variable) continue
    const collectionId = variable.variableCollectionId
    if (!collections.has(collectionId)) {
      collections.set(collectionId, await figma.variables.getVariableCollectionByIdAsync(collectionId))
    }
    const collection = collections.get(collectionId)
    const modes = {}
    const values = variable.valuesByMode
    for (const { modeId, name } of collection?.modes ?? []) {
      const value = values[modeId]
      if (value?.type === 'VARIABLE_ALIAS') referencedVariables.add(value.id)
      if (typeof value?.r === 'number') {
        const channels = [value.r, value.g, value.b]
        if (typeof value.a === 'number' && value.a !== 1) channels.push(value.a)
        modes[name] = channels.map(x => Math.round(x * 255).toString(16).padStart(2, '0')).join('')
      } else modes[name] = value
    }
    variables[id] = { name: variable.name, modes, android: variable.codeSyntax?.ANDROID }
  }

  return { figmaFileKey, frames, components, variables }
}
