import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { motion } from 'framer-motion';
import {
  Compass, ExternalLink, Globe, Locate, MapPin, Maximize2, Minimize2,
  Radio, X, CloudRain, Sun, Navigation
} from 'lucide-react';
import { PRESETS, reverseGeocode } from './weather.js';

const spring = { type: 'spring', stiffness: 420, damping: 34 };

const TILES = {
  dark: {
    name: 'Dark Canvas',
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png',
    options: {
      subdomains: 'abcd',
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap &copy; CARTO',
    },
  },
  satellite: {
    name: 'Satellite',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    options: {
      maxZoom: 18,
      attribution: '&copy; Esri World Imagery',
    },
  },
  radar: {
    name: 'Live Radar',
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png',
    options: {
      subdomains: 'abcd',
      maxZoom: 19,
      attribution: '&copy; CARTO &copy; RainViewer',
    },
  },
  streets: {
    name: 'Streets',
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
    options: {
      subdomains: 'abcd',
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap &copy; CARTO',
    },
  },
};

export default function MapModal({
  isOpen,
  onClose,
  city,
  onSelectLocation,
  onToast,
  initialMode = 'dark',
}) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const radarLayerRef = useRef(null);
  const tileLayerRef = useRef(null);

  const [mode, setMode] = useState(initialMode);
  const [locating, setLocating] = useState(false);
  const [radarPath, setRadarPath] = useState(null);
  const [radarTime, setRadarTime] = useState(null);

  // Sync mode with initialMode when opened
  useEffect(() => {
    if (isOpen && initialMode) {
      setMode(initialMode);
    }
  }, [isOpen, initialMode]);

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Fetch latest RainViewer radar layer
  useEffect(() => {
    if (!isOpen) return;
    fetch('https://api.rainviewer.com/public/weather-maps.json')
      .then((r) => r.json())
      .then((data) => {
        const latest = data.radar?.past?.[data.radar.past.length - 1];
        if (latest?.path) {
          setRadarPath(latest.path);
          if (latest.time) {
            const date = new Date(latest.time * 1000);
            setRadarTime(date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
          }
        }
      })
      .catch(() => {});
  }, [isOpen]);

  // Initialize Leaflet Map when modal opens
  useEffect(() => {
    if (!isOpen || !containerRef.current) return undefined;

    // Clean up any existing leaflet instance on this element
    if (containerRef.current._leaflet_id) {
      delete containerRef.current._leaflet_id;
    }
    if (mapRef.current) {
      try {
        mapRef.current.remove();
      } catch (e) {}
      mapRef.current = null;
    }

    const map = L.map(containerRef.current, {
      center: [city.lat, city.lon],
      zoom: 10,
      zoomControl: false,
      attributionControl: false,
    });
    mapRef.current = map;

    // Zoom control in top right
    L.control.zoom({ position: 'topright' }).addTo(map);

    // Initial base tile layer
    const cfg = TILES[mode] || TILES.dark;
    const tileLayer = L.tileLayer(cfg.url, cfg.options).addTo(map);
    tileLayerRef.current = tileLayer;

    // Glowing custom pin
    const pinIcon = L.divIcon({
      className: 'weatherly-map-pin',
      html: `
        <div class="pin-halo"></div>
        <div class="pin-bubble">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="#ff8a3d" stroke="#ffffff" stroke-width="1.8">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
            <circle cx="12" cy="9" r="2.5" fill="#ffffff"/>
          </svg>
        </div>
      `,
      iconSize: [36, 46],
      iconAnchor: [18, 46],
      popupAnchor: [0, -46],
    });

    const marker = L.marker([city.lat, city.lon], { icon: pinIcon }).addTo(map);
    marker.bindPopup(`<b>${city.name}</b><br/><small>${city.region}</small>`);
    markerRef.current = marker;

    // Ensure size updates immediately and after transition
    const invalidate = () => {
      try {
        map.invalidateSize();
      } catch (e) {}
    };
    invalidate();
    const t1 = setTimeout(invalidate, 120);
    const t2 = setTimeout(invalidate, 400);

    let ro;
    if (typeof ResizeObserver !== 'undefined' && containerRef.current) {
      ro = new ResizeObserver(() => invalidate());
      ro.observe(containerRef.current);
    }

    // Click anywhere on Earth to select location
    map.on('click', async (e) => {
      const { lat, lng } = e.latlng;
      marker.setLatLng([lat, lng]);
      marker.bindPopup('<b>Loading weather...</b>').openPopup();

      onToast?.(`Locating ${lat.toFixed(2)}°, ${lng.toFixed(2)}°...`);
      const newPlace = await reverseGeocode(lat, lng);
      marker.bindPopup(`<b>${newPlace.name}</b><br/><small>${newPlace.region}</small>`).openPopup();
      onSelectLocation?.(newPlace);
      onToast?.(`Weather updated for ${newPlace.name}`);
    });

    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      if (ro) ro.disconnect();
      try {
        map.remove();
      } catch (e) {}
      mapRef.current = null;
      if (containerRef.current) {
        delete containerRef.current._leaflet_id;
      }
    };
  }, [isOpen]);

  // Update base tile layer on mode change
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !isOpen) return;

    if (tileLayerRef.current) {
      try {
        map.removeLayer(tileLayerRef.current);
      } catch (e) {}
    }
    const cfg = TILES[mode] || TILES.dark;
    tileLayerRef.current = L.tileLayer(cfg.url, cfg.options).addTo(map);

    // Radar precipitation overlay
    if (mode === 'radar' && radarPath) {
      if (radarLayerRef.current) {
        try {
          map.removeLayer(radarLayerRef.current);
        } catch (e) {}
      }
      const rLayer = L.tileLayer(`https://tilecache.rainviewer.com${radarPath}/256/{z}/{x}/{y}/2/1_1.png`, {
        opacity: 0.85,
        zIndex: 50,
      }).addTo(map);
      radarLayerRef.current = rLayer;
    } else if (radarLayerRef.current) {
      try {
        map.removeLayer(radarLayerRef.current);
      } catch (e) {}
      radarLayerRef.current = null;
    }

    try {
      map.invalidateSize();
    } catch (e) {}
  }, [mode, radarPath, isOpen]);

  // Sync map center and marker when city changes externally
  useEffect(() => {
    const map = mapRef.current;
    const marker = markerRef.current;
    if (!map || !marker || !isOpen) return;

    const cur = marker.getLatLng();
    if (Math.abs(cur.lat - city.lat) > 0.001 || Math.abs(cur.lng - city.lon) > 0.001) {
      marker.setLatLng([city.lat, city.lon]);
      marker.bindPopup(`<b>${city.name}</b><br/><small>${city.region}</small>`);
      map.flyTo([city.lat, city.lon], Math.max(map.getZoom(), 9), { duration: 1.4 });
    }
  }, [city, isOpen]);

  // GPS Locate Current Position
  const handleLocate = () => {
    if (!navigator.geolocation) {
      onToast?.('Geolocation not supported by your browser');
      return;
    }
    setLocating(true);
    onToast?.('Finding your GPS position...');
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        setLocating(false);
        const { latitude, longitude } = pos.coords;
        const map = mapRef.current;
        if (map) {
          map.flyTo([latitude, longitude], 12, { duration: 1.5 });
        }
        const place = await reverseGeocode(latitude, longitude);
        onSelectLocation?.(place);
        onToast?.(`Position found: ${place.name}`);
      },
      (err) => {
        setLocating(false);
        onToast?.(err.message || 'Unable to retrieve location');
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  const handleCityPreset = (p) => {
    const map = mapRef.current;
    if (map) {
      map.flyTo([p.lat, p.lon], 10, { duration: 1.3 });
    }
    onSelectLocation?.(p);
    onToast?.(`Loaded ${p.name}`);
  };

  if (!isOpen) return null;

  const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${city.lat},${city.lon}`;

  return (
    <motion.div
      className="mapModalOverlay"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="glass mapModalCard"
        initial={{ opacity: 0, scale: 0.94, y: 30 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: 30 }}
        transition={{ type: 'spring', damping: 26, stiffness: 220 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Top Header */}
        <header className="mapModalHeader">
          <div className="mapModalTitleGroup">
            <div className="mapTitleIcon">
              <Compass size={24} color="#ffb454" />
            </div>
            <div>
              <h3>Interactive Weather &amp; Radar Map</h3>
              <p className="mapModalSub">
                <MapPin size={13} color="#8aa6ff" />
                <b>{city.name}</b>, {city.region} • {city.lat.toFixed(2)}°, {city.lon.toFixed(2)}°
              </p>
            </div>
          </div>

          <div className="mapModalActions">
            {/* Quick locate */}
            <button
              className={`modalBtn ${locating ? 'spin' : ''}`}
              onClick={handleLocate}
              title="Locate my position with GPS"
            >
              <Locate size={18} />
              <span>Locate Me</span>
            </button>

            {/* External Google Maps link */}
            <a
              href={googleMapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="modalBtn googleLink"
              title="Open in Google Maps"
            >
              <ExternalLink size={16} />
              <span>Google Maps</span>
            </a>

            {/* Close button */}
            <button
              className="modalCloseBtn"
              onClick={onClose}
              aria-label="Close Map"
              title="Close (Esc)"
            >
              <X size={20} />
            </button>
          </div>
        </header>

        {/* Layer Selector Bar */}
        <div className="mapModalLayerBar">
          <div className="mapLayerTabs" role="tablist">
            {Object.entries(TILES).map(([k, item]) => (
              <button
                key={k}
                role="tab"
                aria-selected={mode === k}
                className={`mapLayerTab ${mode === k ? 'on' : ''}`}
                onClick={() => setMode(k)}
              >
                {mode === k && <motion.span layoutId="modalLayerTabPill" className="tabBg" transition={spring} />}
                <span>
                  {k === 'radar' && <Radio size={14} className="radarPulseIcon" />}
                  {item.name}
                </span>
              </button>
            ))}
          </div>

          {mode === 'radar' && (
            <div className="radarLiveBadge">
              <span className="liveDot" />
              <span>Real-Time Precipitation Radar</span>
              {radarTime && <small>Updated {radarTime}</small>}
            </div>
          )}
        </div>

        {/* Map Canvas Viewport */}
        <div className="mapModalViewport">
          <div ref={containerRef} className="leafletMapCanvas fullCanvas" />

          {/* Floating Instructions and Legend */}
          <div className="mapFloatingBar modalFloatingBar">
            <span className="coordText">
              <Globe size={14} color="#ffb454" />
              <b>{city.name}</b> • {city.lat.toFixed(3)}°, {city.lon.toFixed(3)}°
            </span>
            <span className="mapHelp">
              Click anywhere on Earth to load weather &bull; Scroll to zoom
            </span>
          </div>

          {/* Radar color scale when radar layer is active */}
          {mode === 'radar' && (
            <div className="radarLegend">
              <span className="legendLabel">Rain Intensity:</span>
              <div className="legendGradient" />
              <div className="legendLabels">
                <span>Light</span>
                <span>Moderate</span>
                <span>Heavy</span>
              </div>
            </div>
          )}
        </div>

        {/* Quick Explore presets */}
        <footer className="mapModalFooter">
          <span className="presetLabel">Jump to City:</span>
          <div className="mapPresetChips">
            {PRESETS.map((p) => (
              <button
                key={p.name}
                className={`presetChip ${p.name === city.name ? 'on' : ''}`}
                onClick={() => handleCityPreset(p)}
              >
                {p.name}
              </button>
            ))}
          </div>
        </footer>
      </motion.div>
    </motion.div>
  );
}
