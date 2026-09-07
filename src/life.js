import * as THREE from 'three'
import { CFG, intersectionPos, worldSpan } from './config.js'

const CAR_COLORS = [
  0xc0392b, 0x2980b9, 0x27ae60, 0xf1c40f, 0x8e44ad, 0xecf0f1,
  0x2c3e50, 0xe67e22, 0x1abc9c, 0x7f8c8d, 0x34495e, 0xd35400,
]
const SKIN = [0xf1c27d, 0xc68642, 0x8d5524, 0xffe0bd, 0x5c3317]
const SHIRTS = [0x2c3e50, 0x3498db, 0xe74c3c, 0x2ecc71, 0xf39c12, 0x9b59b6, 0xecf0f1, 0x1abc9c]
const PANTS = [0x2c3e50, 0x34495e, 0x5d4e37, 0x1a1a1a, 0x4a6fa5]

export function createLife(scene, city) {
  const cars = spawnCars(scene, city)
  const people = spawnPeople(scene, city)
  const birds = spawnBirds(scene)
  const fountain = spawnFountain(scene, city)
  const playerCar = spawnPlayerCar(scene)

  return {
    cars,
    people,
    birds,
    fountain,
    playerCar,
    update(dt, hour, cityRef, player) {
      updateLights(cityRef, hour)
      updateCars(cars, dt, cityRef, player)
      updatePeople(people, dt, hour)
      updateBirds(birds, dt)
      updateFountain(fountain, dt)
      updatePlayerCar(playerCar, dt, player, cityRef)
    },
  }
}

function dirs() {
  return [
    { x: 1, z: 0 },
    { x: -1, z: 0 },
    { x: 0, z: 1 },
    { x: 0, z: -1 },
  ]
}

function lanePoint(i, j, dir) {
  const p = intersectionPos(i, j)
  const off = CFG.lane * 0.55
  // right-hand traffic offset
  return {
    x: p.x + dir.z * off,
    z: p.z - dir.x * off,
    i, j, dir,
  }
}

function spawnCars(scene, city) {
  const n = city.n
  const cars = []
  for (let k = 0; k < CFG.cars; k++) {
    let i = Math.floor(Math.random() * (n + 1))
    let j = Math.floor(Math.random() * (n + 1))
    let dir = dirs()[Math.floor(Math.random() * 4)]
    if (!nextIndex(i, j, dir, n)) dir = pickNewDir(i, j, dir, n)
    const kind = Math.random() < 0.08 ? 'bus' : Math.random() < 0.2 ? 'van' : 'sedan'
    const mesh = makeVehicle(kind, CAR_COLORS[k % CAR_COLORS.length])
    const wp = lanePoint(i, j, dir)
    mesh.position.set(wp.x, 0, wp.z)
    mesh.rotation.y = Math.atan2(dir.x, dir.z)
    const dest = nextIndex(i, j, dir, n) || { i, j }
    scene.add(mesh)
    cars.push({
      mesh,
      i: dest.i,
      j: dest.j,
      dir,
      speed: kind === 'bus' ? 7.5 : 10 + Math.random() * 4,
      v: 0,
      kind,
      wait: 0,
      brake: mesh.userData.brake,
      head: mesh.userData.head,
    })
  }
  return cars
}

