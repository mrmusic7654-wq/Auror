import * as THREE from 'three'
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js'
import { CFG, blockOrigin, intersectionPos, worldSpan } from './config.js'
import {
  makeGrass, makeAsphalt, makeSidewalk, makeFacade, makeBrick, makeRoof, makeBark,
} from './textures.js'

function boxAt(w, h, d, x, y, z) {
  const g = new THREE.BoxGeometry(w, h, d)
  g.translate(x, y, z)
  return g
}

function cylAt(rTop, rBot, h, x, y, z, segs = 8) {
  const g = new THREE.CylinderGeometry(rTop, rBot, h, segs)
  g.translate(x, y, z)
  return g
}

export function createCity(scene) {
  const colliders = []
  const parks = []
  const sidewalkLoops = []
  const lampPositions = []
  const trafficLights = []

  const grassTex = makeGrass()
  const asphaltTex = makeAsphalt()
  const walkTex = makeSidewalk()
  const brickTex = makeBrick()
  const roofTex = makeRoof()
  const barkTex = makeBark()

  const facades = [
    makeFacade({ base: '#c4b8a8', accent: '#f2e2a0', cols: 8, rows: 12, litChance: 0.4 }),
    makeFacade({ base: '#8ea4b8', accent: '#b8d8ff', cols: 10, rows: 16, litChance: 0.55 }),
    makeFacade({ base: '#6b4a3a', accent: '#ffd090', cols: 6, rows: 8, litChance: 0.3 }),
    makeFacade({ base: '#3d4450', accent: '#9ad0ff', cols: 12, rows: 20, litChance: 0.6 }),
    makeFacade({ base: '#d8cfc2', accent: '#fff2c8', cols: 7, rows: 10, litChance: 0.35 }),
  ]

  const mats = {
    grass: new THREE.MeshStandardMaterial({ map: grassTex, roughness: 0.95 }),
    asphalt: new THREE.MeshStandardMaterial({ map: asphaltTex, roughness: 0.92, color: 0x888888 }),
    walk: new THREE.MeshStandardMaterial({ map: walkTex, roughness: 0.88 }),
    brick: new THREE.MeshStandardMaterial({ map: brickTex, roughness: 0.85 }),
    roof: new THREE.MeshStandardMaterial({ map: roofTex, roughness: 0.8 }),
    concrete: new THREE.MeshStandardMaterial({ color: 0x9a958c, roughness: 0.9 }),
    dark: new THREE.MeshStandardMaterial({ color: 0x2a2d33, roughness: 0.7 }),
    metal: new THREE.MeshStandardMaterial({ color: 0x555960, metalness: 0.7, roughness: 0.35 }),
    gold: new THREE.MeshStandardMaterial({ color: 0xc9a227, metalness: 0.6, roughness: 0.35 }),
    water: new THREE.MeshStandardMaterial({
      color: 0x3a7ca5, roughness: 0.12, metalness: 0.3, transparent: true, opacity: 0.85,
    }),
    bark: new THREE.MeshStandardMaterial({ map: barkTex, roughness: 1 }),
    leaf: new THREE.MeshStandardMaterial({ color: 0x2f6a38, roughness: 0.85 }),
    leaf2: new THREE.MeshStandardMaterial({ color: 0x4a7c3f, roughness: 0.85 }),
    white: new THREE.MeshStandardMaterial({ color: 0xe8e4d4, roughness: 0.7 }),
    line: new THREE.MeshBasicMaterial({ color: 0xf2e38a }),
    dash: new THREE.MeshBasicMaterial({ color: 0xf4f1ea }),
    awning: new THREE.MeshStandardMaterial({ color: 0xa33b2b, roughness: 0.7 }),
    glassNight: facades.map((f) => new THREE.MeshStandardMaterial({
      map: f.map,
      emissiveMap: f.emissive,
      emissive: 0xffffff,
      emissiveIntensity: 0.15,
      roughness: 0.35,
      metalness: 0.15,
    })),
  }

  const buckets = {
    brick: [], roof: [], concrete: [], dark: [], metal: [], gold: [],
    water: [], bark: [], leaf: [], leaf2: [], white: [], awning: [],
    glass0: [], glass1: [], glass2: [], glass3: [], glass4: [],
  }

  const span = worldSpan()
  const n = CFG.blocks
  const cx = (n - 1) / 2

  const ground = new THREE.Mesh(new THREE.PlaneGeometry(span + 80, span + 80), mats.grass)
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  const roadGeos = []
  const walkGeos = []
  const lineGeos = []
  const dashGeos = []

  for (let i = 0; i <= n; i++) {
    const a = intersectionPos(i, 0)
    roadGeos.push(boxAt(CFG.road, 0.08, span, a.x, 0.04, 0))
    const c = intersectionPos(0, i)
    roadGeos.push(boxAt(span, 0.08, CFG.road, 0, 0.04, c.z))

    lineGeos.push(boxAt(0.18, 0.02, span, a.x, 0.09, 0))
    lineGeos.push(boxAt(span, 0.02, 0.18, 0, 0.09, c.z))

    for (let s = -span / 2; s < span / 2; s += 7) {
      dashGeos.push(boxAt(0.12, 0.02, 2.4, a.x - CFG.lane * 0.55, 0.095, s + 1.2))
      dashGeos.push(boxAt(0.12, 0.02, 2.4, a.x + CFG.lane * 0.55, 0.095, s + 1.2))
      dashGeos.push(boxAt(2.4, 0.02, 0.12, s + 1.2, 0.095, c.z - CFG.lane * 0.55))
      dashGeos.push(boxAt(2.4, 0.02, 0.12, s + 1.2, 0.095, c.z + CFG.lane * 0.55))
    }
  }

  for (let i = 0; i < n; i++) {
    for (let j = 0; j < n; j++) {
      const o = blockOrigin(i, j)
      const s = CFG.sidewalk
      const bs = CFG.blockSize
      walkGeos.push(boxAt(bs, 0.12, s, o.x + bs / 2, 0.08, o.z + s / 2))
      walkGeos.push(boxAt(bs, 0.12, s, o.x + bs / 2, 0.08, o.z + bs - s / 2))
      walkGeos.push(boxAt(s, 0.12, bs - 2 * s, o.x + s / 2, 0.08, o.z + bs / 2))
      walkGeos.push(boxAt(s, 0.12, bs - 2 * s, o.x + bs - s / 2, 0.08, o.z + bs / 2))

      const inset = s + 0.6
      sidewalkLoops.push([
        new THREE.Vector3(o.x + inset, 0, o.z + inset),
        new THREE.Vector3(o.x + bs - inset, 0, o.z + inset),
        new THREE.Vector3(o.x + bs - inset, 0, o.z + bs - inset),
        new THREE.Vector3(o.x + inset, 0, o.z + bs - inset),
      ])

      for (let t = 0; t < 4; t++) {
        const along = 8 + t * 10
        if (along >= bs - 4) continue
        lampPositions.push(new THREE.Vector3(o.x + along, 0, o.z + 0.7))
        lampPositions.push(new THREE.Vector3(o.x + along, 0, o.z + bs - 0.7))
        lampPositions.push(new THREE.Vector3(o.x + 0.7, 0, o.z + along))
        lampPositions.push(new THREE.Vector3(o.x + bs - 0.7, 0, o.z + along))
      }

      const cheb = Math.max(Math.abs(i - cx), Math.abs(j - cx))
      const rnd = seeded(i * 31 + j * 17)
      let kind = 'house'
      if (cheb === 0) kind = 'plaza'
      else if (cheb === 1) kind = 'downtown'
      else if (cheb === 2) kind = rnd() < 0.2 ? 'park' : 'mixed'
      else kind = rnd() < 0.18 ? 'park' : rnd() < 0.5 ? 'house' : 'shop'

      if (kind === 'park' || kind === 'plaza') {
        parks.push({ i, j, ...o, kind })
        fillPark(o, kind, buckets, colliders)
      } else {
        fillBlock(o, kind, rnd, buckets, colliders, facades)
      }
    }
  }

  addMesh(scene, merge(roadGeos), mats.asphalt, true)
  addMesh(scene, merge(walkGeos), mats.walk, true)
  addMesh(scene, merge(lineGeos), mats.line, false)
  addMesh(scene, merge(dashGeos), mats.dash, false)

  const glassBuckets = ['glass0', 'glass1', 'glass2', 'glass3', 'glass4']
  for (const [key, geos] of Object.entries(buckets)) {
    if (!geos.length) continue
    const gi = glassBuckets.indexOf(key)
    const mat = gi >= 0 ? mats.glassNight[gi] : mats[key]
    if (!mat) continue
    addMesh(scene, merge(geos), mat, true)
  }

  const lamps = buildLamps(scene, lampPositions, mats)
  buildTrafficLights(scene, n, mats, trafficLights)
  scatterStreetProps(scene, n, mats)

  const nightMaterials = mats.glassNight
  const lampGlows = lamps.glows

  return {
    colliders,
    parks,
    sidewalkLoops,
    lampPositions,
    trafficLights,
    nightMaterials,
    lampGlows,
    lamps,
    span,
    n,
    lightPhase: 'ns',
  }
}

