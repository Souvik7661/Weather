import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, animate, motion, useMotionValue, useReducedMotion, useTransform } from 'framer-motion';
import {
  Activity, ArrowRight, ArrowUpRight, BarChart3, Bell, CalendarDays, ChevronDown, ChevronLeft, ChevronRight,
  Clock, Cloud, CloudFog, CloudLightning, CloudMoon, CloudRain, CloudSnow, CloudSun, Compass, Droplet,
  Gauge, Home, Leaf, Lightbulb, Loader2, Map as MapIcon, MapPin, Maximize2, Moon, MoreHorizontal, Navigation,
  Radar as RadarIcon, Search, Settings, Sparkles, Sun, Thermometer, Umbrella, Wind, X, Zap,
} from 'lucide-react';
import Scene, { Rain } from './Scene.jsx';
import RealMap from './RealMap.jsx';
import MapModal from './MapModal.jsx';
import {
  PRESETS, dayMonth, dayName, fmtClock, fmtDate, fmtDur, fmtHour, fmtTime, geocode, reverseGeocode, sameDay, useNow, useWeather,
} from './weather.js';

/* ------------------------------------------------------------------ */
/*  Shared Context                                                      */
/* ------------------------------------------------------------------ */
const UnitCtx = createContext(null);
const useUnit = () => useContext(UnitCtx);

const spring = { type: 'spring', stiffness: 380, damping: 28 };
const container = { hidden: {}, show: { transition: { staggerChildren: 0.08, delayChildren: 0.05 } } };
const col = {
  hidden: { opacity: 0, y: 18 },
  show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 90, damping: 18 } },
};

function Card({ className = '', children, id, onClick, style }) {
  return (
    <motion.section
      id={id}
      className={`glass ${className}`}
      variants={col}
      onClick={onClick}
      style={style}
    >
      {children}
    </motion.section>
  );
}

function Counter({ value, className = '' }) {
  const [disp, setDisp] = useState(value);
  const prev = useRef(value);
  const red = useReducedMotion();

  useEffect(() => {
    if (red) { setDisp(value); return; }
    const ctrl = animate(prev.current, value, {
      duration: 0.8,
      ease: [0.16, 1, 0.3, 1],
      onUpdate: (v) => setDisp(Math.round(v)),
    });
    prev.current = value;
    return () => ctrl.stop();
  }, [value, red]);

  return <span className={className}>{disp}</span>;
}

function MiniIcon({ kind, day = true, size = 28 }) {
  const colSun = '#ffbe3d';
  const colCloud = '#d6e2ff';
  const colRain = '#5b98ff';

  if (kind === 'clear') return day ? <Sun size={size} color={colSun} fill={colSun} fillOpacity={0.3} /> : <Moon size={size} color="#ccd6f6" fill="#ccd6f6" fillOpacity={0.2} />;
  if (kind === 'partly') return day ? <CloudSun size={size} color={colCloud} /> : <CloudMoon size={size} color={colCloud} />;
  if (kind === 'rain') return <CloudRain size={size} color={colRain} />;
  if (kind === 'storm') return <CloudLightning size={size} color="#f5b842" />;
  if (kind === 'snow') return <CloudSnow size={size} color="#e1ecfc" />;
  if (kind === 'fog') return <CloudFog size={size} color="#a0aec0" />;
  return <Cloud size={size} color={colCloud} />;
}