function makeVehicle(kind, color) {
  const g = new THREE.Group()
  const bodyMat = new THREE.MeshStandardMaterial({ color, roughness: 0.35, metalness: 0.45 })
  const dark = new THREE.MeshStandardMaterial({ color: 0x111111, roughness: 0.4 })
  const glass = new THREE.MeshStandardMaterial({
    color: 0x89c4e8, roughness: 0.15, metalness: 0.3, transparent: true, opacity: 0.55,
  })
  const wheelMat = new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.8 })

  let len = 4.4, wid = 1.8, hei = 1.15
  if (kind === 'van') { len = 5.2; hei = 1.7 }
  if (kind === 'bus') { len = 8.4; wid = 2.2; hei = 2.4 }

  const body = new THREE.Mesh(new THREE.BoxGeometry(wid, hei, len), bodyMat)
  body.position.y = 0.55 + (kind === 'bus' ? 0.4 : 0)
  body.castShadow = true
  g.add(body)

  const cabin = new THREE.Mesh(
    new THREE.BoxGeometry(wid * 0.92, kind === 'bus' ? hei * 0.5 : 0.7, kind === 'sedan' ? len * 0.5 : len * 0.7),
    glass,
  )
  cabin.position.y = body.position.y + hei * 0.45
  cabin.position.z = kind === 'sedan' ? -0.2 : 0.2
  cabin.castShadow = true
  g.add(cabin)

  const wheels = []
  const wgeo = new THREE.CylinderGeometry(0.32, 0.32, 0.22, 10)
  wgeo.rotateZ(Math.PI / 2)
  const zOff = kind === 'bus' ? 3.1 : kind === 'van' ? 1.8 : 1.4
  const spots = [
    [wid / 2 + 0.02, 0.32, zOff],
    [-wid / 2 - 0.02, 0.32, zOff],
    [wid / 2 + 0.02, 0.32, -zOff],
    [-wid / 2 - 0.02, 0.32, -zOff],
  ]
  if (kind === 'bus') {
    spots.push([wid / 2 + 0.02, 0.32, 0], [-wid / 2 - 0.02, 0.32, 0])
  }
  for (const [x, y, z] of spots) {
    const w = new THREE.Mesh(wgeo, wheelMat)
    w.position.set(x, y, z)
    w.castShadow = true
    g.add(w)
    wheels.push(w)
  }

  const headMat = new THREE.MeshStandardMaterial({ color: 0xfff2c4, emissive: 0xffe08a, emissiveIntensity: 0.2 })
  const brakeMat = new THREE.MeshStandardMaterial({ color: 0x550000, emissive: 0xff2200, emissiveIntensity: 0.2 })
  const hl = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.14, 0.08), headMat)
  const hr = hl.clone()
  hl.position.set(-0.55, 0.55, len / 2 + 0.02)
  hr.position.set(0.55, 0.55, len / 2 + 0.02)
  const bl = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.12, 0.08), brakeMat)
  const br = bl.clone()
  bl.position.set(-0.55, 0.55, -len / 2 - 0.02)
  br.position.set(0.55, 0.55, -len / 2 - 0.02)
  g.add(hl, hr, bl, br)

  g.userData = { wheels, head: headMat, brake: brakeMat, length: len }
  return g
}

function spawnPlayerCar(scene) {
  const mesh = makeVehicle('sedan', 0xb11226)
  const start = intersectionPos(Math.floor(CFG.blocks / 2), CFG.blocks)
  mesh.position.set(start.x + CFG.lane * 0.55, 0, start.z + 8)
  scene.add(mesh)
  return {
    mesh,
    speed: 0,
    yaw: 0,
    max: 22,
  }
}

function nextIndex(i, j, dir, n) {
  const ni = i + dir.x
  const nj = j + dir.z
  if (ni < 0 || nj < 0 || ni > n || nj > n) return null
  return { i: ni, j: nj }
}

function pickNewDir(i, j, dir, n) {
  const options = dirs().filter((d) => {
    if (d.x === -dir.x && d.z === -dir.z) return false
    return nextIndex(i, j, d, n)
  })
  if (!options.length) {
    return { x: -dir.x, z: -dir.z }
  }
  return options[Math.floor(Math.random() * options.length)]
}

function lightAllows(city, dir) {
  const ns = dir.z !== 0
  if (city.lightPhase === 'ns') return ns
  if (city.lightPhase === 'ew') return !ns
  return true
}

function updateLights(city) {
  const t = performance.now() / 1000
  const u = t % 12
  let phase = 'ns'
  let nsWhich = 'green'
  let ewWhich = 'red'
  if (u < 5) {
    phase = 'ns'; nsWhich = 'green'; ewWhich = 'red'
  } else if (u < 6) {
    phase = 'ns'; nsWhich = 'yellow'; ewWhich = 'red'
  } else if (u < 11) {
    phase = 'ew'; nsWhich = 'red'; ewWhich = 'green'
  } else {
    phase = 'ew'; nsWhich = 'red'; ewWhich = 'yellow'
  }
  city.lightPhase = phase
  for (const L of city.trafficLights) {
    L.phase = phase
    const which = L.ns ? nsWhich : ewWhich
    setBulb(L.green, which === 'green')
    setBulb(L.yellow, which === 'yellow')
    setBulb(L.red, which === 'red')
  }
}

function setBulb(mesh, on) {
  mesh.material.emissiveIntensity = on ? 1.6 : 0.08
}

