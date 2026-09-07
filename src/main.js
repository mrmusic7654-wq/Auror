import * as THREE from 'three'
import { Sky } from 'three/addons/objects/Sky.js'
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js'
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js'
import { CFG } from './config.js'
import { createCity } from './city.js'
import { createLife } from './life.js'
import { createPlayer } from './player.js'

const canvas = document.getElementById('c')
const loader = document.getElementById('loader')
const bar = document.getElementById('bar-fill')
const loadMsg = document.getElementById('load-msg')

function progress(p, msg) {
  bar.style.width = `${p}%`
  if (msg) loadMsg.textContent = msg
}

progress(8, 'Laying streets')

const mobile = window.matchMedia('(pointer: coarse)').matches || window.innerWidth < 900
const renderer = new THREE.WebGLRenderer({ canvas, antialias: !mobile, powerPreference: 'high-performance' })
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, mobile ? 1.35 : 1.75))
renderer.setSize(window.innerWidth, window.innerHeight)
renderer.shadowMap.enabled = true
renderer.shadowMap.type = THREE.PCFSoftShadowMap
renderer.toneMapping = THREE.ACESFilmicToneMapping
renderer.toneMappingExposure = 1.05
renderer.outputColorSpace = THREE.SRGBColorSpace

const scene = new THREE.Scene()
scene.background = new THREE.Color(0x87a9c4)
scene.fog = new THREE.Fog(0xb7c9d8, 80, 320)

const camera = new THREE.PerspectiveCamera(62, window.innerWidth / window.innerHeight, 0.1, 2000)

const hemi = new THREE.HemisphereLight(0xbcd6ff, 0x3d4a32, 0.55)
scene.add(hemi)
const sunLight = new THREE.DirectionalLight(0xfff4d6, 1.6)
sunLight.castShadow = true
sunLight.shadow.mapSize.set(mobile ? 1024 : 2048, mobile ? 1024 : 2048)
sunLight.shadow.camera.near = 1
sunLight.shadow.camera.far = 420
const s = 160
sunLight.shadow.camera.left = -s
sunLight.shadow.camera.right = s
sunLight.shadow.camera.top = s
sunLight.shadow.camera.bottom = -s
sunLight.shadow.bias = -0.0004
scene.add(sunLight)
scene.add(sunLight.target)

const sky = new Sky()
sky.scale.setScalar(450000)
scene.add(sky)
const skyU = sky.material.uniforms
skyU.turbidity.value = 6
skyU.rayleigh.value = 1.4
skyU.mieCoefficient.value = 0.005
skyU.mieDirectionalG.value = 0.75
const sunVec = new THREE.Vector3()

progress(28, 'Raising buildings')
const city = createCity(scene)

progress(62, 'Waking the town')
const life = createLife(scene, city)
const player = createPlayer(camera, canvas)

progress(80, 'Lighting the sky')

const composer = new EffectComposer(renderer)
const bloomPass = new UnrealBloomPass(new THREE.Vector2(window.innerWidth, window.innerHeight), 0.22, 0.5, 0.85)
composer.addPass(new RenderPass(scene, camera))
composer.addPass(bloomPass)
composer.addPass(new OutputPass())

const clockEl = document.getElementById('clock')
const peopleEl = document.getElementById('people-count')
const carEl = document.getElementById('car-count')
const weatherEl = document.getElementById('weather')
const hourSlider = document.getElementById('hour')
const speedSlider = document.getElementById('time-speed')
const speedLabel = document.getElementById('speed-label')
const toast = document.getElementById('toast')
const hud = document.getElementById('hud')
const panel = document.getElementById('panel')
const crosshair = document.getElementById('crosshair')
const joystick = document.getElementById('joystick')
const gestureHint = document.getElementById('gesture-hint')
const lookHint = document.getElementById('look-hint')
const joyLabel = document.getElementById('joy-label')

peopleEl.textContent = String(CFG.people)
carEl.textContent = String(CFG.cars + 1)

let hour = 8.5
let timeScale = 8
let autoTime = true
hourSlider.addEventListener('input', () => {
  hour = Number(hourSlider.value)
  autoTime = false
})
speedSlider.addEventListener('input', () => {
  timeScale = Number(speedSlider.value)
  speedLabel.textContent = timeScale === 0 ? 'paused' : `x${timeScale}`
  autoTime = true
})

const modes = document.getElementById('modes')
modes.addEventListener('click', (e) => {
  const btn = e.target.closest('button')
  if (!btn) return
  setMode(btn.dataset.mode)
})

