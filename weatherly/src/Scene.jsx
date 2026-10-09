import { useEffect, useMemo, useRef, useState } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';

/* Time-of-day palettes – the sky cross-fades between them */
const PAL = {
  dusk: {
    sky: ['#0a0f2e', '#261a52', '#7c3a63', '#f0753c', '#ffbb66'], sil: '#0a0916', cloudA: '#4b2d5f', cloudB: '#ff9a5c',
    water: ['#c8561f', '#3a1f3f', '#0b0a1c'], sun: '#ffd27a', glow: '#ff8a3a', star: 0.3, win: 0.95,
  },
  night: {
    sky: ['#03050f', '#070d24', '#0e1a3d', '#17285a', '#243a72'], sil: '#02030a', cloudA: '#1a2448', cloudB: '#3a4d8f',
    water: ['#2a3f7a', '#0d1636', '#03050f'], sun: '#e9edff', glow: '#6f86ff', star: 1, win: 1,
  },
  day: {
    sky: ['#1d5fd0', '#3f8fe8', '#74b8f5', '#b4dcfb', '#e3f2ff'], sil: '#16304a', cloudA: '#ffffff', cloudB: '#cfe3f7',
    water: ['#7fb7e8', '#2b5f95', '#10294a'], sun: '#fffbe0', glow: '#fff2a8', star: 0, win: 0.05,
  },
};
const STOPS = [0, 0.3, 0.55, 0.8, 1];
const HZ = 600; // horizon y in the 1536 x 1024 viewBox