function updateCars(cars, dt, city, player) {
  const n = city.n
  const night = hourIsNight(player.hour)
  for (const car of cars) {
    car.head.emissiveIntensity = night ? 1.8 : 0.15
    const target = lanePoint(car.i, car.j, car.dir)
    const pos = car.mesh.position
    const dx = target.x - pos.x
    const dz = target.z - pos.z
    const dist = Math.hypot(dx, dz)

    let blocked = false
    if (dist < 6.5 && !lightAllows(city, car.dir)) blocked = true
    for (const other of cars) {
      if (other === car) continue
      const od = pos.distanceTo(other.mesh.position)
      if (od < 7) {
        const to = other.mesh.position.clone().sub(pos)
        const facing = car.dir.x * to.x + car.dir.z * to.z
        if (facing > 0 && od < 6.2) blocked = true
      }
    }
    if (player.mode === 'drive') {
      const pd = pos.distanceTo(player.carPos)
      if (pd < 7) {
        const to = player.carPos.clone().sub(pos)
        if (car.dir.x * to.x + car.dir.z * to.z > 0) blocked = true
      }
    }

    const desired = blocked ? 0 : car.speed
    car.v = THREE.MathUtils.damp(car.v, desired, blocked ? 8 : 2.2, dt)
    car.brake.emissiveIntensity = car.v < 2 ? 1.4 : 0.2

    if (dist < 1.15) {
      if (blocked) {
        car.v = 0
        continue
      }
      car.dir = pickNewDir(car.i, car.j, car.dir, n)
      const nxt = nextIndex(car.i, car.j, car.dir, n)
      if (nxt) {
        car.i = nxt.i
        car.j = nxt.j
      }
    } else {
      const step = Math.min(car.v * dt, dist)
      pos.x += (dx / dist) * step
      pos.z += (dz / dist) * step
      const yaw = Math.atan2(car.dir.x, car.dir.z)
      car.mesh.rotation.y = THREE.MathUtils.lerp(car.mesh.rotation.y, yaw, 1 - Math.pow(0.001, dt))
      for (const w of car.mesh.userData.wheels) w.rotation.x += car.v * dt * 1.6
    }
  }
}

function spawnPeople(scene, city) {
  const people = []
  const loops = city.sidewalkLoops
  for (let k = 0; k < CFG.people; k++) {
    const loop = loops[k % loops.length]
    const mesh = makePerson()
    const t = Math.random()
    const seg = Math.floor(Math.random() * loop.length)
    scene.add(mesh)
    people.push({
      mesh,
      loop,
      seg,
      t,
      speed: 1.1 + Math.random() * 0.7,
      phase: Math.random() * Math.PI * 2,
      pause: 0,
    })
  }
  return people
}

function makePerson() {
  const g = new THREE.Group()
  const skin = new THREE.MeshStandardMaterial({ color: SKIN[Math.floor(Math.random() * SKIN.length)], roughness: 0.7 })
  const shirt = new THREE.MeshStandardMaterial({ color: SHIRTS[Math.floor(Math.random() * SHIRTS.length)], roughness: 0.8 })
  const pants = new THREE.MeshStandardMaterial({ color: PANTS[Math.floor(Math.random() * PANTS.length)], roughness: 0.85 })
  const hair = new THREE.MeshStandardMaterial({ color: [0x1a1a1a, 0x4a3310, 0xc9a227, 0x888888][Math.floor(Math.random() * 4)], roughness: 0.9 })

  const torso = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.55, 0.22), shirt)
  torso.position.y = 1.15
  torso.castShadow = true
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.16, 8, 8), skin)
  head.position.y = 1.58
  head.castShadow = true
  const hairM = new THREE.Mesh(new THREE.SphereGeometry(0.165, 8, 8), hair)
  hairM.position.y = 1.64
  hairM.scale.set(1, 0.6, 1)

  const armL = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.48, 0.1), shirt)
  const armR = armL.clone()
  armL.position.set(-0.26, 1.12, 0)
  armR.position.set(0.26, 1.12, 0)
  const legL = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.62, 0.12), pants)
  const legR = legL.clone()
  legL.position.set(-0.1, 0.42, 0)
  legR.position.set(0.1, 0.42, 0)
  armL.castShadow = armR.castShadow = legL.castShadow = legR.castShadow = true

  g.add(torso, head, hairM, armL, armR, legL, legR)
  g.userData = { armL, armR, legL, legR }
  return g
}

function updatePeople(people, dt) {
  for (const p of people) {
    if (p.pause > 0) {
      p.pause -= dt
      continue
    }
    if (Math.random() < 0.0008) {
      p.pause = 1 + Math.random() * 3
      continue
    }
    p.t += (p.speed * dt) / 12
    if (p.t >= 1) {
      p.t -= 1
      p.seg = (p.seg + 1) % p.loop.length
    }
    const a = p.loop[p.seg]
    const b = p.loop[(p.seg + 1) % p.loop.length]
    p.mesh.position.lerpVectors(a, b, p.t)
    p.mesh.position.y = 0
    const ang = Math.atan2(b.x - a.x, b.z - a.z)
    p.mesh.rotation.y = ang
    const swing = Math.sin(performance.now() / 180 * p.speed + p.phase)
    p.mesh.userData.legL.rotation.x = swing * 0.7
    p.mesh.userData.legR.rotation.x = -swing * 0.7
    p.mesh.userData.armL.rotation.x = -swing * 0.5
    p.mesh.userData.armR.rotation.x = swing * 0.5
  }
}