function setMode(mode) {
  player.setMode(mode)
  for (const b of modes.querySelectorAll('button')) {
    b.classList.toggle('active', b.dataset.mode === mode)
  }
  const exploring = mode === 'cinematic'
  crosshair.classList.toggle('hidden', mode !== 'walk')
  joystick.classList.toggle('hidden', exploring)
  gestureHint.classList.toggle('hidden', !exploring)
  lookHint.classList.toggle('hidden', exploring)
  if (joyLabel) joyLabel.textContent = mode === 'drive' ? 'Steer' : 'Move'
  const msg = exploring
    ? 'Drag to orbit · pinch to zoom'
    : mode === 'walk'
      ? 'Left stick moves · drag screen to look'
      : 'Left stick drives · pinch to zoom camera'
  showToast(msg)
  try { navigator.vibrate?.(12) } catch { /* ignore */ }
}

document.getElementById('reset-view')?.addEventListener('click', () => {
  player.resetView()
  setMode('cinematic')
  showToast('View reset')
})

window.addEventListener('keydown', (e) => {
  if (e.code === 'KeyC') {
    const order = ['cinematic', 'walk', 'drive']
    const i = order.indexOf(player.state.mode)
    setMode(order[(i + 1) % order.length])
  }
  if (e.code === 'KeyH') {
    hud.classList.toggle('hidden')
    panel.classList.toggle('hidden')
  }
})

function showToast(msg) {
  toast.textContent = msg
  toast.classList.add('show')
  clearTimeout(showToast._t)
  showToast._t = setTimeout(() => toast.classList.remove('show'), 2200)
}

function applySky(h) {
  const day = h % 24
  const elev = Math.sin(((day - 6) / 12) * Math.PI) * 62
  const azimuth = 180 + (day / 24) * 40
  const phi = THREE.MathUtils.degToRad(90 - Math.max(elev, -8))
  const theta = THREE.MathUtils.degToRad(azimuth)
  sunVec.setFromSphericalCoords(1, phi, theta)
  skyU.sunPosition.value.copy(sunVec)
  sunLight.position.copy(sunVec).multiplyScalar(180)
  sunLight.target.position.set(0, 0, 0)

  const night = day < 5.8 || day > 19.6
  const dusk = !night && (day < 7.4 || day > 17.8)
  const elev01 = THREE.MathUtils.clamp((elev + 8) / 70, 0, 1)

  sunLight.intensity = night ? 0.08 : 0.35 + elev01 * 1.5
  sunLight.color.set(dusk ? 0xffb070 : night ? 0x8899cc : 0xfff3d0)
  hemi.intensity = night ? 0.12 : 0.35 + elev01 * 0.35
  hemi.color.set(night ? 0x1a2440 : 0xc5dbff)
  renderer.toneMappingExposure = night ? 0.72 : dusk ? 0.95 : 1.08

  const fogCol = night ? 0x0b1020 : dusk ? 0xc48a62 : 0xb7c9d8
  scene.fog.color.set(fogCol)
  scene.background.set(fogCol)
  scene.fog.near = night ? 40 : 90
  scene.fog.far = night ? 220 : 340

  const emit = night ? 1.35 : dusk ? 0.45 : 0.12
  for (const m of city.nightMaterials) m.emissiveIntensity = emit
  city.lamps.glowMat.emissiveIntensity = night ? 2.4 : 0.15
  bloomPass.strength = night ? 0.55 : dusk ? 0.28 : 0.14

  const hh = Math.floor(day)
  const mm = Math.floor((day - hh) * 60)
  clockEl.textContent = `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
  weatherEl.textContent = night ? 'Clear night' : dusk ? 'Golden hour' : 'Fair'
  if (document.activeElement !== hourSlider) hourSlider.value = String(day)
}

const clock = new THREE.Clock()

function onResize() {
  const w = window.innerWidth
  const h = window.innerHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  composer.setSize(w, h)
  bloomPass.setSize(w, h)
}
window.addEventListener('resize', onResize)

progress(100, 'Open streets')
requestAnimationFrame(() => {
  hud.classList.remove('hidden')
  panel.classList.remove('hidden')
  loader.classList.add('gone')
  showToast('Drag the town with your finger')
  setMode('cinematic')
})

function tick() {
  const dt = Math.min(clock.getDelta(), 0.05)
  if (autoTime) hour = (hour + dt * timeScale / 60) % 24
  player.state.hour = hour
  applySky(hour)
  player.update(dt, city)
  life.update(dt, hour, city, player.state)
  composer.render()
  requestAnimationFrame(tick)
}
tick()