function addMesh(scene, geo, mat, shadows) {
  if (!geo) return
  const m = new THREE.Mesh(geo, mat)
  m.castShadow = shadows
  m.receiveShadow = true
  scene.add(m)
  return m
}

function merge(geos) {
  if (!geos.length) return null
  const m = mergeGeometries(geos, false)
  geos.forEach((g) => g.dispose())
  return m
}

function seeded(seed) {
  let s = seed + 1
  return () => {
    s = (s * 16807) % 2147483647
    return (s - 1) / 2147483646
  }
}

function fillPark(o, kind, buckets, colliders) {
  const bs = CFG.blockSize
  const s = CFG.sidewalk
  const inner = bs - 2 * s - 1
  const cx = o.x + bs / 2
  const cz = o.z + bs / 2

  if (kind === 'plaza') {
    buckets.concrete.push(boxAt(inner * 0.72, 0.1, inner * 0.72, cx, 0.12, cz))
    buckets.white.push(cylAt(4.2, 4.2, 0.35, cx, 0.32, cz, 16))
    buckets.water.push(cylAt(3.4, 3.4, 0.28, cx, 0.5, cz, 16))
    buckets.stone = buckets.stone || []
    buckets.gold.push(cylAt(0.45, 0.55, 3.4, cx, 2.1, cz, 10))
    buckets.gold.push(cylAt(1.1, 0.3, 0.35, cx, 3.9, cz, 10))
    // clock tower
    buckets.brick.push(boxAt(6, 22, 6, cx + 12, 11, cz + 12))
    buckets.gold.push(boxAt(6.4, 1.2, 6.4, cx + 12, 22.4, cz + 12))
    buckets.dark.push(boxAt(2.2, 2.2, 0.2, cx + 12, 18.5, cz + 12 + 3.12))
    buckets.dark.push(boxAt(2.2, 2.2, 0.2, cx + 12, 18.5, cz + 12 - 3.12))
    buckets.metal.push(cylAt(0.12, 0.12, 4.5, cx + 12, 25.2, cz + 12, 6))
    colliders.push({ minX: cx + 9, maxX: cx + 15, minZ: cz + 9, maxZ: cz + 15, h: 26 })
    colliders.push({ minX: cx - 4.5, maxX: cx + 4.5, minZ: cz - 4.5, maxZ: cz + 4.5, h: 4 })
    for (let k = 0; k < 10; k++) {
      const a = (k / 10) * Math.PI * 2
      const x = cx + Math.cos(a) * 11
      const z = cz + Math.sin(a) * 11
      addTree(buckets, x, z, 1 + (k % 3) * 0.12)
    }
  } else {
    buckets.concrete.push(boxAt(3.2, 0.08, inner * 0.7, cx, 0.1, cz))
    buckets.concrete.push(boxAt(inner * 0.7, 0.08, 3.2, cx, 0.1, cz))
    for (let k = 0; k < CFG.treesPerPark; k++) {
      const x = o.x + s + 2 + Math.random() * (bs - 2 * s - 4)
      const z = o.z + s + 2 + Math.random() * (bs - 2 * s - 4)
      if (Math.hypot(x - cx, z - cz) < 4) continue
      addTree(buckets, x, z, 0.85 + Math.random() * 0.5)
    }
    // benches
    for (let b = 0; b < 4; b++) {
      const ang = (b / 4) * Math.PI * 2
      const x = cx + Math.cos(ang) * 8
      const z = cz + Math.sin(ang) * 8
      buckets.dark.push(boxAt(1.6, 0.12, 0.45, x, 0.45, z))
      buckets.dark.push(boxAt(1.6, 0.4, 0.08, x, 0.72, z - 0.2))
    }
  }
}

