import * as THREE from 'three'

function canvas(size) {
  const c = document.createElement('canvas')
  c.width = c.height = size
  const ctx = c.getContext('2d')
  return { c, ctx }
}

function noise(ctx, size, alpha = 20) {
  const img = ctx.getImageData(0, 0, size, size)
  const d = img.data
  for (let i = 0; i < d.length; i += 4) {
    const n = (Math.random() - 0.5) * alpha
    d[i] = Math.max(0, Math.min(255, d[i] + n))
    d[i + 1] = Math.max(0, Math.min(255, d[i + 1] + n))
    d[i + 2] = Math.max(0, Math.min(255, d[i + 2] + n))
  }
  ctx.putImageData(img, 0, 0)
}

export function makeGrass() {
  const size = 512
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#3a5a32'
  ctx.fillRect(0, 0, size, size)
  for (let i = 0; i < 8000; i++) {
    const g = 70 + Math.random() * 70
    ctx.fillStyle = `rgb(${40 + Math.random() * 30},${g},${30 + Math.random() * 25})`
    ctx.fillRect(Math.random() * size, Math.random() * size, 2, 3 + Math.random() * 4)
  }
  noise(ctx, size, 18)
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.repeat.set(40, 40)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export function makeAsphalt() {
  const size = 512
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#2a2c30'
  ctx.fillRect(0, 0, size, size)
  noise(ctx, size, 28)
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export function makeSidewalk() {
  const size = 256
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#8a8680'
  ctx.fillRect(0, 0, size, size)
  ctx.strokeStyle = 'rgba(0,0,0,0.15)'
  ctx.lineWidth = 2
  for (let i = 0; i <= 4; i++) {
    ctx.beginPath()
    ctx.moveTo(i * 64, 0)
    ctx.lineTo(i * 64, size)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(0, i * 64)
    ctx.lineTo(size, i * 64)
    ctx.stroke()
  }
  noise(ctx, size, 16)
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.repeat.set(2, 8)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export function makeFacade({ base, accent, cols, rows, litChance = 0.35 }) {
  const w = cols * 16
  const h = rows * 24
  const { c, ctx } = canvasMath(w, h)
  ctx.fillStyle = base
  ctx.fillRect(0, 0, w, h)
  const padX = 4
  const padY = 6
  const ww = 8
  const wh = 12
  for (let y = 0; y < rows; y++) {
    for (let x = 0; x < cols; x++) {
      const lit = Math.random() < litChance
      ctx.fillStyle = lit ? accent : '#1a1d24'
      const px = x * 16 + padX
      const py = y * 24 + padY
      ctx.fillRect(px, py, ww, wh)
      if (lit) {
        ctx.fillStyle = 'rgba(255, 230, 160, 0.35)'
        ctx.fillRect(px, py, ww, 3)
      }
    }
  }
  const map = new THREE.CanvasTexture(c)
  map.colorSpace = THREE.SRGBColorSpace
  map.wrapS = map.wrapT = THREE.RepeatWrapping

  const { c: ce, ctx: xe } = canvasMath(w, h)
  xe.fillStyle = '#000'
  xe.fillRect(0, 0, w, h)
  for (let y = 0; y < rows; y++) {
    for (let x = 0; x < cols; x++) {
      const lit = Math.random() < litChance
      if (!lit) continue
      xe.fillStyle = accent
      xe.fillRect(x * 16 + padX, y * 24 + padY, ww, wh)
    }
  }
  const emissive = new THREE.CanvasTexture(ce)
  emissive.wrapS = emissive.wrapT = THREE.RepeatWrapping
  return { map, emissive, cols, rows }
}

function canvasMath(w, h) {
  const c = document.createElement('canvas')
  c.width = w
  c.height = h
  return { c, ctx: c.getContext('2d') }
}

export function makeBrick() {
  const size = 256
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#6a3d32'
  ctx.fillRect(0, 0, size, size)
  const bh = 14
  const bw = 28
  for (let y = 0, row = 0; y < size; y += bh, row++) {
    const off = row % 2 ? bw / 2 : 0
    for (let x = -bw; x < size; x += bw) {
      const shade = 90 + Math.random() * 50
      ctx.fillStyle = `rgb(${shade + 20},${shade * 0.45},${shade * 0.35})`
      ctx.fillRect(x + off + 1, y + 1, bw - 2, bh - 2)
    }
  }
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export function makeRoof() {
  const size = 128
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#4a2c28'
  ctx.fillRect(0, 0, size, size)
  ctx.strokeStyle = 'rgba(0,0,0,0.25)'
  for (let y = 0; y < size; y += 8) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(size, y)
    ctx.stroke()
  }
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export function makeBark() {
  const size = 128
  const { c, ctx } = canvas(size)
  ctx.fillStyle = '#4a3428'
  ctx.fillRect(0, 0, size, size)
  noise(ctx, size, 30)
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}
