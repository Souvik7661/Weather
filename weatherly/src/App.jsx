import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, animate, motion, useMotionValue, useReducedMotion, useTransform } from 'framer-motion';
import {
  Activity, ArrowRight, BarChart3, Bell, ChevronDown, ChevronLeft, ChevronRight, Clock, Cloud, CloudFog, CloudLightning,
  CloudMoon, CloudRain, CloudSnow, CloudSun, CalendarDays, Droplet, Gauge, Home, Leaf, Lightbulb, Map as MapIcon,
  MapPin, Maximize2, Minimize2, Moon, Radar as RadarIcon, Search, Settings, Sun, Thermometer, Umbrella, Wind,
} from 'lucide-react';
import Scene, { Rain } from './Scene.jsx';
import RealMap from './RealMap.jsx';
import {
  PRESETS, dayMonth, dayName, fmtClock, fmtDate, fmtDur, fmtHour, fmtTime, geocode, sameDay, useNow, useWeather,
} from './weather.js';

/* ------------------------------------------------------------------ */
/*  Shared bits                                                         */
/* ------------------------------------------------------------------ */
const UnitCtx = createContext(null);
const useUnit = () => useContext(UnitCtx);

const container = { hidden: {}, show: { transition: { delayChildren: 0.1 } } };
const col = { hidden: {}, show: { transition: { staggerChildren: 0.09 } } };
const rise = {
  hidden: { opacity: 0, y: 28, scale: 0.97 },
  show: { opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 110, damping: 16 } },
};
const spring = { type: 'spring', stiffness: 420, damping: 34 };

const Card = ({ className = '', children, ...rest }) => (
  <motion.section variants={rise} className={`glass ${className}`} {...rest}>{children}</motion.section>
);

function Counter({ value, className }) {
  const mv = useMotionValue(0);
  const shown = useTransform(mv, (v) => Math.round(v));
  useEffect(() => {
    const c = animate(mv, value ?? 0, { duration: 1.6, ease: [0.16, 1, 0.3, 1] });
    return () => c.stop();
  }, [value, mv]);
  return <motion.span className={className}>{shown}</motion.span>;
}

function useOutside(ref, cb, on) {
  useEffect(() => {
    if (!on) return undefined;
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) cb(); };
    document.addEventListener('pointerdown', h);
    return () => document.removeEventListener('pointerdown', h);
  }, [on, ref, cb]);
}

/* Small filled weather icons */
function MiniIcon({ kind, day = true, size = 34 }) {
  const s = size;
  const sun = { color: '#ffc23d', fill: '#ffc23d', fillOpacity: 0.9, strokeWidth: 1.8 };
  const cloud = { color: '#e9efff', fill: '#dfe8ff', fillOpacity: 0.95, strokeWidth: 1.6 };
  const moon = { color: '#9a8cff', fill: '#9a8cff', fillOpacity: 0.85, strokeWidth: 1.8 };
  const box = { position: 'relative', width: s, height: s, display: 'inline-block' };
  switch (kind) {
    case 'clear': return day ? <Sun size={s} {...sun} /> : <Moon size={s} {...moon} />;
    case 'partly':
      return day ? (
        <span style={box}>
          <Sun size={s * 0.66} style={{ position: 'absolute', left: 0, top: 0 }} {...sun} />
          <Cloud size={s * 0.84} style={{ position: 'absolute', right: -2, bottom: 0 }} {...cloud} />
        </span>
      ) : (
        <span style={box}>
          <Moon size={s * 0.66} style={{ position: 'absolute', left: 0, top: 0 }} {...moon} />
          <Cloud size={s * 0.84} style={{ position: 'absolute', right: -2, bottom: 0 }} {...cloud} />
        </span>
      );
    case 'rain': return <CloudRain size={s} color="#7cb8ff" fill="#cfe6ff" fillOpacity={0.35} strokeWidth={1.7} />;
    case 'storm': return <CloudLightning size={s} color="#ffd24a" fill="#9aa4d8" fillOpacity={0.4} strokeWidth={1.7} />;
    case 'snow': return <CloudSnow size={s} color="#d6eaff" fill="#d6eaff" fillOpacity={0.3} strokeWidth={1.7} />;
    case 'fog': return <CloudFog size={s} color="#c9d3ee" strokeWidth={1.7} />;
    default: return <Cloud size={s} {...cloud} />;
  }
}