function addTree(buckets, x, z, scale) {
  const h = 2.4 * scale
  buckets.bark.push(cylAt(0.18 * scale, 0.28 * scale, h, x, h / 2, z, 6))
  const leafBucket = Math.random() < 0.5 ? buckets.leaf : buckets.leaf2
  leafBucket.push(cylAt(0.2, 2.1 * scale, 3.2 * scale, x, h + 1.4 * scale, z, 7))
  leafBucket.push(cylAt(0.15, 1.4 * scale, 1.8 * scale, x, h + 2.6 * scale, z, 7))
}

function fillBlock(o, kind, rnd, buckets, colliders, facades) {
  const s = CFG.sidewalk + 0.8
  const lotX = o.x + s
  const lotZ = o.z + s
  const lotW = CFG.blockSize - 2 * s
  const lotD = CFG.blockSize - 2 * s

  if (kind === 'downtown') {
    const towers = rnd() < 0.45 ? 1 : 2
    if (towers === 1) {
      placeTower(lotX + 2, lotZ + 2, lotW - 4, lotD - 4, 28 + rnd() * 42, rnd, buckets, colliders, facades)
    } else {
      const w = (lotW - 3) / 2
      placeTower(lotX, lotZ, w, lotD, 18 + rnd() * 28, rnd, buckets, colliders, facades)
      placeTower(lotX + w + 3, lotZ, w, lotD, 22 + rnd() * 30, rnd, buckets, colliders, facades)
    }
    return
  }

  if (kind === 'mixed' || kind === 'shop') {
    const shops = 2 + Math.floor(rnd() * 2)
    const w = (lotW - (shops - 1) * 1.4) / shops
    for (let k = 0; k < shops; k++) {
      const h = kind === 'shop' ? 5 + rnd() * 4 : 8 + rnd() * 10
      placeBuilding(lotX + k * (w + 1.4), lotZ, w, lotD * (0.7 + rnd() * 0.28), h, rnd, buckets, colliders, facades, true)
    }
    return
  }

  const houses = 2 + Math.floor(rnd() * 2)
  const w = (lotW - (houses - 1) * 2.2) / houses
  for (let k = 0; k < houses; k++) {
    const depth = lotD * (0.45 + rnd() * 0.25)
    const h = 4.2 + rnd() * 2.4
    placeHouse(lotX + k * (w + 2.2), lotZ + 1.5, w * 0.9, depth, h, rnd, buckets, colliders)
  }
}