const mulberry = (a) => () => {
  a |= 0; a = (a + 0x6d2b79f5) | 0;
  let t = Math.imul(a ^ (a >>> 15), 1 | a);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

const CLOUDS = [
  { y: 150, s: 1.7, d: 190, o: 20 }, { y: 270, s: 1.2, d: 240, o: 120 },
  { y: 80, s: 1.0, d: 300, o: 60 }, { y: 360, s: 2.1, d: 210, o: 170 },
  { y: 210, s: 1.4, d: 260, o: 220 },
];

export default function Scene({ phase, progress }) {
  const pal = PAL[phase];
  const [hasPhoto, setHasPhoto] = useState(false);

  /* Optional: drop your own picture at /public/bg.jpg and it replaces the SVG scene */
  useEffect(() => {
    const img = new Image();
    img.onload = () => setHasPhoto(true);
    img.src = '/bg.jpg';
  }, []);

  /* Mouse parallax */
  const mx = useMotionValue(0);
  useEffect(() => {
    const h = (e) => mx.set(e.clientX / window.innerWidth - 0.5);
    window.addEventListener('pointermove', h);
    return () => window.removeEventListener('pointermove', h);
  }, [mx]);
  const sx = useSpring(mx, { stiffness: 40, damping: 22 });
  const far = useTransform(sx, (v) => v * -10);
  const mid = useTransform(sx, (v) => v * -24);
  const near = useTransform(sx, (v) => v * -40);

  /* Sun follows the real sun */
  const pc = Math.min(1, Math.max(0, progress));
  const sunX = 520 + pc * 560;
  const sunY = HZ - Math.sin(Math.PI * pc) * 440;
  const sunUp = phase !== 'night' && progress > 0 && progress < 1.03;

  const stars = useMemo(() => {
    const r = mulberry(3);
    return Array.from({ length: 80 }, () => ({ x: r() * 1536, y: r() * 440, r: 0.6 + r() * 1.3, d: r() * 5 }));
  }, []);

  const buildings = useMemo(() => {
    const r = mulberry(42);
    const out = [];
    const make = (from, to) => {
      let x = from;
      while (x < to) {
        const w = 26 + r() * 36;
        const h = 50 + r() * (x < 300 || x > 1250 ? 250 : 150);
        const wins = [];
        for (let cx = 5; cx < w - 6; cx += 9) for (let cy = 10; cy < h - 8; cy += 13) if (r() > 0.62) wins.push({ x: cx, y: cy, d: r() * 6, t: 4 + r() * 5 });
        out.push({ x, w, h, wins });
        x += w + 2 + r() * 6;
      }
    };
    make(-20, 470);
    make(1130, 1560);
    return out;
  }, []);

  const shimmer = useMemo(() => {
    const r = mulberry(9);
    return Array.from({ length: 22 }, (_, i) => ({ y: HZ + 14 + i * 17, w: 30 + r() * 120, dx: (r() - 0.5) * 90, d: r() * 4, t: 2.5 + r() * 3 }));
  }, []);

  const cables = useMemo(() => {
    const lines = [];
    const deckY = HZ - 62;
    [{ tx: 690, top: HZ - 330 }, { tx: 960, top: HZ - 300 }].forEach(({ tx, top }) => {
      for (let k = 1; k <= 9; k++) {
        const ay = top + 28 + k * 11;
        lines.push([tx, ay, tx - k * 34, deckY], [tx, ay, tx + k * 34, deckY]);
      }
    });
    return lines;
  }, []);

  if (hasPhoto) {
    return (
      <div className="scene">
        <motion.div className="photo" style={{ backgroundImage: 'url(/bg.jpg)' }} animate={{ scale: [1, 1.06, 1] }} transition={{ duration: 40, repeat: Infinity, ease: 'easeInOut' }} />
      </div>
    );
  }

  return (
    <div className="scene" aria-hidden="true">
      <svg viewBox="0 0 1536 1024" preserveAspectRatio="xMidYMid slice" width="100%" height="100%">
        <defs>
          <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
            {STOPS.map((o, i) => (
              <motion.stop key={i} offset={o} initial={false} animate={{ stopColor: pal.sky[i] }} transition={{ duration: 3 }} />
            ))}
          </linearGradient>
          <linearGradient id="water" x1="0" y1="0" x2="0" y2="1">
            <motion.stop offset="0" initial={false} animate={{ stopColor: pal.water[0] }} transition={{ duration: 3 }} />
            <motion.stop offset="0.35" initial={false} animate={{ stopColor: pal.water[1] }} transition={{ duration: 3 }} />
            <motion.stop offset="1" initial={false} animate={{ stopColor: pal.water[2] }} transition={{ duration: 3 }} />
          </linearGradient>
          <radialGradient id="glow">
            <stop offset="0" stopColor={pal.glow} stopOpacity="0.85" />
            <stop offset="0.35" stopColor={pal.glow} stopOpacity="0.28" />
            <stop offset="1" stopColor={pal.glow} stopOpacity="0" />
          </radialGradient>
          <radialGradient id="core" cx="0.4" cy="0.35">
            <stop offset="0" stopColor="#fff" />
            <stop offset="0.5" stopColor={pal.sun} />
            <stop offset="1" stopColor={pal.glow} />
          </radialGradient>
          <linearGradient id="streak" x1="0" x2="1">
            <stop offset="0" stopColor={pal.glow} stopOpacity="0" />
            <stop offset="0.5" stopColor={pal.sun} stopOpacity="0.8" />
            <stop offset="1" stopColor={pal.glow} stopOpacity="0" />
          </linearGradient>
          <filter id="blur" x="-30%" y="-60%" width="160%" height="220%"><feGaussianBlur stdDeviation="16" /></filter>
        </defs>

        <rect width="1536" height="1024" fill="url(#sky)" />

        {/* stars */}
        <motion.g initial={false} animate={{ opacity: pal.star }} transition={{ duration: 3 }}>
          {stars.map((s, i) => (
            <circle key={i} className="star" cx={s.x} cy={s.y} r={s.r} fill="#fff" style={{ animationDelay: `${s.d}s` }} />
          ))}
        </motion.g>

        {/* sun */}
        <motion.g initial={false} animate={{ opacity: sunUp ? 1 : 0 }} transition={{ duration: 2 }}>
          <motion.circle initial={false} animate={{ cx: sunX, cy: sunY }} transition={{ duration: 2, ease: 'easeInOut' }} r="420" fill="url(#glow)" className="pulse" />
          <motion.circle initial={false} animate={{ cx: sunX, cy: sunY }} transition={{ duration: 2, ease: 'easeInOut' }} r="38" fill="url(#core)" />
        </motion.g>

        {/* moon */}
        <motion.g initial={false} animate={{ opacity: phase === 'night' ? 1 : 0 }} transition={{ duration: 3 }}>
          <circle cx="1130" cy="190" r="240" fill="url(#glow)" opacity="0.5" className="pulse" />
          <path transform="translate(1086 140) scale(1.05)" d="M62 10a42 42 0 1 0 28 62A34 34 0 0 1 62 10z" fill="#eef1ff" />
        </motion.g>

        {/* drifting clouds */}
        <motion.g style={{ x: far }}>
          {CLOUDS.map((c, i) => (
            <g key={i} className="drift" style={{ animationDuration: `${c.d}s`, animationDelay: `-${c.o}s` }}>
              <g transform={`translate(0 ${c.y}) scale(${c.s})`} filter="url(#blur)">
                <ellipse cx="0" cy="0" rx="160" ry="34" fill={pal.cloudA} opacity="0.92" />
                <ellipse cx="-70" cy="-22" rx="90" ry="34" fill={pal.cloudA} opacity="0.92" />
                <ellipse cx="40" cy="-34" rx="110" ry="40" fill={pal.cloudA} opacity="0.92" />
                <ellipse cx="110" cy="-10" rx="80" ry="28" fill={pal.cloudA} opacity="0.92" />
                <ellipse cx="0" cy="22" rx="150" ry="12" fill={pal.cloudB} opacity="0.75" />
              </g>
            </g>
          ))}
        </motion.g>

        {/* birds */}
        {phase !== 'night' && [0, 1, 2].map((i) => (
          <g key={i} className="bird" style={{ animationDelay: `-${i * 7}s`, animationDuration: `${46 + i * 6}s` }}>
            <path d={`M0 ${i * 14} q8 -9 16 0 q8 -9 16 0`} stroke={pal.sil} strokeWidth="2" fill="none" strokeLinecap="round" />
          </g>
        ))}

        {/* skyline + bridge */}
        <motion.g style={{ x: mid }}>
          <motion.g initial={false} animate={{ fill: pal.sil }} transition={{ duration: 3 }}>
            {buildings.map((b, i) => (
              <g key={i} transform={`translate(${b.x} ${HZ - b.h})`}>
                <rect width={b.w} height={b.h + 4} />
                {b.wins.map((w, j) => (
                  <rect key={j} className="win" x={w.x} y={w.y} width="4" height="6" fill="#ffd58a" opacity={pal.win} style={{ animationDelay: `${w.d}s`, animationDuration: `${w.t}s` }} />
                ))}
              </g>
            ))}
            {/* bridge */}
            <rect x="380" y={HZ - 66} width="790" height="9" />
            <rect x="380" y={HZ - 57} width="790" height="57" opacity="0.0" />
            {[690, 960].map((tx, i) => (
              <g key={tx}>
                <rect x={tx - 7} y={HZ - (i ? 300 : 330)} width="14" height={i ? 300 : 330} />
                <circle className="beacon" cx={tx} cy={HZ - (i ? 304 : 334)} r="4" fill="#ff4d4d" />
              </g>
            ))}
            {[420, 530, 800, 1080, 1130].map((px) => <rect key={px} x={px} y={HZ - 57} width="10" height="62" />)}
            {cables.map(([x1, y1, x2, y2], i) => (
              <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={pal.sil} strokeWidth="1.6" />
            ))}
          </motion.g>
        </motion.g>

        {/* water */}
        <rect x="0" y={HZ} width="1536" height={1024 - HZ} fill="url(#water)" />
        <motion.g style={{ x: near }}>
          {sunUp && <motion.rect initial={false} animate={{ x: sunX - 90 }} transition={{ duration: 2 }} y={HZ} width="180" height="300" fill="url(#streak)" opacity="0.55" className="pulse" />}
          {shimmer.map((s, i) => (
            <rect key={i} className="shimmer" x={(sunUp ? sunX : 768) - s.w / 2 + s.dx} y={s.y} width={s.w} height="2.4" rx="1.2" fill={pal.sun} style={{ animationDelay: `${s.d}s`, animationDuration: `${s.t}s` }} />
          ))}
        </motion.g>
        <rect x="0" y={HZ} width="1536" height="3" fill={pal.glow} opacity="0.35" />
      </svg>
    </div>
  );
}

/* Falling rain – only mounted when it is actually raining */
export function Rain({ active, heavy }) {
  const ref = useRef(null);
  useEffect(() => {
    if (!active) return undefined;
    const c = ref.current;
    const x = c.getContext('2d');
    let w; let h; let raf;
    const size = () => { w = c.width = window.innerWidth; h = c.height = window.innerHeight; };
    size();
    window.addEventListener('resize', size);
    const drops = Array.from({ length: heavy ? 260 : 140 }, () => ({ x: Math.random() * w, y: Math.random() * h, l: 10 + Math.random() * 18, v: 14 + Math.random() * 12 }));
    const loop = () => {
      raf = requestAnimationFrame(loop);
      x.clearRect(0, 0, w, h);
      x.strokeStyle = 'rgba(180,210,255,.32)';
      x.lineWidth = 1;
      x.beginPath();
      for (const d of drops) {
        x.moveTo(d.x, d.y);
        x.lineTo(d.x - d.l * 0.25, d.y + d.l);
        d.y += d.v; d.x -= d.v * 0.25;
        if (d.y > h) { d.y = -20; d.x = Math.random() * (w + 200); }
      }
      x.stroke();
    };
    loop();
    return () => { cancelAnimationFrame(raf); window.removeEventListener('resize', size); };
  }, [active, heavy]);
  return active ? <canvas ref={ref} className="rain" /> : null;
}