function spawnBirds(scene) {
  const birds = []
  const mat = new THREE.MeshStandardMaterial({ color: 0x222226, roughness: 0.7 })
  for (let i = 0; i < 24; i++) {
    const m = new THREE.Mesh(new THREE.ConeGeometry(0.18, 0.7, 3), mat)
    m.rotation.x = Math.PI / 2
    scene.add(m)
    birds.push({
      mesh: m,
      r: 40 + Math.random() * 90,
      h: 18 + Math.random() * 28,
      a: Math.random() * Math.PI * 2,
      s: 0.25 + Math.random() * 0.4,
    })
  }
  return birds
}

function updateBirds(birds, dt) {
  for (const b of birds) {
    b.a += b.s * dt
    b.mesh.position.set(Math.cos(b.a) * b.r, b.h + Math.sin(b.a * 3) * 1.4, Math.sin(b.a) * b.r)
    b.mesh.rotation.y = -b.a
  }
}

function spawnFountain(scene, city) {
  const plaza = city.parks.find((p) => p.kind === 'plaza')
  const origin = plaza
    ? new THREE.Vector3(plaza.x + CFG.blockSize / 2, 0.7, plaza.z + CFG.blockSize / 2)
    : new THREE.Vector3(0, 0.7, 0)
  const count = 220
  const geo = new THREE.BufferGeometry()
  const pos = new Float32Array(count * 3)
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  const mat = new THREE.PointsMaterial({
    color: 0xcfefff, size: 0.12, transparent: true, opacity: 0.85,
  })
  const points = new THREE.Points(geo, mat)
  scene.add(points)
  const drops = []
  for (let i = 0; i < count; i++) {
    drops.push({
      a: Math.random() * Math.PI * 2,
      v: 4 + Math.random() * 5,
      life: Math.random(),
      spread: 0.3 + Math.random() * 0.8,
    })
  }
  return { points, drops, origin, pos }
}

function updateFountain(f, dt) {
  const { drops, pos, origin } = f
  for (let i = 0; i < drops.length; i++) {
    const d = drops[i]
    d.life += dt * 0.45
    if (d.life > 1) d.life = 0
    const t = d.life
    const y = origin.y + d.v * t - 9.8 * t * t
    const r = d.spread * t * 3.2
    pos[i * 3] = origin.x + Math.cos(d.a) * r
    pos[i * 3 + 1] = Math.max(0.4, y)
    pos[i * 3 + 2] = origin.z + Math.sin(d.a) * r
  }
  f.points.geometry.attributes.position.needsUpdate = true
}

function hourIsNight(hour) {
  return hour < 6.2 || hour > 19.4
}

function updatePlayerCar(car, dt, player, city) {
  player.carPos.copy(car.mesh.position)
  player.carYaw = car.yaw
  if (player.mode !== 'drive') {
    car.mesh.visible = player.showParkedCar
    return
  }
  car.mesh.visible = true
  const input = player.input
  const accel = (input.forward ? 1 : 0) + (input.back ? -1 : 0)
  car.speed += accel * 18 * dt
  car.speed *= Math.pow(0.22, dt * (input.forward || input.back ? 0.15 : 1))
  car.speed = THREE.MathUtils.clamp(car.speed, -8, car.max)
  const steer = ((input.right ? 1 : 0) + (input.left ? -1 : 0)) * (car.speed >= 0 ? 1 : -1)
  car.yaw -= steer * dt * 1.6 * Math.min(1, Math.abs(car.speed) / 6)
  car.mesh.rotation.y = car.yaw
  const nx = car.mesh.position.x + Math.sin(car.yaw) * car.speed * dt
  const nz = car.mesh.position.z + Math.cos(car.yaw) * car.speed * dt
  if (!collides(nx, nz, 1.4, city.colliders)) {
    car.mesh.position.x = nx
    car.mesh.position.z = nz
  } else {
    car.speed *= -0.2
  }
  const half = worldSpan() / 2 - 4
  car.mesh.position.x = THREE.MathUtils.clamp(car.mesh.position.x, -half, half)
  car.mesh.position.z = THREE.MathUtils.clamp(car.mesh.position.z, -half, half)
  for (const w of car.mesh.userData.wheels) w.rotation.x += car.speed * dt * 1.4
  player.carPos.copy(car.mesh.position)
  player.carYaw = car.yaw
}

export function collides(x, z, radius, colliders) {
  for (const c of colliders) {
    if (x + radius > c.minX && x - radius < c.maxX && z + radius > c.minZ && z - radius < c.maxZ) {
      return true
    }
  }
  return false
}