function placeTower(x, z, w, d, h, rnd, buckets, colliders, facades) {
  const gi = Math.floor(rnd() * 5)
  const y = h / 2
  const cx = x + w / 2
  const cz = z + d / 2
  buckets[`glass${gi}`].push(boxAt(w, h, d, cx, y, cz))
  buckets.concrete.push(boxAt(w + 0.6, 1.2, d + 0.6, cx, 0.6, cz))
  buckets.dark.push(boxAt(w * 0.4, 2.2, d * 0.3, cx, h + 1.1, cz))
  if (rnd() > 0.4) buckets.metal.push(cylAt(0.08, 0.08, 4 + rnd() * 6, cx + w * 0.2, h + 4, cz, 5))
  // setbacks
  if (h > 30) {
    buckets.dark.push(boxAt(w * 0.7, 3, d * 0.7, cx, h - 1.2, cz))
  }
  colliders.push({ minX: x, maxX: x + w, minZ: z, maxZ: z + d, h })
}

function placeBuilding(x, z, w, d, h, rnd, buckets, colliders, facades, shop) {
  const gi = Math.floor(rnd() * 5)
  const cx = x + w / 2
  const cz = z + d / 2
  buckets[`glass${gi}`].push(boxAt(w, h, d, cx, h / 2, cz))
  buckets.concrete.push(boxAt(w + 0.3, 0.4, d + 0.3, cx, 0.2, cz))
  if (shop) {
    buckets.awning.push(boxAt(w * 0.92, 0.12, 1.4, cx, 3.1, z - 0.2))
    buckets.dark.push(boxAt(w * 0.5, 2.2, 0.12, cx, 1.4, z + 0.05))
  }
  buckets.dark.push(boxAt(w * 0.25, 1.4, d * 0.25, cx, h + 0.8, cz))
  colliders.push({ minX: x, maxX: x + w, minZ: z, maxZ: z + d, h })
}

function placeHouse(x, z, w, d, h, rnd, buckets, colliders) {
  const cx = x + w / 2
  const cz = z + d / 2
  buckets.brick.push(boxAt(w, h, d, cx, h / 2, cz))
  // pitched roof via two slabs
  const roofA = new THREE.BoxGeometry(w + 0.5, 0.18, d * 0.62)
  roofA.rotateX(0.55)
  roofA.translate(cx, h + 0.7, cz - d * 0.18)
  buckets.roof.push(roofA)
  const roofB = new THREE.BoxGeometry(w + 0.5, 0.18, d * 0.62)
  roofB.rotateX(-0.55)
  roofB.translate(cx, h + 0.7, cz + d * 0.18)
  buckets.roof.push(roofB)
  buckets.brick.push(boxAt(0.4, 1.2, 0.4, cx + w * 0.25, h + 1.5, cz))
  buckets.dark.push(boxAt(0.9, 1.8, 0.08, cx, 0.95, z + 0.04))
  buckets.white.push(boxAt(0.7, 0.8, 0.06, cx - w * 0.22, 2.2, z + 0.04))
  buckets.white.push(boxAt(0.7, 0.8, 0.06, cx + w * 0.22, 2.2, z + 0.04))
  colliders.push({ minX: x, maxX: x + w, minZ: z, maxZ: z + d, h: h + 2 })
}

