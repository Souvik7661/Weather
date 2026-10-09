import { useEffect, useState } from 'react';

export const API_KEY = import.meta.env?.VITE_WEATHER_API_KEY || 'd624f6ccefc94d368d7191636260810';

/* ------------------------------------------------------------------ */
/*  Cities Presets                                                     */
/* ------------------------------------------------------------------ */
export const PRESETS = [
  { name: 'Kolkata', region: 'West Bengal, India', lat: 22.5726, lon: 88.3639 },
  { name: 'Mumbai', region: 'Maharashtra, India', lat: 19.076, lon: 72.8777 },
  { name: 'New Delhi', region: 'Delhi, India', lat: 28.6139, lon: 77.209 },
  { name: 'London', region: 'England, United Kingdom', lat: 51.5072, lon: -0.1276 },
  { name: 'New York', region: 'New York, United States', lat: 40.7128, lon: -74.006 },
  { name: 'Tokyo', region: 'Tokyo, Japan', lat: 35.6762, lon: 139.6503 },
];

/* ------------------------------------------------------------------ */
/*  Weather condition classification                                   */
/* ------------------------------------------------------------------ */
export function parseKind(code, text = '') {
  const t = text.toLowerCase();
  if (t.includes('thunder') || t.includes('storm')) return 'storm';
  if (t.includes('snow') || t.includes('blizzard') || t.includes('sleet') || t.includes('ice') || t.includes('pellet')) return 'snow';
  if (t.includes('rain') || t.includes('drizzle') || t.includes('shower')) return 'rain';
  if (t.includes('fog') || t.includes('mist') || t.includes('haze') || t.includes('smoke')) return 'fog';
  if (t.includes('overcast') || (t.includes('cloud') && !t.includes('partly'))) return 'cloudy';
  if (t.includes('partly')) return 'partly';
  if (t.includes('sunny') || t.includes('clear')) return 'clear';
  return 'partly';
}

const KIND_LABEL = {
  clear: 'Clear Sky',
  partly: 'Partly Cloudy',
  cloudy: 'Overcast',
  rain: 'Light Rain',
  storm: 'Thunderstorm',
  snow: 'Snow',
  fog: 'Foggy',
};

/* ------------------------------------------------------------------ */
/*  Time helpers                                                        */
/*  "Local" dates are stored as Dates whose UTC fields = local fields.  */
/* ------------------------------------------------------------------ */
const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export const localNow = (offsetSec, ms = Date.now()) => new Date(ms + offsetSec * 1000);
export const parseLocal = (s) => new Date(s.length === 10 ? `${s}T00:00:00Z` : (s.includes('T') ? `${s}Z` : `${s.replace(' ', 'T')}:00Z`));

export function parseAstroTime(dateStr, timeStr) {
  const match = timeStr?.match(/^(\d+):(\d+)\s*(AM|PM)$/i);
  if (!match) return new Date(`${dateStr}T06:00:00Z`);
  let h = parseInt(match[1], 10);
  const m = parseInt(match[2], 10);
  const ap = match[3].toUpperCase();
  if (ap === 'PM' && h < 12) h += 12;
  if (ap === 'AM' && h === 12) h = 0;
  return new Date(`${dateStr}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00Z`);
}

export const fmtDate = (d) => `${DAYS[d.getUTCDay()]}, ${d.getUTCDate()} ${MONTHS[d.getUTCMonth()]} ${d.getUTCFullYear()}`;
export const fmtClock = (d) => {
  const h = d.getUTCHours();
  return { h: h % 12 || 12, m: String(d.getUTCMinutes()).padStart(2, '0'), s: String(d.getUTCSeconds()).padStart(2, '0'), ap: h < 12 ? 'AM' : 'PM' };
};
export const fmtTime = (d) => {
  const c = fmtClock(d);
  return `${c.h}:${c.m} ${c.ap}`;
};
export const fmtHour = (d) => `${d.getUTCHours() % 12 || 12} ${d.getUTCHours() < 12 ? 'AM' : 'PM'}`;
export const dayName = (d) => DAYS[d.getUTCDay()];
export const dayMonth = (d) => `${d.getUTCDate()} ${MONTHS[d.getUTCMonth()]}`;
export const fmtDur = (sec) => `${Math.floor(sec / 3600)}h ${String(Math.round((sec % 3600) / 60)).padStart(2, '0')}m`;
export const sameDay = (a, b) => a.toISOString().slice(0, 10) === b.toISOString().slice(0, 10);

