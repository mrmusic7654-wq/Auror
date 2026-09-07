import * as THREE from 'three'
import { collides } from './life.js'
import { worldSpan } from './config.js'

export function createPlayer(camera, canvas) {
  const input = {
    forward: false, back: false, left: false, right: false, sprint: false, jump: false,
  }
  const state = {
    mode: 'cinematic',
    pos: new THREE.Vector3(0, 1.7, 42),
    velY: 0,
    yaw: 0,
    pitch: -0.18,
    fly: false,
    hour: 8.5,
    carPos: new THREE.Vector3(),
    carYaw: 0,
    input,
    showParkedCar: true,
  }

  const cinematic = {
    theta: 0.4,
    phi: 0.72,
    radius: 86,
    look: new THREE.Vector3(0, 8, 0),
  }

  const keys = {
    KeyW: 'forward', ArrowUp: 'forward',
    KeyS: 'back', ArrowDown: 'back',
    KeyA: 'left', ArrowLeft: 'left',
    KeyD: 'right', ArrowRight: 'right',
  }

  window.addEventListener('keydown', (e) => {
    if (keys[e.code]) input[keys[e.code]] = true
    if (e.code === 'ShiftLeft' || e.code === 'ShiftRight') input.sprint = true
    if (e.code === 'Space') input.jump = true
    if (e.code === 'KeyF') state.fly = !state.fly
  })
  window.addEventListener('keyup', (e) => {
    if (keys[e.code]) input[keys[e.code]] = false
    if (e.code === 'ShiftLeft' || e.code === 'ShiftRight') input.sprint = false
    if (e.code === 'Space') input.jump = false
  })

  let dragging = false
  canvas.addEventListener('click', () => {
    if (state.mode !== 'cinematic') canvas.requestPointerLock?.()
  })
  canvas.addEventListener('mousedown', () => { dragging = true })
  window.addEventListener('mouseup', () => { dragging = false })
  window.addEventListener('mousemove', (e) => {
    const locked = document.pointerLockElement === canvas
    if (!locked && !(dragging && state.mode !== 'cinematic')) return
    state.yaw -= e.movementX * 0.0022
    state.pitch -= e.movementY * 0.0022
    state.pitch = THREE.MathUtils.clamp(state.pitch, -1.2, 1.2)
  })

  function setMode(mode) {
    state.mode = mode
    if (mode === 'cinematic') document.exitPointerLock?.()
    if (mode === 'walk') {
      state.pos.set(6, 1.7, 28)
      state.yaw = Math.PI
      state.fly = false
    }
  }

  function update(dt, city) {
    if (state.mode === 'cinematic') {
      cinematic.theta += dt * 0.08
      const x = Math.sin(cinematic.theta) * cinematic.radius
      const z = Math.cos(cinematic.theta) * cinematic.radius
      const y = 28 + Math.sin(cinematic.theta * 0.6) * 8
      camera.position.set(x, y, z)
      camera.lookAt(cinematic.look)
      return
    }

    if (state.mode === 'drive') {
      const behind = 7.2
      const height = 2.6
      camera.position.set(
        state.carPos.x - Math.sin(state.carYaw) * behind,
        state.carPos.y + height,
        state.carPos.z - Math.cos(state.carYaw) * behind,
      )
      camera.lookAt(state.carPos.x, state.carPos.y + 1.2, state.carPos.z)
      return
    }

    const speed = (state.fly ? 22 : 6.2) * (input.sprint ? 2.1 : 1)
    const fwd = new THREE.Vector3(Math.sin(state.yaw), 0, Math.cos(state.yaw))
    const right = new THREE.Vector3(Math.cos(state.yaw), 0, -Math.sin(state.yaw))
    const move = new THREE.Vector3()
    if (input.forward) move.add(fwd)
    if (input.back) move.sub(fwd)
    if (input.right) move.add(right)
    if (input.left) move.sub(right)
    if (move.lengthSq() > 0) move.normalize().multiplyScalar(speed * dt)

    let nx = state.pos.x + move.x
    let nz = state.pos.z + move.z
    if (!collides(nx, state.pos.z, 0.45, city.colliders)) state.pos.x = nx
    if (!collides(state.pos.x, nz, 0.45, city.colliders)) state.pos.z = nz

    const half = worldSpan() / 2 - 2
    state.pos.x = THREE.MathUtils.clamp(state.pos.x, -half, half)
    state.pos.z = THREE.MathUtils.clamp(state.pos.z, -half, half)

    if (state.fly) {
      if (input.jump) state.pos.y += speed * dt
      if (input.sprint && !move.lengthSq()) { /* hold shift still sprints on ground */ }
      if (input.back && input.jump) { /* noop */ }
    } else {
      state.velY -= 22 * dt
      if (state.pos.y <= 1.7) {
        state.pos.y = 1.7
        state.velY = 0
        if (input.jump) state.velY = 7
      }
      state.pos.y += state.velY * dt
    }
    if (state.fly && input.jump) {
      /* already handled */
    }
    if (state.fly) {
      state.pos.y += ((input.jump ? 1 : 0) - (input.sprint && !input.forward && !input.back ? 0 : 0)) * 0
    }

    camera.position.copy(state.pos)
    if (state.fly) {
      if (input.jump) camera.position.y = state.pos.y
    }
    const look = new THREE.Vector3(
      state.pos.x + Math.sin(state.yaw) * Math.cos(state.pitch),
      state.pos.y + Math.sin(state.pitch),
      state.pos.z + Math.cos(state.yaw) * Math.cos(state.pitch),
    )
    camera.lookAt(look)
  }

  return { state, setMode, update, input, cinematic }
}