/* ------------------------------------------------------------------ */
/*  Big 3D-ish weather orb                                              */
/* ------------------------------------------------------------------ */
function CloudShape({ dark, style }) {
  return (
    <div className={`cloudShape ${dark ? 'dark' : ''}`} style={style}>
      <i /><i /><i /><b />
    </div>
  );
}

function Orb({ kind, isDay }) {
  const sunny = isDay && (kind === 'clear' || kind === 'partly');
  const moony = !isDay && (kind === 'clear' || kind === 'partly');
  const wet = kind === 'rain' || kind === 'storm';
  const hasCloud = kind !== 'clear';
  return (
    <div className={`orb ${kind}`}>
      {sunny && <motion.div className="sunBall" animate={{ scale: [1, 1.06, 1] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }} />}
      {moony && (
        <motion.svg className="moonBall" viewBox="0 0 100 100" animate={{ rotate: [-4, 4, -4] }} transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}>
          <defs><radialGradient id="mg" cx="0.35" cy="0.3"><stop offset="0" stopColor="#fff" /><stop offset="1" stopColor="#b9b2ff" /></radialGradient></defs>
          <path d="M62 10a42 42 0 1 0 28 62A34 34 0 0 1 62 10z" fill="url(#mg)" />
        </motion.svg>
      )}
      {hasCloud && (
        <motion.div className="cloudWrap" animate={{ y: [0, -7, 0], x: [0, 5, 0] }} transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}>
          {kind === 'cloudy' && <CloudShape style={{ position: 'absolute', left: 30, bottom: 26, transform: 'scale(.7)', opacity: 0.8 }} dark />}
          <CloudShape dark={wet || kind === 'cloudy'} />
          {wet && [8, 24, 42, 60, 78, 96, 114].map((l, i) => (
            <i key={l} className="rainDrop" style={{ left: l, animationDelay: `${(i % 4) * 0.22}s` }} />
          ))}
          {kind === 'storm' && (
            <svg className="bolt" viewBox="0 0 24 40"><path d="M14 0 2 22h8l-3 18L22 16h-9z" fill="#ffe27a" /></svg>
          )}
          {kind === 'snow' && [14, 36, 58, 80, 102].map((l, i) => <i key={l} className="flake" style={{ left: l, animationDelay: `${i * 0.5}s` }} />)}
          {kind === 'fog' && [0, 1, 2].map((i) => <i key={i} className="fogBar" style={{ top: 80 + i * 12, animationDelay: `${i * 0.8}s` }} />)}
        </motion.div>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Top bar                                                             */
/* ------------------------------------------------------------------ */
function Topbar({ city, setCity, now, live, locOpen, setLocOpen, alerts, unread, setUnread }) {
  const [q, setQ] = useState('');
  const [res, setRes] = useState([]);
  const [bell, setBell] = useState(false);
  const sRef = useRef(null); const lRef = useRef(null); const bRef = useRef(null);

  useEffect(() => {
    if (q.trim().length < 2) { setRes([]); return undefined; }
    const ac = new AbortController();
    const id = setTimeout(() => geocode(q, ac.signal).then(setRes).catch(() => {}), 350);
    return () => { clearTimeout(id); ac.abort(); };
  }, [q]);

  useOutside(sRef, () => setRes([]), res.length > 0);
  useOutside(lRef, () => setLocOpen(false), locOpen);
  useOutside(bRef, () => setBell(false), bell);

  const pick = (c) => { setCity(c); setQ(''); setRes([]); setLocOpen(false); };
  const { h, m, ap } = fmtClock(now);
  const list = PRESETS.some((p) => p.name === city.name) ? PRESETS : [city, ...PRESETS];

  return (
    <motion.header variants={rise} className="top">
      <div className="searchWrap" ref={sRef}>
        <label className="glass search">
          <Search size={20} />
          <input
            value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search city, state or country..."
            onKeyDown={(e) => { if (e.key === 'Enter' && res[0]) pick(res[0]); }} aria-label="Search for a place"
          />
        </label>
        <AnimatePresence>
          {res.length > 0 && (
            <motion.ul className="glass drop" initial={{ opacity: 0, y: -8, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -8 }}>
              {res.map((r) => (
                <li key={`${r.lat}${r.lon}`}>
                  <button onClick={() => pick(r)}><MapPin size={16} /><span>{r.name}<small>{r.region}</small></span></button>
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>

      <div className="searchWrap" ref={lRef}>
        <button className="glass locBtn" onClick={() => setLocOpen(!locOpen)} aria-expanded={locOpen}>
          <MapPin size={20} className="pin" />
          <span>{city.name}, {city.region.split(', ').pop()}</span>
          <motion.span animate={{ rotate: locOpen ? 180 : 0 }}><ChevronDown size={20} /></motion.span>
        </button>
        <AnimatePresence>
          {locOpen && (
            <motion.ul className="glass drop" initial={{ opacity: 0, y: -8, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -8 }}>
              {list.map((p) => (
                <li key={p.name}>
                  <button onClick={() => pick(p)} className={p.name === city.name ? 'on' : ''}><MapPin size={16} /><span>{p.name}<small>{p.region}</small></span></button>
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>

      <div className="glass dateChip">
        <div>
          <span>{fmtDate(now)}</span>
          <strong>{h}<i className="colon">:</i>{m} {ap}</strong>
        </div>
        <span className={`live ${live ? 'on' : ''}`} title={live ? 'Live data' : 'Demo data (offline)'}><i />{live ? 'Live' : 'Demo'}</span>
      </div>

      <div className="searchWrap" ref={bRef}>
        <motion.button
          className="glass bellBtn" aria-label="Notifications" onClick={() => { setBell(!bell); setUnread(false); }}
          animate={unread ? { rotate: [0, 16, -14, 9, -5, 0] } : {}} transition={{ duration: 0.9, repeat: Infinity, repeatDelay: 5 }}
        >
          <Bell size={22} />
          {unread && <span className="dot" />}
        </motion.button>
        <AnimatePresence>
          {bell && (
            <motion.div className="glass drop bellPop" initial={{ opacity: 0, y: -8, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -8 }}>
              <h4>Alerts</h4>
              {alerts.map((a) => <p key={a.title}><strong>{a.title}</strong>{a.text}</p>)}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.header>
  );
}

/* ------------------------------------------------------------------ */
/*  Sidebar                                                             */
/* ------------------------------------------------------------------ */
const NAV = [
  ['Home', Home, 'top'], ['Forecast', CloudSun, 'week'], ['Maps', MapIcon, 'map'], ['Air Quality', Gauge, 'aqi'],
  ['Radar', RadarIcon, 'map'], ['Locations', MapPin, 'loc'], ['Insights', BarChart3, 'insights'], ['Settings', Settings, 'settings'],
];

function Sidebar({ active, onNav, unit }) {
  return (
    <motion.aside className="glass side" initial={{ opacity: 0, x: -36 }} animate={{ opacity: 1, x: 0 }} transition={{ type: 'spring', stiffness: 90, damping: 16 }}>
      <div className="logo">
        <motion.div animate={{ y: [0, -3, 0] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}><Cloud size={42} fill="#8aa6ff" color="#cfe0ff" strokeWidth={1.4} /></motion.div>
        <div><b>Weatherly</b><small>See Beyond the Sky</small></div>
      </div>
      <nav>
        {NAV.map(([name, Icon, target]) => (
          <button key={name} className={active === name ? 'on' : ''} onClick={() => onNav(name, target)}>
            {active === name && <motion.span layoutId="navPill" className="navPill" transition={spring} />}
            <Icon size={22} /><span>{name}</span>
            {name === 'Settings' && <em>°{unit}</em>}
          </button>
        ))}
      </nav>
      <div className="grow" />
      <div className="glass me">
        <div className="avatar"><span>SK</span></div>
        <div><b>Souvik Kundu</b></div>
      </div>
    </motion.aside>
  );
}

/* ------------------------------------------------------------------ */
/*  Hero                                                                */
/* ------------------------------------------------------------------ */
const periodOf = (h) => (h < 5 ? 'night' : h < 12 ? 'morning' : h < 17 ? 'afternoon' : h < 21 ? 'evening' : 'night');
const TAIL = {
  partly: 'with scattered clouds across the sky', clear: 'under clear skies', cloudy: 'beneath a blanket of clouds',
  rain: 'with rain falling nearby', storm: 'with thunderstorms in the area', snow: 'with snow in the air', fog: 'wrapped in a soft fog',
};
const feelWord = (t) => (t >= 31 ? 'hot' : t >= 24 ? 'warm and pleasant' : t >= 16 ? 'mild' : t >= 8 ? 'cool' : 'cold');
const uvLabel = (u) => (u < 3 ? 'Low' : u < 6 ? 'Moderate' : u < 8 ? 'High' : 'Very High');

function Hero({ city, data, now, day, isDay, onCity }) {
  const { t, unit, spd, spdUnit } = useUnit();
  const c = data.current;
  const desc = `A ${feelWord(c.temp)} ${periodOf(now.getUTCHours())} ${TAIL[c.kind] || ''}.`;
  const stats = [
    { Icon: Thermometer, color: '#ff6b6b', label: 'Feels Like', value: `${t(c.feels)}°${unit}` },
    { Icon: Droplet, color: '#59a8ff', label: 'Humidity', value: `${Math.round(c.humidity)}%` },
    { Icon: Wind, color: '#4fd1ff', label: 'Wind', value: `${spd(c.wind)} ${spdUnit}` },
    { Icon: Sun, color: '#ffc23d', label: 'UV Index', value: Math.round(c.uv), sub: uvLabel(c.uv) },
  ];
  return (
    <Card className="hero">
      <div className="heroHead">
        <AnimatePresence mode="wait">
          <motion.h1 key={city.name} initial={{ opacity: 0, y: 14, filter: 'blur(8px)' }} animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }} exit={{ opacity: 0, y: -14, filter: 'blur(8px)' }} transition={{ duration: 0.35 }}>
            {city.name}
          </motion.h1>
        </AnimatePresence>
        <button className="iconBtn" onClick={onCity} aria-label="Change city"><ChevronDown size={24} /></button>
      </div>
      <p className="region">{city.region}</p>

      <div className="temp">
        <Counter value={t(c.temp)} className="deg" />
        <span className="unit">°{unit}</span>
      </div>
      <AnimatePresence mode="wait">
        <motion.div key={c.label} initial={{ opacity: 0, x: -16 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 16 }} transition={{ duration: 0.3 }}>
          <h2 className="cond">{c.label}</h2>
          <p className="desc">{desc}</p>
        </motion.div>
      </AnimatePresence>

      <div className="stats">
        {stats.map(({ Icon, color, label, value, sub }, i) => (
          <motion.div key={label} className="chip" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.9 + i * 0.08, type: 'spring', stiffness: 120, damping: 14 }} whileHover={{ y: -4 }}>
            <Icon size={28} color={color} strokeWidth={1.8} />
            <div><small>{label}</small><b>{value}</b>{sub && <em>{sub}</em>}</div>
          </motion.div>
        ))}
      </div>

      <div className="glass orbCard">
        <Orb kind={c.kind} isDay={isDay} />
        <div className="glass orbPill">
          <MiniIcon kind={c.kind} day={isDay} size={30} />
          <div><b>{c.label}</b><small>{t(c.temp)}°{unit} | {t(day.lo)}° - {t(day.hi)}°</small></div>
        </div>
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Hourly + weekly                                                     */
/* ------------------------------------------------------------------ */
function Hourly({ list, current }) {
  const { t } = useUnit();
  const [sel, setSel] = useState(0);
  const [wrap, setWrap] = useState(false);
  const ref = useRef(null);
  const scroll = (d) => ref.current?.scrollBy({ left: d * 340, behavior: 'smooth' });
  return (
    <Card className="hourly" id="hourly" layout>
      <header>
        <h3><Clock size={20} /> Hourly Forecast</h3>
        <button className="seeAll" onClick={() => setWrap(!wrap)}>{wrap ? 'Show less' : 'See All'} <ChevronRight size={16} style={{ transform: wrap ? 'rotate(90deg)' : 'none' }} /></button>
      </header>
      <div className="hrow">
        <div ref={ref} className={`hours ${wrap ? 'wrap' : ''}`}>
          {list.map((h, i) => (
            <motion.button layout="position" key={h.time.getTime()} className={`hour ${sel === i ? 'on' : ''}`} onClick={() => setSel(i)} whileTap={{ scale: 0.95 }}>
              {sel === i && <motion.span layoutId="hourSel" className="hourBg" transition={spring} />}
              <span className="hl">{i === 0 ? 'Now' : fmtHour(h.time)}</span>
              <MiniIcon kind={h.kind} day={h.isDay} size={34} />
              <span className="ht">{t(i === 0 ? current.temp : h.temp)}°</span>
            </motion.button>
          ))}
        </div>
        {!wrap && (
          <div className="arrows">
            <button onClick={() => scroll(1)} aria-label="Later hours"><ChevronRight size={20} /></button>
            <button onClick={() => scroll(-1)} aria-label="Earlier hours"><ChevronLeft size={20} /></button>
          </div>
        )}
      </div>
    </Card>
  );
}

function Week({ daily }) {
  const { t } = useUnit();
  const [sel, setSel] = useState(0);
  return (
    <Card className="week" id="week">
      <header><h3><CalendarDays size={20} /> 7-Day Forecast</h3><ArrowRight size={18} className="mut" /></header>
      <div className="days">
        {daily.map((d, i) => (
          <motion.button key={d.date.getTime()} className={`day ${sel === i ? 'on' : ''}`} onClick={() => setSel(i)} whileTap={{ scale: 0.95 }}>
            {sel === i && <motion.span layoutId="daySel" className="dayBg" transition={spring} />}
            <b>{dayName(d.date)}</b><small>{dayMonth(d.date)}</small>
            <MiniIcon kind={d.kind} day size={32} />
            <strong>{t(d.hi)}°</strong><em>{t(d.lo)}°</em>
          </motion.button>
        ))}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Sun arc                                                             */
/* ------------------------------------------------------------------ */
function SunCard({ day, now }) {
  const total = day.sunset - day.sunrise;
  const raw = (now - day.sunrise) / total;
  const p = Math.min(1, Math.max(0, raw));
  const P0 = [40, 100]; const P1 = [190, -50]; const P2 = [340, 100];
  const pt = (k) => [0, 1].map((i) => (1 - k) ** 2 * P0[i] + 2 * (1 - k) * k * P1[i] + k * k * P2[i]);
  const [sx, sy] = pt(p);
  const up = raw > 0 && raw < 1;
  const d = 'M40 100 Q190 -50 340 100';
  return (
    <Card className="sunCard">
      <div className="cardTools"><Sun size={18} className="mut" /><button className="iconBtn" aria-label="More options"><span className="dots">•••</span></button></div>
      <svg viewBox="0 0 380 118" className="arc" role="img" aria-label="Sun position today">
        <defs>
          <linearGradient id="arcG" x1="0" x2="1"><stop offset="0" stopColor="#ffb454" /><stop offset="1" stopColor="#ff7e5f" /></linearGradient>
          <filter id="sunGlow"><feGaussianBlur stdDeviation="4" /></filter>
        </defs>
        <line x1="14" y1="100" x2="366" y2="100" stroke="rgba(255,255,255,.14)" />
        <path d={d} pathLength="1" fill="none" stroke="rgba(255,255,255,.4)" strokeWidth="1.4" strokeDasharray="0.012 0.018" />
        <motion.path d={d} fill="none" stroke="url(#arcG)" strokeWidth="2.6" strokeLinecap="round" initial={{ pathLength: 0 }} animate={{ pathLength: p }} transition={{ duration: 2, ease: 'easeOut', delay: 0.6 }} />
        <circle cx="40" cy="100" r="5" fill="#ffb454" />
        <path transform="translate(331 91) scale(.17)" d="M62 10a42 42 0 1 0 28 62A34 34 0 0 1 62 10z" fill="#b9c4ff" />
        <motion.g initial={false} animate={{ opacity: up ? 1 : 0 }}>
          <motion.circle initial={false} animate={{ cx: sx, cy: sy, r: [14, 22, 14] }} transition={{ r: { duration: 3, repeat: Infinity }, cx: { duration: 2 }, cy: { duration: 2 } }} fill="#ffb454" opacity="0.5" filter="url(#sunGlow)" />
          <motion.circle initial={false} animate={{ cx: sx, cy: sy }} transition={{ duration: 2 }} r="9" fill="#ffd36e" stroke="#fff3c4" strokeWidth="2" />
        </motion.g>
      </svg>
      <div className="sunTimes">
        <div><b>{fmtTime(day.sunrise)}</b><small>Sunrise</small></div>
        <div><b>{fmtDur(day.daylight)}</b><small>Daylight</small></div>
        <div><b>{fmtTime(day.sunset)}</b><small>Sunset</small></div>
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Interactive Global Map                                              */
/* ------------------------------------------------------------------ */
function MapCard({ city, onSelectLocation, onToast }) {
  return (
    <Card className="mapCard" id="map">
      <RealMap city={city} onSelectLocation={onSelectLocation} onToast={onToast} />
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Insights + AQI                                                      */
/* ------------------------------------------------------------------ */
function buildInsights({ data, now, hours, day1 }) {
  const c = data.current;
  const next12 = Math.max(0, ...hours.slice(0, 12).map((h) => h.pop));
  const wetNow = c.kind === 'rain' || c.kind === 'storm';
  const wetTomorrow = day1 && (day1.kind === 'rain' || day1.kind === 'storm' || day1.pop >= 50);
  let umbrella;
  if (wetNow) umbrella = { title: 'Carry an umbrella', text: 'Rain in the area right now.' };
  else if (next12 >= 50) umbrella = { title: 'Carry an umbrella', text: 'Rain is likely later today.' };
  else if (wetTomorrow) umbrella = { title: 'Carry an umbrella', text: 'Light rain expected tomorrow.' };
  else umbrella = { title: 'No umbrella needed', text: 'Dry skies for the next day.' };

  const period = periodOf(now.getUTCHours());
  let outdoor;
  if (wetNow) outdoor = { title: 'Plan indoor activities', text: 'Wet weather outside.' };
  else if (c.feels > 35) outdoor = { title: 'Limit time outdoors', text: 'It feels very hot. Take breaks in the shade.' };
  else if (c.feels < 12) outdoor = { title: 'Dress warm outside', text: 'It feels cold out there.' };
  else outdoor = { title: 'Good for outdoor activities', text: period === 'evening' ? 'Pleasant weather this evening.' : 'Pleasant weather right now.' };

  const u = c.uv;
  const uv = u < 3 ? { title: 'Low UV levels', text: 'No sun protection needed.' }
    : u < 6 ? { title: 'Moderate UV levels', text: 'Use sunscreen if outdoors.' }
      : u < 8 ? { title: 'High UV levels', text: 'Cover up and wear sunscreen.' }
        : { title: 'Very high UV levels', text: 'Avoid the midday sun.' };

  const hum = c.humidity >= 70 ? { title: 'Humidity is high', text: 'Stay hydrated.' }
    : c.humidity <= 30 ? { title: 'The air is dry', text: 'Drink water and moisturise.' }
      : { title: 'Humidity is comfortable', text: 'No special precautions.' };

  return [
    { ...umbrella, Icon: Umbrella, color: '#59a8ff' }, { ...outdoor, Icon: Activity, color: '#3be59a' },
    { ...uv, Icon: Sun, color: '#ffc23d' }, { ...hum, Icon: Droplet, color: '#59a8ff' },
  ];
}

function Insights({ items }) {
  return (
    <Card className="insights" id="insights">
      <header><h3><Lightbulb size={20} color="#ffc23d" fill="#ffc23d" fillOpacity={0.35} /> Today&apos;s Insights</h3></header>
      <div className="insList">
        <AnimatePresence initial={false}>
          {items.map(({ title, text, Icon, color }, i) => (
            <motion.div key={title} className="ins" layout initial={{ opacity: 0, x: 30 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -30 }} transition={{ delay: 0.5 + i * 0.1, type: 'spring', stiffness: 120, damping: 16 }} whileHover={{ x: 4 }}>
              <span className="insIcon" style={{ '--c': color }}><Icon size={24} color={color} /></span>
              <div><b>{title}</b><small>{text}</small></div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </Card>
  );
}

const aqiInfo = (v) => (v <= 50 ? ['Excellent', '#3be59a', '#16c7a5'] : v <= 100 ? ['Good', '#53e08a', '#1fd1b0'] : v <= 150 ? ['Moderate', '#ffd23f', '#ffa53a'] : v <= 200 ? ['Poor', '#ff8a3d', '#ff5a4a'] : ['Very Poor', '#ff4d5e', '#c2185b']);

function AQI({ aqi }) {
  const v = Math.round(aqi.value ?? 0);
  const [label, c1, c2] = aqiInfo(v);
  const frac = Math.min(1, v / 120);
  const arc = 'M35.4 114.6 A56 56 0 1 1 114.6 114.6';
  const tiles = [['PM2.5', aqi.pm25], ['PM10', aqi.pm10], ['NO₂', aqi.no2], ['SO₂', aqi.so2]];
  return (
    <Card className="aqi" id="aqi">
      <header><h3><Leaf size={20} color="#3be59a" /> Air Quality Index</h3><ArrowRight size={18} className="mut" /></header>
      <div className="gaugeRow">
        <motion.div className="leaf" animate={{ scale: [1, 1.08, 1] }} transition={{ duration: 3, repeat: Infinity }} style={{ color: c1 }}><Leaf size={22} /></motion.div>
        <div className="gauge">
          <svg viewBox="0 0 150 150">
            <defs><linearGradient id="gG" x1="0" x2="1" y1="1" y2="0"><motion.stop offset="0" initial={false} animate={{ stopColor: c1 }} /><motion.stop offset="1" initial={false} animate={{ stopColor: c2 }} /></linearGradient></defs>
            <path d={arc} fill="none" stroke="rgba(255,255,255,.12)" strokeWidth="9" strokeLinecap="round" />
            <motion.path d={arc} fill="none" stroke="url(#gG)" strokeWidth="9" strokeLinecap="round" initial={{ pathLength: 0 }} animate={{ pathLength: frac }} transition={{ duration: 1.8, ease: [0.16, 1, 0.3, 1], delay: 0.5 }} />
          </svg>
          <div className="gv"><Counter value={v} className="n" /><small>{label}</small></div>
        </div>
      </div>
      <div className="pollutants">
        {tiles.map(([k, val], i) => (
          <motion.div key={k} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1 + i * 0.07 }}>
            <small>{k}</small><b><Counter value={val ?? 0} /></b>
          </motion.div>
        ))}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  App                                                                 */
/* ------------------------------------------------------------------ */
const phaseFor = (now, day) => {
  const m = (a) => (now - a) / 60000;
  const set = m(day.sunset); const rise_ = m(day.sunrise);
  if (rise_ < -45 || set > 45) return 'night';
  if (set > -90 || rise_ < 60) return 'dusk';
  return 'day';
};

export default function App() {
  const [city, setCity] = useState(PRESETS[0]);
  const { data, live, loading, city: shown } = useWeather(city);
  const now = useNow(data.offset);
  const [unit, setUnit] = useState('C');
  const [active, setActive] = useState('Home');
  const [mode, setMode] = useState('radar');
  const [locOpen, setLocOpen] = useState(false);
  const [unread, setUnread] = useState(true);
  const [toast, setToast] = useState('');

  useEffect(() => {
    if (!toast) return undefined;
    const id = setTimeout(() => setToast(''), 2400);
    return () => clearTimeout(id);
  }, [toast]);

  const ctx = useMemo(() => ({
    unit,
    t: (v) => Math.round(unit === 'C' ? v : (v * 9) / 5 + 32),
    spd: (v) => Math.round(unit === 'C' ? v : v * 0.621371),
    spdUnit: unit === 'C' ? 'km/h' : 'mph',
  }), [unit]);

  const day = data.daily.find((d) => sameDay(d.date, now)) || data.daily[0];
  const dayIdx = Math.max(0, data.daily.indexOf(day));
  const week = data.daily.slice(dayIdx, dayIdx + 7);
  const raw = (now - day.sunrise) / (day.sunset - day.sunrise);
  const isDay = raw > 0 && raw < 1;
  const override = new URLSearchParams(window.location.search).get('phase');
  const phase = ['day', 'dusk', 'night'].includes(override) ? override : phaseFor(now, day);

  const hourKey = `${now.getUTCDate()}-${now.getUTCHours()}`;
  const hours = useMemo(() => {
    let idx = data.hourly.findIndex((h) => h.time > now) - 1;
    if (idx < 0) idx = 0;
    return data.hourly.slice(idx, idx + 24);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, hourKey]);

  const insights = useMemo(() => buildInsights({ data, now, hours, day1: week[1] }), [data, hourKey, hours, week]); // eslint-disable-line react-hooks/exhaustive-deps

  const goto = (id) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  const onNav = (name, target) => {
    setActive(name);
    if (name === 'Radar') setMode('radar');
    if (name === 'Maps') setMode('temperature');
    if (target === 'top') window.scrollTo({ top: 0, behavior: 'smooth' });
    else if (target === 'loc') { window.scrollTo({ top: 0, behavior: 'smooth' }); setLocOpen(true); }
    else if (target === 'settings') { const n = unit === 'C' ? 'F' : 'C'; setUnit(n); setToast(`Units switched to °${n}`); }
    else if (target) goto(target);
  };

  const wet = data.current.kind === 'rain' || data.current.kind === 'storm';

  return (
    <UnitCtx.Provider value={ctx}>
      <Scene phase={phase} progress={raw} />
      <Rain active={wet} heavy={data.current.kind === 'storm'} />
      <div className="vignette" />
      <AnimatePresence>
        {loading && <motion.div className="loadbar" initial={{ scaleX: 0, opacity: 1 }} animate={{ scaleX: 0.85 }} exit={{ scaleX: 1, opacity: 0 }} transition={{ duration: 1.2 }} />}
      </AnimatePresence>

      <div className="app">
        <Sidebar active={active} onNav={onNav} unit={unit} />
        <motion.main className="main" variants={container} initial="hidden" animate="show">
          <Topbar
            city={shown} setCity={setCity} now={now} live={live} locOpen={locOpen} setLocOpen={setLocOpen}
            alerts={insights.slice(0, 2)} unread={unread} setUnread={setUnread}
          />
          <div className="content">
            <motion.div className="colL" variants={col}>
              <Hero city={shown} data={data} now={now} day={day} isDay={isDay} onCity={() => { window.scrollTo({ top: 0, behavior: 'smooth' }); setLocOpen(true); }} />
              <Hourly list={hours} current={data.current} />
              <div className="bottom">
                <Week daily={week} />
                <AQI aqi={data.aqi} />
              </div>
            </motion.div>
            <motion.div className="colR" variants={col}>
              <SunCard day={day} now={now} />
              <MapCard
                city={shown}
                onSelectLocation={(newPlace) => {
                  setCity(newPlace);
                  setLocOpen(false);
                }}
                onToast={(msg) => setToast(msg)}
              />
              <Insights items={insights} />
            </motion.div>
          </div>
        </motion.main>
      </div>

      <AnimatePresence>
        {toast && (
          <motion.div className="glass toast" initial={{ opacity: 0, y: 30, scale: 0.95 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 20 }}>
            {toast}
          </motion.div>
        )}
      </AnimatePresence>
    </UnitCtx.Provider>
  );
}
