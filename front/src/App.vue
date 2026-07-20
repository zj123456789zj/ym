<template>
  <div class="app">
    <canvas ref="rainCanvas" class="rain-canvas"></canvas>
    <router-view />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const rainCanvas = ref(null)
let animId = null

function startRain() {
  const canvas = rainCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let W, H

  function resize() {
    W = canvas.width = window.innerWidth
    H = canvas.height = window.innerHeight
  }
  window.addEventListener('resize', resize)
  resize()

  const COUNT = 80
  const drops = []
  for (let i = 0; i < COUNT; i++) {
    drops.push({
      x: Math.random() * W,
      y: Math.random() * H * -1,
      length: 14 + Math.random() * 20,
      speed: 3 + Math.random() * 4,
      opacity: 0.3 + Math.random() * 0.4,
      width: 1.2 + Math.random() * 1.0
    })
  }

  function draw() {
    ctx.clearRect(0, 0, W, H)
    for (const d of drops) {
      d.y += d.speed
      d.x += 0.3
      if (d.y > H + 20) { d.y = -d.length - 20; d.x = Math.random() * W }
      if (d.x > W + 20) { d.x = -10 }
      ctx.beginPath()
      ctx.moveTo(d.x, d.y)
      ctx.lineTo(d.x - 1.2, d.y - d.length)
      ctx.strokeStyle = `rgba(14,165,233,${d.opacity})`
      ctx.lineWidth = d.width
      ctx.stroke()
    }
    animId = requestAnimationFrame(draw)
  }
  draw()
}

function stopRain() {
  if (animId) cancelAnimationFrame(animId)
}

onMounted(() => startRain())
onUnmounted(() => stopRain())
</script>

<style>
.rain-canvas {
  position: fixed; top: 0; left: 0;
  width: 100vw; height: 100vh;
  pointer-events: none; z-index: 0;
  opacity: 0.7;
}
.app { position: relative; z-index: 1; }
</style>