function calcAqi(pm25) {
  if (pm25 == null) return 50;
  if (pm25 <= 12) return Math.round((50 / 12) * pm25);
  if (pm25 <= 35.4) return Math.round(50 + ((50 / 23.4) * (pm25 - 12)));
  if (pm25 <= 55.4) return Math.round(100 + ((50 / 20) * (pm25 - 35.4)));
  if (pm25 <= 150.4) return Math.round(150 + ((50 / 95) * (pm25 - 55.4)));
  return Math.round(200 + ((100 / 100) * (pm25 - 150.4)));
}

/* ------------------------------------------------------------------ */
/*  Global Geocoding (WeatherAPI + OpenStreetMap Nominatim + GPS)       */
/* ------------------------------------------------------------------ */
export async function geocode(q, signal) {
  const query = q.trim();
  if (!query) return [];

  // 1. Direct GPS coordinates: "lat, lon"
  const coordMatch = query.match(/^(-?\d+(\.\d+)?)\s*,\s*(-?\d+(\.\d+)?)$/);
  if (coordMatch) {
    const lat = parseFloat(coordMatch[1]);
    const lon = parseFloat(coordMatch[3]);
    if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
      const rev = await reverseGeocode(lat, lon);
      return [rev];
    }
  }

  const results = [];
  const seen = new Set();

  const addUnique = (item) => {
    const key = `${item.name.toLowerCase()}_${item.lat.toFixed(2)}_${item.lon.toFixed(2)}`;
    if (!seen.has(key)) {
      seen.add(key);
      results.push(item);
    }
  };

  // 2. WeatherAPI.com search
  try {
    const r = await fetch(`https://api.weatherapi.com/v1/search.json?key=${API_KEY}&q=${encodeURIComponent(query)}`, { signal });
    if (r.ok) {
      const j = await r.json();
      if (Array.isArray(j)) {
        j.forEach((x) => addUnique({
          name: x.name,
          region: [x.region, x.country].filter(Boolean).join(', '),
          lat: x.lat,
          lon: x.lon,
        }));
      }
    }
  } catch (err) {
    if (err.name === 'AbortError') throw err;
  }

  // 3. OpenStreetMap Nominatim global landmark/village/street search
  if (results.length < 5) {
    try {
      const r = await fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&addressdetails=1`, {
        signal,
        headers: { 'User-Agent': 'Weatherly/1.0' },
      });
      if (r.ok) {
        const j = await r.json();
        if (Array.isArray(j)) {
          j.forEach((x) => {
            const parts = (x.display_name || '').split(', ');
            const name = parts[0];
            const region = parts.slice(1, 4).join(', ');
            addUnique({
              name,
              region: region || parts.slice(1).join(', '),
              lat: parseFloat(x.lat),
              lon: parseFloat(x.lon),
            });
          });
        }
      }
    } catch (err) {
      if (err.name === 'AbortError') throw err;
    }
  }

  return results.slice(0, 8);
}

/* ------------------------------------------------------------------ */
/*  Reverse Geocode (Coordinates -> Location Name)                     */
/* ------------------------------------------------------------------ */
export async function reverseGeocode(lat, lon) {
  try {
    const r = await fetch(`https://api.weatherapi.com/v1/current.json?key=${API_KEY}&q=${lat},${lon}`);
    if (r.ok) {
      const j = await r.json();
      if (j.location?.name) {
        return {
          name: j.location.name,
          region: [j.location.region, j.location.country].filter(Boolean).join(', '),
          lat,
          lon,
        };
      }
    }
  } catch {}

  // Fallback to OpenStreetMap reverse geocoding
  try {
    const r = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`, {
      headers: { 'User-Agent': 'Weatherly/1.0' },
    });
    if (r.ok) {
      const j = await r.json();
      const addr = j.address || {};
      const name = addr.city || addr.town || addr.village || addr.suburb || addr.hamlet || j.name || 'Location';
      const region = [addr.state || addr.county, addr.country].filter(Boolean).join(', ');
      return {
        name,
        region: region || `${lat.toFixed(3)}°, ${lon.toFixed(3)}°`,
        lat,
        lon,
      };
    }
  } catch {}

  return {
    name: 'GPS Pin',
    region: `${lat.toFixed(4)}, ${lon.toFixed(4)}`,
    lat,
    lon,
  };
}

/* ------------------------------------------------------------------ */
/*  WeatherAPI.com Normalizer                                          */
/* ------------------------------------------------------------------ */
function normalizeWeatherAPI(data) {
  const loc = data.location;
  const localMs = Date.parse(loc.localtime.replace(' ', 'T') + ':00Z');
  const epochMs = loc.localtime_epoch * 1000;
  // Round to nearest 15-minute timezone boundary (900 seconds)
  const offset = Math.round((localMs - epochMs) / 900000) * 900;

  const cur = data.current;
  const currentKind = parseKind(cur.condition.code, cur.condition.text);

  const hourly = [];
  (data.forecast?.forecastday || []).forEach((f) => {
    (f.hour || []).forEach((h) => {
      hourly.push({
        time: new Date(h.time.replace(' ', 'T') + ':00Z'),
        temp: h.temp_c,
        label: h.condition.text,
        kind: parseKind(h.condition.code, h.condition.text),
        isDay: !!h.is_day,
        pop: h.chance_of_rain ?? (h.will_it_rain ? 80 : 0),
        uv: h.uv ?? 0,
      });
    });
  });

  const daily = (data.forecast?.forecastday || []).map((f) => {
    const sunrise = parseAstroTime(f.date, f.astro?.sunrise);
    const sunset = parseAstroTime(f.date, f.astro?.sunset);
    const daylight = Math.max(0, Math.round((sunset - sunrise) / 1000));
    return {
      date: new Date(`${f.date}T00:00:00Z`),
      label: f.day.condition.text,
      kind: parseKind(f.day.condition.code, f.day.condition.text),
      hi: f.day.maxtemp_c,
      lo: f.day.mintemp_c,
      pop: f.day.daily_chance_of_rain ?? 0,
      sunrise,
      sunset,
      daylight,
    };
  });

  const aq = cur.air_quality || {};
  const pm25 = aq.pm2_5 != null ? Math.round(aq.pm2_5) : 28;
  const aqiVal = aq['us-epa-index'] ? Math.min(300, aq['us-epa-index'] * 26) : calcAqi(pm25);

  return {
    offset,
    current: {
      temp: cur.temp_c,
      feels: cur.feelslike_c,
      humidity: cur.humidity,
      wind: cur.wind_kph,
      label: cur.condition.text,
      kind: currentKind,
      isDay: !!cur.is_day,
      uv: cur.uv ?? 0,
    },
    hourly,
    daily,
    aqi: {
      value: aqiVal,
      pm25,
      pm10: aq.pm10 != null ? Math.round(aq.pm10) : 54,
      no2: aq.no2 != null ? Math.round(aq.no2) : 18,
      so2: aq.so2 != null ? Math.round(aq.so2) : 6,
    },
  };
}

/* ------------------------------------------------------------------ */
/*  Fetch Weather (WeatherAPI.com primary, fallback safe)               */
/* ------------------------------------------------------------------ */
export async function fetchWeather(city, signal) {
  const query = `${city.lat},${city.lon}`;
  const url = `https://api.weatherapi.com/v1/forecast.json?key=${API_KEY}&q=${query}&days=7&aqi=yes&alerts=yes`;

  const response = await fetch(url, { signal });
  if (!response.ok) {
    throw new Error(`WeatherAPI request failed with status: ${response.status}`);
  }
  const data = await response.json();
  return normalizeWeatherAPI(data);
}

/* ------------------------------------------------------------------ */
/*  Offline fallback – mirrors the design mock-up (Kolkata, 28°C)       */
/* ------------------------------------------------------------------ */
export function buildFallback() {
  const off = 19800;
  const n = localNow(off);
  const day0 = Date.UTC(n.getUTCFullYear(), n.getUTCMonth(), n.getUTCDate());
  const HR = 3600e3;
  const hourly = Array.from({ length: 7 * 24 }, (_, i) => {
    const time = new Date(day0 + i * HR);
    const h = time.getUTCHours();
    const isDay = h >= 6 && h < 18;
    const kind = !isDay ? (h === 22 ? 'rain' : h >= 23 || h < 2 ? 'cloudy' : 'clear') : 'partly';
    return {
      time,
      isDay,
      kind,
      label: KIND_LABEL[kind],
      temp: 24 + 8 * Math.max(0, Math.sin(((h - 7) / 12) * Math.PI)),
      pop: kind === 'rain' ? 60 : 10,
      uv: isDay ? Math.max(0, 8 * Math.sin(((h - 6) / 12) * Math.PI)) : 0,
    };
  });
  const kinds = ['partly', 'rain', 'cloudy', 'clear', 'clear', 'partly', 'rain'];
  const hi = [32, 30, 31, 33, 34, 32, 31];
  const lo = [24, 23, 23, 24, 24, 24, 23];
  const pop = [10, 70, 30, 5, 5, 15, 60];
  const daily = kinds.map((kind, d) => ({
    date: new Date(day0 + d * 24 * HR),
    kind,
    label: KIND_LABEL[kind],
    hi: hi[d],
    lo: lo[d],
    pop: pop[d],
    sunrise: new Date(day0 + d * 24 * HR + (5 * 60 + 32) * 60e3),
    sunset: new Date(day0 + d * 24 * HR + (17 * 60 + 26) * 60e3),
    daylight: 11 * 3600 + 54 * 60,
  }));
  return {
    offset: off,
    current: { temp: 28, feels: 32, humidity: 78, wind: 12, kind: 'partly', label: 'Partly Cloudy', isDay: true, uv: 4 },
    hourly,
    daily,
    aqi: { value: 78, pm25: 28, pm10: 54, no2: 18, so2: 6 },
  };
}

/* ------------------------------------------------------------------ */
/*  React Hooks                                                         */
/* ------------------------------------------------------------------ */
export function useWeather(city) {
  const [state, setState] = useState(() => ({ data: buildFallback(), live: false, loading: true, city }));

  useEffect(() => {
    let alive = true;
    let ac;
    const load = async () => {
      ac?.abort();
      ac = new AbortController();
      setState((s) => ({ ...s, loading: true }));
      try {
        const data = await fetchWeather(city, ac.signal);
        if (alive) setState({ data, live: true, loading: false, city });
      } catch (e) {
        if (e.name === 'AbortError' || !alive) return;
        setState((s) => (s.live && s.city.lat === city.lat ? { ...s, loading: false } : { data: buildFallback(), live: false, loading: false, city }));
      }
    };
    load();
    const id = setInterval(load, 10 * 60 * 1000); // refresh every 10 minutes
    return () => {
      alive = false;
      ac?.abort();
      clearInterval(id);
    };
  }, [city]);

  return state;
}

/** Ticks every second in the *city's* local time. */
export function useNow(offsetSec) {
  const [now, setNow] = useState(() => localNow(offsetSec));
  useEffect(() => {
    setNow(localNow(offsetSec));
    const id = setInterval(() => setNow(localNow(offsetSec)), 1000);
    return () => clearInterval(id);
  }, [offsetSec]);
  return now;
}