function buildLamps(scene, positions, mats) {
  const poleGeo = new THREE.CylinderGeometry(0.07, 0.09, 5.2, 6)
  const armGeo = new THREE.BoxGeometry(1.3, 0.07, 0.07)
  const glowGeo = new THREE.SphereGeometry(0.18, 8, 8)
  const poles = new THREE.InstancedMesh(poleGeo, mats.metal, positions.length)
  const arms = new THREE.InstancedMesh(armGeo, mats.metal, positions.length)
  const glowMat = new THREE.MeshStandardMaterial({
    color: 0xffe6b0, emissive: 0xffcc77, emissiveIntensity: 0.2, roughness: 0.4,
  })
  const glows = new THREE.InstancedMesh(glowGeo, glowMat, positions.length)
  poles.castShadow = true
  const dummy = new THREE.Object3D()
  positions.forEach((p, i) => {
    dummy.position.set(p.x, 2.6, p.z)
    dummy.rotation.set(0, 0, 0)
    dummy.updateMatrix()
    poles.setMatrixAt(i, dummy.matrix)
    dummy.position.set(p.x + 0.5, 5.15, p.z)
    dummy.updateMatrix()
    arms.setMatrixAt(i, dummy.matrix)
    dummy.position.set(p.x + 1.05, 5.0, p.z)
    dummy.updateMatrix()
    glows.setMatrixAt(i, dummy.matrix)
  })
  scene.add(poles, arms, glows)
  return { poles, arms, glows, glowMat }
}

function buildTrafficLights(scene, n, mats, store) {
  for (let i = 0; i <= n; i++) {
    for (let j = 0; j <= n; j++) {
      if (i === 0 || j === 0 || i === n || j === n) {
        if (Math.abs(i - n / 2) > 2 && Math.abs(j - n / 2) > 2) continue
      }
      const p = intersectionPos(i, j)
      const group = new THREE.Group()
      const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.1, 4.2, 6), mats.metal)
      pole.position.y = 2.1
      group.add(pole)
      const head = new THREE.Mesh(new THREE.BoxGeometry(0.28, 0.85, 0.22), mats.dark)
      head.position.set(0, 4.3, 0.2)
      group.add(head)
      const red = new THREE.Mesh(
        new THREE.SphereGeometry(0.08, 8, 8),
        new THREE.MeshStandardMaterial({ color: 0xff2211, emissive: 0xff2211, emissiveIntensity: 1.4 }),
      )
      const yellow = new THREE.Mesh(
        new THREE.SphereGeometry(0.08, 8, 8),
        new THREE.MeshStandardMaterial({ color: 0x442200, emissive: 0xffaa00, emissiveIntensity: 0.1 }),
      )
      const green = new THREE.Mesh(
        new THREE.SphereGeometry(0.08, 8, 8),
        new THREE.MeshStandardMaterial({ color: 0x113308, emissive: 0x33ff66, emissiveIntensity: 0.1 }),
      )
      red.position.set(0, 4.55, 0.32)
      yellow.position.set(0, 4.3, 0.32)
      green.position.set(0, 4.05, 0.32)
      group.add(red, yellow, green)
      group.position.set(p.x + CFG.road * 0.42, 0, p.z + CFG.road * 0.42)
      scene.add(group)
      store.push({
        i, j, group, red, yellow, green,
        ns: (i + j) % 2 === 0,
      })
    }
  }
}

function scatterStreetProps(scene, n, mats) {
  const binGeo = new THREE.CylinderGeometry(0.22, 0.26, 0.7, 8)
  const hydrantGeo = new THREE.CylinderGeometry(0.14, 0.16, 0.7, 8)
  const hydrantMat = new THREE.MeshStandardMaterial({ color: 0xb22222, roughness: 0.45 })
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < n; j++) {
      const o = blockOrigin(i, j)
      const bin = new THREE.Mesh(binGeo, mats.dark)
      bin.position.set(o.x + 3.2, 0.4, o.z + 1.1)
      bin.castShadow = true
      scene.add(bin)
      if ((i + j) % 2 === 0) {
        const h = new THREE.Mesh(hydrantGeo, hydrantMat)
        h.position.set(o.x + CFG.blockSize - 2.4, 0.4, o.z + 1.05)
        h.castShadow = true
        scene.add(h)
      }
    }
  }
}
