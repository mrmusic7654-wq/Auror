import * as THREE from 'three'
import { collides } from './life.js'
import { worldSpan } from './config.js'

export function createPlayer(camera, canvas) {
  const input = {
    forward: false, back: false, left: false, right: false, sprint: false, jump: false,
    axisX: 0, axisY: 0,
  }
  const state = {
    mode: 'cinematic',
    pos: new THREE.Vector3(0, 1.7, 42),
    velY: 0,
    yaw: 0.2,
    pitch: -0.12,
    fly: false,
    hour: 8.5,
    carPos: new THREE.Vector3(),
    carYaw: 0,
    input,
    showParkedCar: true,
    camDist: 8.2,
    fov: 62,
  }

  const orbit = {
    theta: 0.55,
    phi: 1.05,
    radius: 72,
    target: new THREE.Vector3(0, 7, 0),
    auto: true,
    thetaVel: 0,
    phiVel: 0,
    zoomVel: 0,
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

  const pointers = new Map()
  let lastPinch = 0
  let pinchMid = null
  let lastTap = 0
  let joyActive = false
  let joyPid = null
  const joyEl = document.getElementById('joystick')
  const knob = joyEl?.querySelector('.stick-knob')

  canvas.style.touchAction = 'none'
  canvas.addEventListener('contextmenu', (e) => e.preventDefault())

  function onJoyStart(e) {
    e.preventDefault()
    e.stopPropagation()
    joyActive = true
    joyPid = e.pointerId
    joyEl.setPointerCapture?.(e.pointerId)
    moveJoy(e)
  }
  function moveJoy(e) {
    if (!joyActive) return
    const base = joyEl.querySelector('.stick-base')
    const r = base.getBoundingClientRect()
    const cx = r.left + r.width / 2
    const cy = r.top + r.height / 2
    let dx = e.clientX - cx
    let dy = e.clientY - cy
    const max = r.width * 0.34
    const len = Math.hypot(dx, dy) || 1
    if (len > max) {
      dx *= max / len
      dy *= max / len
    }
    if (knob) knob.style.transform = `translate(${dx}px, ${dy}px)`
    input.axisX = dx / max
    input.axisY = -dy / max
    input.forward = input.axisY > 0.2
    input.back = input.axisY < -0.2
    input.left = input.axisX < -0.2
    input.right = input.axisX > 0.2
    input.sprint = Math.hypot(input.axisX, input.axisY) > 0.82
  }
  function endJoy(e) {
    if (!joyActive) return
    if (e && e.pointerId != null && joyPid != null && e.pointerId !== joyPid) return
    if (e && e.pointerId != null) joyEl.releasePointerCapture?.(e.pointerId)
    joyActive = false
    joyPid = null
    if (knob) knob.style.transform = 'translate(0,0)'
    input.axisX = input.axisY = 0
    input.forward = input.back = input.left = input.right = input.sprint = false
  }

  if (joyEl) {
    const base = joyEl.querySelector('.stick-base')
    base.addEventListener('pointerdown', onJoyStart)
    window.addEventListener('pointermove', (e) => {
      if (joyActive) {
        e.preventDefault()
        moveJoy(e)
      }
    }, { passive: false })
    window.addEventListener('pointerup', endJoy)
    window.addEventListener('pointercancel', endJoy)
  }

  canvas.addEventListener('pointerdown', (e) => {
    if (e.target.closest?.('#hud, #panel, #joystick')) return
    canvas.setPointerCapture?.(e.pointerId)
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY })
    orbit.auto = false
    if (pointers.size === 1 && state.mode === 'cinematic') {
      const now = performance.now()
      if (now - lastTap < 280) resetView()
      lastTap = now
    }
  })

  canvas.addEventListener('pointermove', (e) => {
    const prev = pointers.get(e.pointerId)
    if (!prev) return
    const dx = e.clientX - prev.x
    const dy = e.clientY - prev.y
    prev.x = e.clientX
    prev.y = e.clientY

    const pts = [...pointers.values()]
    if (pointers.size >= 2) {
      const a = pts[0]
      const b = pts[1]
      const dist = Math.hypot(a.x - b.x, a.y - b.y)
      const mx = (a.x + b.x) * 0.5
      const my = (a.y + b.y) * 0.5
      if (lastPinch) zoomBy(-(dist - lastPinch) * 0.12)
      if (pinchMid) panBy(-(mx - pinchMid.x) * 0.085, (my - pinchMid.y) * 0.085)
      lastPinch = dist
      pinchMid = { x: mx, y: my }
      return
    }

    lastPinch = 0
    pinchMid = null
    lookBy(dx, dy)
  })

  function endPtr(e) {
    pointers.delete(e.pointerId)
    if (pointers.size < 2) {
      lastPinch = 0
      pinchMid = null
    }
  }
  canvas.addEventListener('pointerup', endPtr)
  canvas.addEventListener('pointercancel', endPtr)
  canvas.addEventListener('lostpointercapture', endPtr)

  canvas.addEventListener('wheel', (e) => {
    e.preventDefault()
    zoomBy(e.deltaY * 0.04)
    orbit.auto = false
  }, { passive: false })

  function lookBy(dx, dy) {
    if (state.mode === 'cinematic') {
      orbit.theta -= dx * 0.0055
      orbit.phi -= dy * 0.004
      orbit.phi = THREE.MathUtils.clamp(orbit.phi, 0.18, 1.42)
    } else if (state.mode === 'drive') {
      state.yaw -= dx * 0.004
      state.camDist = THREE.MathUtils.clamp(state.camDist, 5, 16)
    } else {
      state.yaw -= dx * 0.0048
      state.pitch -= dy * 0.004
      state.pitch = THREE.MathUtils.clamp(state.pitch, -1.15, 1.15)
    }
  }

  function zoomBy(amount) {
    if (state.mode === 'cinematic') {
      orbit.radius = THREE.MathUtils.clamp(orbit.radius + amount, 10, 240)
    } else if (state.mode === 'drive') {
      state.camDist = THREE.MathUtils.clamp(state.camDist + amount * 0.25, 5, 18)
    } else {
      state.fov = THREE.MathUtils.clamp(state.fov + amount * 0.35, 42, 80)
      camera.fov = state.fov
      camera.updateProjectionMatrix()
    }
  }

  function panBy(dx, dz) {
    if (state.mode !== 'cinematic') return
    const sin = Math.sin(orbit.theta)
    const cos = Math.cos(orbit.theta)
    orbit.target.x += dx * cos + dz * sin
    orbit.target.z += -dx * sin + dz * cos
    const half = worldSpan() / 2 - 8
    orbit.target.x = THREE.MathUtils.clamp(orbit.target.x, -half, half)
    orbit.target.z = THREE.MathUtils.clamp(orbit.target.z, -half, half)
  }

  function resetView() {
    orbit.theta = 0.55
    orbit.phi = 1.05
    orbit.radius = 72
    orbit.target.set(0, 7, 0)
    orbit.auto = true
    state.fov = 62
    camera.fov = 62
    camera.updateProjectionMatrix()
    state.camDist = 8.2
  }

  function setMode(mode) {
    state.mode = mode
    document.exitPointerLock?.()
    if (mode === 'walk') {
      state.pos.set(4, 1.7, 22)
      state.yaw = Math.PI
      state.pitch = -0.05
      state.fly = false
      orbit.auto = false
    }
    if (mode === 'cinematic') orbit.auto = pointers.size === 0
    if (mode === 'drive') {
      state.camDist = 8.2
    }
  }

  function analog() {
    let x = input.axisX
    let y = input.axisY
    if (Math.abs(x) < 0.04 && Math.abs(y) < 0.04) {
      x = (input.right ? 1 : 0) + (input.left ? -1 : 0)
      y = (input.forward ? 1 : 0) + (input.back ? -1 : 0)
    }
    return { x, y }
  }

  function update(dt, city) {
    input._axisX = analog().x
    input._axisY = analog().y

    if (state.mode === 'cinematic') {
      if (orbit.auto) orbit.theta += dt * 0.07
      const x = orbit.target.x + orbit.radius * Math.sin(orbit.phi) * Math.sin(orbit.theta)
      const y = orbit.target.y + orbit.radius * Math.cos(orbit.phi)
      const z = orbit.target.z + orbit.radius * Math.sin(orbit.phi) * Math.cos(orbit.theta)
      camera.position.set(x, Math.max(2.2, y), z)
      camera.lookAt(orbit.target)
      return
    }

    if (state.mode === 'drive') {
      const behind = state.camDist
      const height = 2.4 + state.camDist * 0.12
      const yaw = state.carYaw
      camera.position.set(
        state.carPos.x - Math.sin(yaw) * behind,
        state.carPos.y + height,
        state.carPos.z - Math.cos(yaw) * behind,
      )
      camera.lookAt(state.carPos.x, state.carPos.y + 1.15, state.carPos.z)
      return
    }

    const { x: ax, y: ay } = analog()
    const speed = (state.fly ? 22 : 5.6) * (input.sprint ? 1.85 : 1)
    const fwd = new THREE.Vector3(Math.sin(state.yaw), 0, Math.cos(state.yaw))
    const right = new THREE.Vector3(Math.cos(state.yaw), 0, -Math.sin(state.yaw))
    const move = new THREE.Vector3()
    move.addScaledVector(fwd, ay)
    move.addScaledVector(right, ax)
    if (move.lengthSq() > 1) move.normalize()
    move.multiplyScalar(speed * dt)

    let nx = state.pos.x + move.x
    let nz = state.pos.z + move.z
    if (!collides(nx, state.pos.z, 0.42, city.colliders)) state.pos.x = nx
    if (!collides(state.pos.x, nz, 0.42, city.colliders)) state.pos.z = nz

    const half = worldSpan() / 2 - 2
    state.pos.x = THREE.MathUtils.clamp(state.pos.x, -half, half)
    state.pos.z = THREE.MathUtils.clamp(state.pos.z, -half, half)

    if (state.fly) {
      if (input.jump) state.pos.y += speed * dt
    } else {
      state.velY -= 22 * dt
      if (state.pos.y <= 1.7) {
        state.pos.y = 1.7
        state.velY = 0
        if (input.jump) state.velY = 7
      }
      state.pos.y += state.velY * dt
    }

    camera.position.copy(state.pos)
    camera.lookAt(
      state.pos.x + Math.sin(state.yaw) * Math.cos(state.pitch),
      state.pos.y + Math.sin(state.pitch),
      state.pos.z + Math.cos(state.yaw) * Math.cos(state.pitch),
    )
  }

  return { state, setMode, update, input, orbit, resetView, analog }
}