/* ------------------------------------------------------------------ */
/*  Topbar                                                              */
/* ------------------------------------------------------------------ */
/*  Topbar                                                              */
/* ------------------------------------------------------------------ */
function Topbar({ city, setCity, now, live, locOpen, setLocOpen, alerts, unread, setUnread, onToast }) {
  const [q, setQ] = useState('');
  const [res, setRes] = useState([]);
  const [searchOpen, setSearchOpen] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [highlightIdx, setHighlightIdx] = useState(-1);
  const [bell, setBell] = useState(false);
  const sRef = useRef(null);
  const lRef = useRef(null);
  const bRef = useRef(null);

  useEffect(() => {
    if (q.trim().length < 2) {
      setRes([]);
      setIsSearching(false);
      setHighlightIdx(-1);
      return undefined;
    }
    setIsSearching(true);
    const id = setTimeout(async () => {
      try {
        const hits = await geocode(q);
        setRes(hits);
        setHighlightIdx(hits.length > 0 ? 0 : -1);
      } catch (e) {
        console.error('Geocode search error:', e);
      } finally {
        setIsSearching(false);
      }
    }, 250);
    return () => clearTimeout(id);
  }, [q]);

  useEffect(() => {
    const out = (e) => {
      if (sRef.current && !sRef.current.contains(e.target)) setSearchOpen(false);
      if (lRef.current && !lRef.current.contains(e.target)) setLocOpen(false);
      if (bRef.current && !bRef.current.contains(e.target)) setBell(false);
    };
    window.addEventListener('click', out);
    return () => window.removeEventListener('click', out);
  }, [setLocOpen]);

  const pick = (place) => {
    setCity(place);
    setQ('');
    setRes([]);
    setSearchOpen(false);
    setLocOpen(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightIdx((prev) => (res.length ? (prev + 1) % res.length : -1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightIdx((prev) => (res.length ? (prev - 1 + res.length) % res.length : -1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (highlightIdx >= 0 && res[highlightIdx]) {
        pick(res[highlightIdx]);
      } else if (res.length > 0) {
        pick(res[0]);
      } else if (q.trim()) {
        geocode(q).then((hits) => {
          if (hits.length > 0) pick(hits[0]);
        });
      }
    } else if (e.key === 'Escape') {
      setSearchOpen(false);
    }
  };

  const handleCurrentLocation = () => {
    if (!navigator.geolocation) {
      onToast?.('Geolocation not supported by your browser');
      return;
    }
    onToast?.('Detecting your GPS location...');
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;
        const place = await reverseGeocode(lat, lon);
        pick(place);
        onToast?.(`Located: ${place.name}`);
      },
      (err) => {
        onToast?.(`GPS error: ${err.message}`);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  const timeStr = fmtTime(now);

  return (
    <motion.header className="top" variants={col}>
      {/* 1. Global Search Bar with Live Worldwide Autocomplete */}
      <div className="searchWrap searchContainer" ref={sRef}>
        <div className="glass search">
          {isSearching ? (
            <Loader2 size={18} className="searchIcon searchSpin" />
          ) : (
            <Search size={18} className="searchIcon" />
          )}
          <input
            placeholder="Search any place in the world (e.g. Tokyo, Honolulu, Kolkata, Paris)..."
            value={q}
            onChange={(e) => {
              setQ(e.target.value);
              setSearchOpen(true);
            }}
            onFocus={() => {
              if (q.trim().length >= 2 || res.length > 0) setSearchOpen(true);
            }}
            onKeyDown={handleKeyDown}
            aria-label="Search place worldwide"
            aria-expanded={searchOpen && res.length > 0}
          />
          {q.length > 0 && (
            <button
              className="searchClearBtn"
              onClick={() => {
                setQ('');
                setRes([]);
                setSearchOpen(false);
              }}
              title="Clear search"
            >
              <X size={15} />
            </button>
          )}
        </div>

        {/* Worldwide Autocomplete Dropdown anchored directly below search bar */}
        <AnimatePresence>
          {searchOpen && (res.length > 0 || isSearching) && (
            <motion.ul
              className="glass drop searchDrop"
              initial={{ opacity: 0, y: -6, scale: 0.99 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.16 }}
            >
              <li className="dropHeader">
                <span>{isSearching ? 'Searching worldwide database...' : `${res.length} global locations found`}</span>
                <span className="dropHint">Press Enter to select</span>
              </li>
              {res.map((p, idx) => (
                <li key={`${p.name}-${p.lat}-${p.lon}-${idx}`}>
                  <button
                    onClick={() => pick(p)}
                    className={`dropItem searchItem ${idx === highlightIdx ? 'highlight' : ''}`}
                    onMouseEnter={() => setHighlightIdx(idx)}
                  >
                    <span className="dropFlag">{p.flag || '📍'}</span>
                    <div className="dropInfo">
                      <strong className="dropName">{p.name}</strong>
                      <small className="dropRegion">{p.region}</small>
                    </div>
                    <span className="dropCoords">{p.lat.toFixed(2)}°, {p.lon.toFixed(2)}°</span>
                  </button>
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>

      {/* 2. Location Dropdown Pill (Iconic Hubs + GPS Geolocation) */}
      <div className="searchWrap" ref={lRef}>
        <button
          className="glass locBtn"
          onClick={() => setLocOpen(!locOpen)}
          aria-expanded={locOpen}
          title="Select location"
        >
          <span className="locFlag">{city.flag || '📍'}</span>
          <span className="locText">{city.name}, {city.region.split(', ').pop()}</span>
          <motion.span animate={{ rotate: locOpen ? 180 : 0 }} transition={{ duration: 0.2 }}>
            <ChevronDown size={16} />
          </motion.span>
        </button>

        <AnimatePresence>
          {locOpen && (
            <motion.ul
              className="glass drop presetsDrop"
              initial={{ opacity: 0, y: -8, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18 }}
            >
              <li className="dropHeader">
                <span>Worldwide Hubs</span>
              </li>
              <li>
                <button onClick={handleCurrentLocation} className="dropItem gpsItem">
                  <Navigation size={15} color="#5b98ff" />
                  <div className="dropInfo">
                    <strong style={{ color: '#8bb7ff' }}>Use Current GPS Location</strong>
                    <small>Auto-detect latitude & longitude</small>
                  </div>
                </button>
              </li>
              <div className="dropDivider" />
              {PRESETS.map((p) => (
                <li key={`${p.name}-${p.lat}`}>
                  <button
                    onClick={() => pick(p)}
                    className={`dropItem presetItem ${p.name === city.name ? 'on' : ''}`}
                  >
                    <span className="dropFlag">{p.flag || '📍'}</span>
                    <div className="dropInfo">
                      <strong className="dropName">{p.name}</strong>
                      <small className="dropRegion">{p.region}</small>
                    </div>
                    {p.name === city.name && <span className="activeDot" />}
                  </button>
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>

      {/* 3. Date & Time Pill */}
      <div className="glass dateChip">
        <div className="dateChipInner">
          <span>{fmtDate(now)}</span>
          <strong>{timeStr}</strong>
        </div>
      </div>

      {/* 4. Bell Notification Button */}
      <div className="searchWrap" ref={bRef}>
        <motion.button
          className="glass bellBtn"
          aria-label="Notifications"
          onClick={() => { setBell(!bell); setUnread(false); }}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          <Bell size={20} />
          {unread && <span className="dot" />}
        </motion.button>
        <AnimatePresence>
          {bell && (
            <motion.div
              className="glass bellPop"
              initial={{ opacity: 0, y: -8, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18 }}
            >
              <div className="bellPopHeader">
                <div className="bellPopTitle">
                  <Bell size={16} color="#ffb454" />
                  <h4>Live Weather Alerts & Tips</h4>
                </div>
                <button
                  onClick={() => setBell(false)}
                  className="bellPopClose"
                  aria-label="Close alerts"
                >
                  <X size={14} />
                </button>
              </div>

              <div className="bellPopList">
                {alerts && alerts.length > 0 ? (
                  alerts.map((a) => {
                    const ItemIcon = a.Icon || Bell;
                    return (
                      <div key={a.title} className="bellPopItem">
                        <div
                          className="bellPopIconCircle"
                          style={{ color: a.color || '#5b98ff' }}
                        >
                          <ItemIcon size={16} />
                        </div>
                        <div className="bellPopItemText">
                          <strong>{a.title}</strong>
                          <span>{a.text}</span>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="bellPopEmpty">
                    <p>No severe weather warnings active.</p>
                    <small>Atmospheric conditions are optimal.</small>
                  </div>
                )}
              </div>
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
  ['Home', Home, 'top'],
  ['Forecast', CloudSun, 'week'],
  ['Maps', MapIcon, 'map'],
  ['Air Quality', Gauge, 'aqi'],
  ['Radar', RadarIcon, 'map'],
  ['Locations', MapPin, 'loc'],
  ['Insights', BarChart3, 'insights'],
  ['Settings', Settings, 'settings'],
];

function Sidebar({ active, onNav, unit }) {
  return (
    <motion.aside className="glass side" initial={{ opacity: 0, x: -36 }} animate={{ opacity: 1, x: 0 }} transition={{ type: 'spring', stiffness: 90, damping: 16 }}>
      {/* Brand Logo */}
      <div className="logo">
        <div className="logoBadge">
          <Cloud size={24} fill="#8aa6ff" color="#cfe0ff" strokeWidth={1.6} />
        </div>
        <div className="logoText">
          <b>Weatherly</b>
          <small>See Beyond the Sky</small>
        </div>
      </div>

      {/* Nav List */}
      <nav>
        {NAV.map(([name, Icon, target]) => (
          <button key={name} className={active === name ? 'on' : ''} onClick={() => onNav(name, target)}>
            {active === name && <motion.span layoutId="navPill" className="navPill" transition={spring} />}
            <Icon size={20} />
            <span>{name}</span>
            {name === 'Settings' && <em>°{unit}</em>}
          </button>
        ))}
      </nav>

      <div className="grow" />

      {/* Profile Card */}
      <div className="glass me">
        <div className="avatar"><span>SK</span></div>
        <div className="meInfo">
          <b>Souvik Kundu</b>
        </div>
      </div>
    </motion.aside>
  );
}

/* ------------------------------------------------------------------ */
/*  Hero Section                                                        */
/* ------------------------------------------------------------------ */
const periodOf = (h) => (h < 5 ? 'night' : h < 12 ? 'morning' : h < 17 ? 'afternoon' : h < 21 ? 'evening' : 'night');
const TAIL = {
  partly: 'with scattered clouds across the sky',
  clear: 'under clear skies',
  cloudy: 'beneath a blanket of clouds',
  rain: 'with rain falling nearby',
  storm: 'with thunderstorms in the area',
  snow: 'with snow in the air',
  fog: 'wrapped in a soft fog',
};
const feelWord = (t) => (t >= 31 ? 'hot' : t >= 24 ? 'warm and pleasant' : t >= 16 ? 'mild' : t >= 8 ? 'cool' : 'cold');
const uvLabel = (u) => (u < 3 ? 'Low' : u < 6 ? 'Moderate' : u < 8 ? 'High' : 'Very High');

function Hero({ city, data, now, day, isDay, onCity }) {
  const { t, unit, spd, spdUnit } = useUnit();
  const c = data.current;
  const desc = `A ${feelWord(c.temp)} ${periodOf(now.getUTCHours())} ${TAIL[c.kind] || ''}.`;

  const stats = [
    { Icon: Thermometer, color: '#ff6b6b', bg: 'rgba(255, 107, 107, 0.22)', label: 'Feels Like', value: `${t(c.feels)}°${unit}` },
    { Icon: Droplet, color: '#59a8ff', bg: 'rgba(89, 168, 255, 0.22)', label: 'Humidity', value: `${Math.round(c.humidity)}%` },
    { Icon: Wind, color: '#4fd1ff', bg: 'rgba(79, 209, 255, 0.22)', label: 'Wind', value: `${spd(c.wind)} ${spdUnit}` },
    { Icon: Sun, color: '#ffc23d', bg: 'rgba(255, 194, 61, 0.22)', label: 'UV Index', value: `${Math.round(c.uv)}`, sub: uvLabel(c.uv) },
  ];

  return (
    <motion.section className="heroCard" id="hero" variants={col}>
      <div className="heroTop">
        <div className="heroTitleRow" onClick={onCity} style={{ cursor: 'pointer' }}>
          <AnimatePresence mode="wait">
            <motion.h1
              key={city.name}
              initial={{ opacity: 0, y: 10, filter: 'blur(6px)' }}
              animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
              exit={{ opacity: 0, y: -10, filter: 'blur(6px)' }}
              transition={{ duration: 0.3 }}
            >
              {city.name}
            </motion.h1>
          </AnimatePresence>
          <ChevronDown size={20} className="heroChevron" />
        </div>
        <p className="heroRegion">{city.region}</p>
      </div>

      <div className="heroTempRow">
        <div className="heroTemp">
          <Counter value={t(c.temp)} className="tempDigits" />
          <span className="tempUnit">°{unit}</span>
        </div>
      </div>

      <div className="heroCondBlock">
        <h2 className="heroCond">{c.label}</h2>
        <p className="heroDesc">{desc}</p>
      </div>

      {/* 4 Metric Glassy Pills */}
      <div className="heroStats">
        {stats.map(({ Icon, color, bg, label, value, sub }) => (
          <div key={label} className="statPill">
            <div className="statIconCircle" style={{ background: bg, color }}>
              <Icon size={18} strokeWidth={2} />
            </div>
            <div className="statText">
              <small>{label}</small>
              <b>
                {value} {sub && <span className="statSub">{sub}</span>}
              </b>
            </div>
          </div>
        ))}
      </div>
    </motion.section>
  );
}

/* ------------------------------------------------------------------ */
/*  Hourly Forecast                                                     */
/* ------------------------------------------------------------------ */
function Hourly({ list, current }) {
  const { t } = useUnit();
  const [sel, setSel] = useState(0);
  const [wrap, setWrap] = useState(false);
  const ref = useRef(null);
  const scroll = (d) => ref.current?.scrollBy({ left: d * 300, behavior: 'smooth' });

  return (
    <Card className="hourly" id="hourly" layout>
      <header className="hourlyHeader">
        <div className="hourlyTitle">
          <Clock size={18} className="mut" />
          <h3>Hourly Forecast</h3>
        </div>
        <button className="seeAllBtn" onClick={() => setWrap(!wrap)}>
          {wrap ? 'Show less' : 'See All'} <ChevronRight size={15} style={{ transform: wrap ? 'rotate(90deg)' : 'none' }} />
        </button>
      </header>

      <div className="hourlyScrollContainer">
        <div ref={ref} className={`hours ${wrap ? 'wrap' : ''}`}>
          {list.map((h, i) => (
            <motion.button
              layout="position"
              key={h.time.getTime()}
              className={`hour ${sel === i ? 'on' : ''} ${i === 0 ? 'nowCard' : ''}`}
              onClick={() => setSel(i)}
              whileTap={{ scale: 0.95 }}
            >
              {sel === i && <motion.span layoutId="hourSel" className="hourBg" transition={spring} />}
              <span className="hl">{i === 0 ? 'Now' : fmtHour(h.time)}</span>
              <MiniIcon kind={h.kind} day={h.isDay} size={30} />
              <span className="ht">{t(i === 0 ? current.temp : h.temp)}°</span>
            </motion.button>
          ))}
        </div>
        {!wrap && (
          <button className="hourScrollNext" onClick={() => scroll(1)} aria-label="Next hours">
            <ChevronRight size={18} />
          </button>
        )}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  7-Day Forecast                                                      */
/* ------------------------------------------------------------------ */
function Week({ daily }) {
  const { t } = useUnit();
  const [sel, setSel] = useState(0);

  return (
    <Card className="weekCard" id="week">
      <header className="weekHeader">
        <div className="weekTitle">
          <CalendarDays size={18} className="mut" />
          <h3>7-Day Forecast</h3>
        </div>
        <ArrowUpRight size={18} className="mut" />
      </header>
      <div className="days">
        {daily.map((d, i) => (
          <motion.button
            key={d.date.getTime()}
            className={`day ${sel === i ? 'on' : ''}`}
            onClick={() => setSel(i)}
            whileTap={{ scale: 0.96 }}
          >
            {sel === i && <motion.span layoutId="daySel" className="dayBg" transition={spring} />}
            <b className="dayName">{dayName(d.date)}</b>
            <small className="dayDate">{dayMonth(d.date)}</small>
            <MiniIcon kind={d.kind} day size={30} />
            <strong className="dayHi">{t(d.hi)}°</strong>
            <em className="dayLo">{t(d.lo)}°</em>
          </motion.button>
        ))}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Air Quality Index                                                   */
/* ------------------------------------------------------------------ */
const aqiInfo = (v) => (v <= 50 ? ['Excellent', '#3be59a', '#16c7a5'] : v <= 100 ? ['Good', '#53e08a', '#1fd1b0'] : v <= 150 ? ['Moderate', '#ffd23f', '#ffa53a'] : v <= 200 ? ['Poor', '#ff8a3d', '#ff5a4a'] : ['Very Poor', '#ff4d5e', '#c2185b']);

function AQI({ aqi }) {
  const v = Math.round(aqi.value ?? 78);
  const [label, c1, c2] = aqiInfo(v);
  const frac = Math.min(1, v / 120);
  const arc = 'M35.4 114.6 A56 56 0 1 1 114.6 114.6';
  const tiles = [
    ['PM2.5', aqi.pm25 || 28],
    ['PM10', aqi.pm10 || 54],
    ['NO₂', aqi.no2 || 18],
    ['SO₂', aqi.so2 || 6],
  ];

  return (
    <Card className="aqiCard" id="aqi">
      <header className="aqiHeader">
        <div className="aqiTitle">
          <Leaf size={18} color="#3be59a" />
          <h3>Air Quality Index</h3>
        </div>
        <ArrowUpRight size={18} className="mut" />
      </header>

      <div className="gaugeWrapper">
        <div className="gaugeSvgBox">
          <svg viewBox="0 0 150 150" className="gaugeSvg">
            <defs>
              <linearGradient id="aqiGrad" x1="0" x2="1" y1="1" y2="0">
                <stop offset="0%" stopColor="#3be59a" />
                <stop offset="100%" stopColor="#53e08a" />
              </linearGradient>
            </defs>
            <path d={arc} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="10" strokeLinecap="round" />
            <motion.path
              d={arc}
              fill="none"
              stroke="url(#aqiGrad)"
              strokeWidth="10"
              strokeLinecap="round"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: frac }}
              transition={{ duration: 1.8, ease: [0.16, 1, 0.3, 1], delay: 0.3 }}
            />
          </svg>
          <div className="gaugeCenter">
            <Leaf size={22} color="#3be59a" className="gaugeLeaf" />
            <Counter value={v} className="gaugeValue" />
            <span className="gaugeStatus">{label}</span>
          </div>
        </div>
      </div>

      <div className="pollutantsRow">
        {tiles.map(([k, val]) => (
          <div key={k} className="pollutantItem">
            <small>{k}</small>
            <b>{val}</b>
          </div>
        ))}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Sun Schedule Arc                                                    */
/* ------------------------------------------------------------------ */
/*  Sun Schedule Arc Card (High-Contrast Sunrise & Morning Visibility) */
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
    <Card className="sunScheduleCard">
      <div className="sunScheduleHeader">
        <div className="sunScheduleTitle">
          <Sun size={20} color="#ffbe3d" fill="#ffbe3d" fillOpacity={0.4} />
          <span className="sunScheduleLabel">Sun Schedule</span>
        </div>
        <button className="iconBtn" aria-label="More options">
          <MoreHorizontal size={18} />
        </button>
      </div>

      <svg viewBox="0 0 380 122" className="arcSvg" role="img" aria-label="Sun position today">
        <defs>
          <linearGradient id="arcG" x1="0" x2="1">
            <stop offset="0%" stopColor="#ffbe3d" />
            <stop offset="50%" stopColor="#ffa02e" />
            <stop offset="100%" stopColor="#ff6542" />
          </linearGradient>
          <linearGradient id="arcAreaG" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#ffbe3d" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#ffbe3d" stopOpacity="0.0" />
          </linearGradient>
          <filter id="sunGlow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="6" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {/* Luminous Golden Sun Dome under Arc for Morning Visibility */}
        <path d="M40 100 Q190 -50 340 100 L340 100 L40 100 Z" fill="url(#arcAreaG)" />

        {/* Horizon Baseline with High-Contrast Dual Layer */}
        <line x1="20" y1="100" x2="360" y2="100" stroke="rgba(0,0,0,0.45)" strokeWidth="2.5" />
        <line x1="20" y1="100" x2="360" y2="100" stroke="rgba(255,255,255,0.45)" strokeWidth="1.5" />

        {/* Background Dashed Trajectory Arc (Shadow + High-Contrast Crisp Dashes) */}
        <path d={d} pathLength="1" fill="none" stroke="rgba(0,0,0,0.5)" strokeWidth="3.2" strokeDasharray="0.012 0.018" />
        <path d={d} pathLength="1" fill="none" stroke="rgba(255,255,255,0.9)" strokeWidth="2.2" strokeDasharray="0.012 0.018" />

        {/* Active Golden Progress Arc */}
        <motion.path
          d={d}
          fill="none"
          stroke="url(#arcG)"
          strokeWidth="3.6"
          strokeLinecap="round"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: p }}
          transition={{ duration: 2, ease: 'easeOut', delay: 0.5 }}
        />

        {/* Sunrise Point Marker (Left) */}
        <circle cx="40" cy="100" r="7" fill="rgba(255, 180, 84, 0.25)" stroke="#ffb454" strokeWidth="1.5" />
        <circle cx="40" cy="100" r="4" fill="#ffb454" />

        {/* Sunset Point Marker (Right with Moon Badge) */}
        <g transform="translate(330, 90)">
          <circle cx="10" cy="10" r="9" fill="rgba(180, 195, 255, 0.2)" stroke="rgba(180, 195, 255, 0.5)" strokeWidth="1" />
          <path transform="translate(2, 2) scale(0.16)" d="M62 10a42 42 0 1 0 28 62A34 34 0 0 1 62 10z" fill="#ccd6f6" />
        </g>

        {/* Live Sun Indicator & Pulse Glow */}
        <motion.g initial={false} animate={{ opacity: up ? 1 : 0 }}>
          <motion.circle
            initial={false}
            animate={{ cx: sx, cy: sy, r: [16, 24, 16] }}
            transition={{ r: { duration: 3, repeat: Infinity }, cx: { duration: 2 }, cy: { duration: 2 } }}
            fill="#ffbe3d"
            opacity="0.45"
            filter="url(#sunGlow)"
          />
          <motion.circle
            initial={false}
            animate={{ cx: sx, cy: sy }}
            transition={{ duration: 2 }}
            r="9.5"
            fill="#ffa726"
            stroke="#ffffff"
            strokeWidth="2.5"
          />
          <motion.circle
            initial={false}
            animate={{ cx: sx, cy: sy }}
            transition={{ duration: 2 }}
            r="4"
            fill="#ffffff"
          />
        </motion.g>
      </svg>

      <div className="sunScheduleTimes">
        <div><b>{fmtTime(day.sunrise)}</b><small>Sunrise</small></div>
        <div><b>{fmtDur(day.daylight)}</b><small>Daylight</small></div>
        <div><b>{fmtTime(day.sunset)}</b><small>Sunset</small></div>
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Live Weather Map Card                                               */
/* ------------------------------------------------------------------ */
function MapCard({ city, onSelectLocation, onToast, onOpenMap }) {
  return (
    <Card className="mapCard" id="map">
      <RealMap city={city} onSelectLocation={onSelectLocation} onToast={onToast} onOpenMap={onOpenMap} />
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Today's Insights                                                    */
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
    { ...umbrella, Icon: Umbrella, color: '#59a8ff' },
    { ...outdoor, Icon: Activity, color: '#3be59a' },
    { ...uv, Icon: Sun, color: '#ffc23d' },
    { ...hum, Icon: Droplet, color: '#59a8ff' },
  ];
}

function Insights({ items }) {
  return (
    <Card className="insightsCard" id="insights">
      <header className="insightsHeader">
        <Lightbulb size={20} color="#ffc23d" fill="#ffc23d" fillOpacity={0.4} />
        <h3>Today&apos;s Insights</h3>
      </header>
      <div className="insightsList">
        {items.map(({ title, text, Icon, color }, i) => (
          <motion.div
            key={title}
            className="insightRow"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 + i * 0.08, type: 'spring', stiffness: 120, damping: 16 }}
            whileHover={{ x: 3 }}
          >
            <div className="insightIconCircle" style={{ color }}>
              <Icon size={18} strokeWidth={2} />
            </div>
            <div className="insightContent">
              <b>{title}</b>
              <small>{text}</small>
            </div>
          </motion.div>
        ))}
      </div>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/*  Main Application Component                                          */
/* ------------------------------------------------------------------ */
const phaseFor = (now, day) => {
  if (!day || !day.sunrise || !day.sunset) return 'day';
  const m = (a) => (now.getTime() - a.getTime()) / 60000;
  const rise = m(day.sunrise);
  const set = m(day.sunset);

  // Sunrise window: 45 min before sunrise to 60 min after sunrise
  if (rise >= -45 && rise <= 60) return 'sunrise';
  // Dusk/Sunset window: 60 min before sunset to 45 min after sunset
  if (set >= -60 && set <= 45) return 'dusk';
  // Nighttime: before morning dawn or after evening dusk
  if (rise < -45 || set > 45) return 'night';
  return 'day';
};

export default function App() {
  const [city, setCity] = useState(PRESETS[0]);
  const { data, live, loading, city: shown } = useWeather(city);
  const now = useNow(data.offset);
  const [unit, setUnit] = useState('C');
  const [active, setActive] = useState('Home');
  const [mapOpen, setMapOpen] = useState(false);
  const [mapMode, setMapMode] = useState('radar');
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
  const phase = ['day', 'dusk', 'night', 'sunrise'].includes(override) ? override : phaseFor(now, day);

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
    if (name === 'Maps') {
      setMapMode('radar');
      setMapOpen(true);
      return;
    }
    if (name === 'Radar') {
      setMapMode('radar');
      setMapOpen(true);
      return;
    }
    if (target === 'top') window.scrollTo({ top: 0, behavior: 'smooth' });
    else if (target === 'loc') { window.scrollTo({ top: 0, behavior: 'smooth' }); setLocOpen(true); }
    else if (target === 'settings') { const n = unit === 'C' ? 'F' : 'C'; setUnit(n); setToast(`Units switched to °${n}`); }
    else if (target) goto(target);
  };

  return (
    <UnitCtx.Provider value={ctx}>
      <Scene
        phase={phase}
        progress={raw}
        current={data.current}
        city={shown}
        now={now}
        day={day}
      />

      <AnimatePresence>
        {loading && <motion.div className="loadbar" initial={{ scaleX: 0, opacity: 1 }} animate={{ scaleX: 0.85 }} exit={{ scaleX: 1, opacity: 0 }} transition={{ duration: 1.2 }} />}
      </AnimatePresence>

      <div className="app">
        <Sidebar active={active} onNav={onNav} unit={unit} />

        <motion.main className="main" variants={container} initial="hidden" animate="show">
          {/* Topbar Bar */}
          <Topbar
            city={shown} setCity={setCity} now={now} live={live} locOpen={locOpen} setLocOpen={setLocOpen}
            alerts={insights} unread={unread} setUnread={setUnread} onToast={setToast}
          />

          {/* 2-Column Dashboard Grid matching the Mockup */}
          <div className="content">
            {/* Left Column */}
            <motion.div className="colL" variants={col}>
              {/* Row 1: Hero Card */}
              <Hero
                city={shown} data={data} now={now} day={day} isDay={isDay}
                onCity={() => { window.scrollTo({ top: 0, behavior: 'smooth' }); setLocOpen(true); }}
              />

              {/* Row 2: Hourly Forecast Card */}
              <Hourly list={hours} current={data.current} />

              {/* Row 3: 7-Day Forecast + Air Quality Index */}
              <div className="bottomRowL">
                <Week daily={week} />
                <AQI aqi={data.aqi} />
              </div>
            </motion.div>

            {/* Right Column */}
            <motion.div className="colR" variants={col}>
              {/* Card 1: Sun Schedule Arc */}
              <SunCard day={day} now={now} />

              {/* Card 2: Live Weather Map */}
              <MapCard
                city={shown}
                onSelectLocation={(newPlace) => {
                  setCity(newPlace);
                  setLocOpen(false);
                }}
                onToast={(msg) => setToast(msg)}
                onOpenMap={() => {
                  setMapMode('radar');
                  setMapOpen(true);
                }}
              />

              {/* Card 3: Today's Insights */}
              <Insights items={insights} />
            </motion.div>
          </div>
        </motion.main>
      </div>

      {/* Interactive Map Modal */}
      <AnimatePresence>
        {mapOpen && (
          <MapModal
            isOpen={mapOpen}
            onClose={() => setMapOpen(false)}
            city={shown}
            onSelectLocation={(newPlace) => {
              setCity(newPlace);
              setLocOpen(false);
            }}
            onToast={(msg) => setToast(msg)}
            initialMode={mapMode}
          />
        )}
      </AnimatePresence>

      {/* Toast popup */}
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
