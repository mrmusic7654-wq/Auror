export const CFG = {
  blocks: 7,
  blockSize: 42,
  road: 12,
  sidewalk: 2.6,
  lane: 3.2,
  people: 120,
  cars: 48,
  treesPerPark: 28,
}

export function worldSpan() {
  return CFG.blocks * (CFG.blockSize + CFG.road) + CFG.road
}

export function blockOrigin(i, j) {
  const cell = CFG.blockSize + CFG.road
  const span = worldSpan()
  const x = -span / 2 + CFG.road + i * cell
  const z = -span / 2 + CFG.road + j * cell
  return { x, z }
}

export function intersectionPos(i, j) {
  const cell = CFG.blockSize + CFG.road
  const span = worldSpan()
  const x = -span / 2 + CFG.road / 2 + i * cell
  const z = -span / 2 + CFG.road / 2 + j * cell
  return { x, z }
}
