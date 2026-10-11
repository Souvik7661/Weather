import { useEffect, useMemo, useRef, useState } from 'react';
import { motion, AnimatePresence, useMotionValue, useSpring, useTransform } from 'framer-motion';

/* ------------------------------------------------------------------ */
/*  Determine Background Image 100% Dynamically by World Live Weather  */
/* ------------------------------------------------------------------ */
function resolveBackground({ current, city, phase, now, day }) {
  if (!current) return '/assets/bg_sunset_bridge.jpg';

  const kind = current.kind || 'partly';
  const isDay = current.isDay;

  // 1. Severe Calamities & precipitation anywhere in the world
  if (kind === 'storm') return '/assets/bg_storm.jpg';
  if (kind === 'snow') return '/assets/bg_snow.jpg';
  if (kind === 'rain') return '/assets/bg_rain.jpg';

  // 2. Solar Transitions: Golden Dawn Sunrise & Sunset Dusk
  if (phase === 'sunrise') {
    return '/assets/bg_sunrise.jpg';
  }
  if (phase === 'dusk') {
    return '/assets/bg_sunset_bridge.jpg';
  }

  // 3. Nighttime: If currently night at that location
  if (!isDay || phase === 'night') {
    return '/assets/bg_night.jpg';
  }

  // 4. Daytime atmospheric conditions (Overcast, fog, mist, smoke, haze)
  if (kind === 'cloudy' || kind === 'fog') {
    return '/assets/bg_overcast_london.jpg';
  }

  // 5. Clear Sunny or Partly Cloudy Daytime
  return '/assets/bg_sunny.jpg';
}

/* ------------------------------------------------------------------ */
/*  Scene Component                                                    */
/* ------------------------------------------------------------------ */
export default function Scene({ phase, progress, current, city, now, day }) {
  const bgUrl = useMemo(
    () => resolveBackground({ current, city, phase, now, day }),
    [current, city, phase, now, day]
  );

  /* Mouse parallax */
  const mx = useMotionValue(0);
  useEffect(() => {
    const handleMove = (e) => mx.set(e.clientX / window.innerWidth - 0.5);
    window.addEventListener('pointermove', handleMove);
    return () => window.removeEventListener('pointermove', handleMove);
  }, [mx]);

  const sx = useSpring(mx, { stiffness: 35, damping: 25 });
  const bgShiftX = useTransform(sx, (v) => v * -18);

  return (
    <div className="scene" aria-hidden="true">
      {/* Live Dynamic World Background with Smooth Crossfade */}
      <AnimatePresence initial={false}>
        <motion.div
          key={bgUrl}
          className="photo"
          style={{
            backgroundImage: `url(${bgUrl})`,
            x: bgShiftX,
          }}
          initial={{ opacity: 0, scale: 1.04 }}
          animate={{ opacity: 1, scale: 1.01 }}
          exit={{ opacity: 0 }}
          transition={{
            opacity: { duration: 0.8, ease: 'easeInOut' },
            scale: { duration: 35, repeat: Infinity, repeatType: 'reverse', ease: 'linear' },
          }}
        />
      </AnimatePresence>

      {/* Atmospheric Glass Scrim – lightweight so background shines through transparently */}
      <div className="scrim" />

      {/* Live Weather Particle Systems */}
      <Rain active={current?.kind === 'rain' || current?.kind === 'storm'} heavy={current?.kind === 'storm'} />
      <Lightning active={current?.kind === 'storm'} />
      <Snow active={current?.kind === 'snow'} />
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Dynamic Canvas Rain Effect                                         */
/* ------------------------------------------------------------------ */
export function Rain({ active, heavy }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!active) return undefined;
    const c = ref.current;
    if (!c) return undefined;
    const ctx = c.getContext('2d');
    let w;
    let h;
    let raf;

    const size = () => {
      w = c.width = window.innerWidth;
      h = c.height = window.innerHeight;
    };
    size();
    window.addEventListener('resize', size);

    const count = heavy ? 280 : 160;
    const drops = Array.from({ length: count }, () => ({
      x: Math.random() * (w + 200),
      y: Math.random() * h,
      l: 12 + Math.random() * 20,
      v: 16 + Math.random() * 14,
      a: 0.2 + Math.random() * 0.3,
    }));

    const loop = () => {
      raf = requestAnimationFrame(loop);
      ctx.clearRect(0, 0, w, h);

      for (const d of drops) {
        ctx.strokeStyle = `rgba(200, 225, 255, ${d.a})`;
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.moveTo(d.x, d.y);
        ctx.lineTo(d.x - d.l * 0.28, d.y + d.l);
        ctx.stroke();

        d.y += d.v;
        d.x -= d.v * 0.28;
        if (d.y > h) {
          d.y = -20;
          d.x = Math.random() * (w + 200);
        }
      }
    };

    loop();
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', size);
    };
  }, [active, heavy]);

  return active ? <canvas ref={ref} className="rainCanvas" /> : null;
}

/* ------------------------------------------------------------------ */
/*  Dynamic Lightning Flashes for Storm Calamity                      */
/* ------------------------------------------------------------------ */
export function Lightning({ active }) {
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    if (!active) return undefined;
    let timer;

    const scheduleFlash = () => {
      const delay = 4000 + Math.random() * 8000;
      timer = setTimeout(() => {
        setFlash(true);
        setTimeout(() => setFlash(false), 90);
        setTimeout(() => {
          setFlash(true);
          setTimeout(() => setFlash(false), 140);
        }, 160);
        scheduleFlash();
      }, delay);
    };

    scheduleFlash();
    return () => clearTimeout(timer);
  }, [active]);

  if (!active || !flash) return null;
  return <div className="lightningFlash" aria-hidden="true" />;
}

/* ------------------------------------------------------------------ */
/*  Dynamic Canvas Snow for Blizzard Calamity                         */
/* ------------------------------------------------------------------ */
export function Snow({ active }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!active) return undefined;
    const c = ref.current;
    if (!c) return undefined;
    const ctx = c.getContext('2d');
    let w;
    let h;
    let raf;

    const size = () => {
      w = c.width = window.innerWidth;
      h = c.height = window.innerHeight;
    };
    size();
    window.addEventListener('resize', size);

    const flakes = Array.from({ length: 140 }, () => ({
      x: Math.random() * w,
      y: Math.random() * h,
      r: 1 + Math.random() * 3.2,
      v: 0.8 + Math.random() * 1.8,
      wobble: Math.random() * Math.PI * 2,
      wobbleSpeed: 0.02 + Math.random() * 0.03,
      a: 0.3 + Math.random() * 0.6,
    }));

    const loop = () => {
      raf = requestAnimationFrame(loop);
      ctx.clearRect(0, 0, w, h);

      for (const f of flakes) {
        f.wobble += f.wobbleSpeed;
        f.x += Math.sin(f.wobble) * 0.8;
        f.y += f.v;

        ctx.fillStyle = `rgba(255, 255, 255, ${f.a})`;
        ctx.beginPath();
        ctx.arc(f.x, f.y, f.r, 0, Math.PI * 2);
        ctx.fill();

        if (f.y > h) {
          f.y = -10;
          f.x = Math.random() * w;
        }
      }
    };

    loop();
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', size);
    };
  }, [active]);

  return active ? <canvas ref={ref} className="snowCanvas" /> : null;
}
