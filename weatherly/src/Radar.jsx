import { useEffect, useRef } from 'react';

const mulberry = (a) => () => {
  a |= 0; a = (a + 0x6d2b79f5) | 0;
  let t = Math.imul(a ^ (a >>> 15), 1 | a);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

const STOPS = {
  radar: [[0, 'rgba(20,80,200,0)'], [0.1, 'rgba(30,110,255,.5)'], [0.28, 'rgba(0,190,255,.8)'], [0.45, 'rgba(0,225,140,.92)'], [0.62, 'rgba(190,240,40,.95)'], [0.8, 'rgba(255,170,0,1)'], [1, 'rgba(255,40,40,1)']],
  temperature: [[0, 'rgba(50,120,255,0)'], [0.2, 'rgba(50,140,255,.45)'], [0.5, 'rgba(255,214,90,.6)'], [0.8, 'rgba(255,130,50,.72)'], [1, 'rgba(255,50,40,.82)']],
  clouds: [[0, 'rgba(255,255,255,0)'], [0.25, 'rgba(255,255,255,.16)'], [1, 'rgba(255,255,255,.82)']],
};

function makeLut(mode) {
  const c = document.createElement('canvas');
  c.width = 256; c.height = 1;
  const x = c.getContext('2d');
  const g = x.createLinearGradient(0, 0, 256, 0);
  STOPS[mode].forEach(([p, col]) => g.addColorStop(p, col));
  x.fillStyle = g;
  x.fillRect(0, 0, 256, 1);
  return x.getImageData(0, 0, 256, 1).data;
}

/**
 * Animated "satellite" map. Everything is procedural: moving gaussian
 * cells are summed into a tiny field, colour-mapped and scaled up.
 */
export default function Radar({ mode, reduce }) {
  const ref = useRef(null);

  useEffect(() => {
    const cvs = ref.current;
    const ctx = cvs.getContext('2d');
    const W = 104; const H = 52;
    const field = new Float32Array(W * H);
    const off = document.createElement('canvas');
    off.width = W; off.height = H;
    const octx = off.getContext('2d');
    const img = octx.createImageData(W, H);
    const r = mulberry(11);
    const big = mode !== 'radar';
    const count = big ? 10 : 18;
    const blobs = Array.from({ length: count }, (_, i) => {
      const t = i / count;
      return { x: W * 0.7 * t + r() * 14, y: H * (1 - t * 0.95) + (r() - 0.5) * 8, r: (big ? 9 : 4) + r() * (big ? 10 : 6), a: 0.55 + r() * 0.5, vx: 2.2 + r() * 1.6, vy: -1.1 - r() * 0.8, ph: r() * 6 };
    });
    const lut = mode === 'wind' ? null : makeLut(mode);
    let bg; let raf; let last = 0;
    let dpr = 1;
    const parts = Array.from({ length: 80 }, () => ({ x: r(), y: r(), age: r() * 100 }));

    const buildBg = () => {
      const w = cvs.width; const h = cvs.height;
      bg = document.createElement('canvas');
      bg.width = w; bg.height = h;
      const b = bg.getContext('2d');
      const g = b.createLinearGradient(0, 0, w, h);
      g.addColorStop(0, '#0d1c3a'); g.addColorStop(1, '#07112a');
      b.fillStyle = g; b.fillRect(0, 0, w, h);
      b.strokeStyle = 'rgba(120,160,255,.07)'; b.lineWidth = 1;
      for (let x = 0; x < w; x += w / 8) { b.beginPath(); b.moveTo(x, 0); b.lineTo(x, h); b.stroke(); }
      for (let y = 0; y < h; y += h / 4) { b.beginPath(); b.moveTo(0, y); b.lineTo(w, y); b.stroke(); }
      const rr = mulberry(5);
      b.strokeStyle = 'rgba(130,170,255,.3)'; b.lineWidth = 1.1 * dpr;
      for (let k = 0; k < 14; k++) {
        let x = rr() * w; let y = rr() * h;
        b.beginPath(); b.moveTo(x, y);
        for (let s = 0; s < 9; s++) { x += (rr() - 0.45) * w * 0.12; y += (rr() - 0.5) * h * 0.2; b.lineTo(x, y); }
        b.stroke();
      }
      b.fillStyle = 'rgba(8,28,80,.6)';
      b.beginPath(); b.moveTo(w * 0.55, h); b.quadraticCurveTo(w * 0.7, h * 0.7, w, h * 0.62); b.lineTo(w, h); b.fill();
    };

    const resize = () => {
      const rect = cvs.getBoundingClientRect();
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      cvs.width = Math.max(10, rect.width * dpr);
      cvs.height = Math.max(10, rect.height * dpr);
      buildBg();
    };

    const compute = (t) => {
      field.fill(mode === 'temperature' ? 0.22 : 0);
      for (const b of blobs) {
        const cx = ((b.x + b.vx * t + 26) % (W + 52)) - 26;
        const cy = ((((b.y + b.vy * t) % (H + 40)) + (H + 40)) % (H + 40)) - 20;
        const amp = b.a * (0.78 + 0.22 * Math.sin(t * 0.7 + b.ph));
        const rad = b.r; const R = Math.ceil(rad * 2.4);
        for (let y = Math.max(0, Math.floor(cy - R)); y < Math.min(H, Math.ceil(cy + R)); y++) {
          for (let x = Math.max(0, Math.floor(cx - R)); x < Math.min(W, Math.ceil(cx + R)); x++) {
            const dx = x - cx; const dy = y - cy;
            field[y * W + x] += amp * Math.exp(-(dx * dx + dy * dy) / (rad * rad));
          }
        }
      }
    };

    const paint = (t) => {
      const d = img.data;
      for (let i = 0; i < W * H; i++) {
        let v = field[i];
        if (mode === 'radar') { const x = i % W; const y = (i / W) | 0; v *= 0.82 + 0.18 * Math.sin(x * 0.9 + t) * Math.cos(y * 0.8 - t * 0.7); }
        const k = (Math.min(1, v) * 255) | 0;
        d[i * 4] = lut[k * 4]; d[i * 4 + 1] = lut[k * 4 + 1]; d[i * 4 + 2] = lut[k * 4 + 2]; d[i * 4 + 3] = lut[k * 4 + 3];
      }
      octx.putImageData(img, 0, 0);
      ctx.imageSmoothingEnabled = true;
      ctx.drawImage(off, 0, 0, cvs.width, cvs.height);
    };

    const wind = (t) => {
      const w = cvs.width; const h = cvs.height;
      ctx.strokeStyle = 'rgba(160,225,255,.85)';
      ctx.lineWidth = 1.4 * dpr;
      ctx.lineCap = 'round';
      for (const p of parts) {
        const px = p.x * w; const py = p.y * h;
        const ang = -0.5 + Math.sin(px * 0.006 + t * 0.4) * 0.7 + Math.cos(py * 0.008 - t * 0.3) * 0.4;
        const sp = (1.4 + Math.sin(px * 0.01 + py * 0.01 + t) * 0.6) * dpr;
        const nx = px + Math.cos(ang) * sp; const ny = py + Math.sin(ang) * sp;
        ctx.beginPath(); ctx.moveTo(px, py); ctx.lineTo(nx, ny); ctx.stroke();
        p.x = nx / w; p.y = ny / h; p.age += 1;
        if (p.age > 140 || p.x < 0 || p.x > 1 || p.y < 0 || p.y > 1) { p.x = Math.random(); p.y = Math.random(); p.age = 0; }
      }
    };

    const frame = (ts, loop = true) => {
      if (loop) raf = requestAnimationFrame(frame);
      if (loop && ts - last < 33) return;
      last = ts;
      const t = ts / 1000;
      if (mode === 'wind') {
        ctx.globalAlpha = reduce ? 1 : 0.16;
        ctx.drawImage(bg, 0, 0);
        ctx.globalAlpha = 1;
        wind(t);
      } else {
        ctx.drawImage(bg, 0, 0);
        compute(t);
        paint(t);
      }
    };

    resize();
    const ro = new ResizeObserver(resize);
    ro.observe(cvs);
    if (reduce) { for (let i = 0; i < 40; i++) frame(2000 + i * 33, false); } else { raf = requestAnimationFrame(frame); }
    return () => { cancelAnimationFrame(raf); ro.disconnect(); };
  }, [mode, reduce]);

  return <canvas ref={ref} className="radarCanvas" />;
}
