import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { motion } from 'framer-motion';
import { Compass, ExternalLink, Layers, Locate, Maximize2, Minimize2 } from 'lucide-react';
import { reverseGeocode } from './weather.js';

const spring = { type: 'spring', stiffness: 420, damping: 34 };

const TILES = {
  dark: {
    name: 'Dark',
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
    name: 'Radar',
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

export default function RealMap({ city, onSelectLocation, onToast, onOpenMap }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const radarLayerRef = useRef(null);
  const tileLayerRef = useRef(null);

  const [mode, setMode] = useState('dark');
  const [big, setBig] = useState(false);
  const [locating, setLocating] = useState(false);
  const [radarPath, setRadarPath] = useState(null);

  // Fetch latest RainViewer radar layer
  useEffect(() => {
    fetch('https://api.rainviewer.com/public/weather-maps.json')
      .then((r) => r.json())
      .then((data) => {
        const latest = data.radar?.past?.[data.radar.past.length - 1];
        if (latest?.path) {
          setRadarPath(latest.path);
        }
      })
      .catch(() => {});
  }, []);

  // Initialize Leaflet Map
  useEffect(() => {
    if (!containerRef.current) return undefined;

    // Clean up if container was already initialized by Leaflet (prevents "Map container is already initialized" crash)
    if (containerRef.current._leaflet_id) {
      delete containerRef.current._leaflet_id;
    }
    if (mapRef.current) {
      try {
        mapRef.current.remove();
      } catch (err) {}
      mapRef.current = null;
    }

    const map = L.map(containerRef.current, {
      center: [city.lat, city.lon],
      zoom: 9,
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
          <svg viewBox="0 0 24 24" width="22" height="22" fill="#ff8a3d" stroke="#ffffff" stroke-width="1.8">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
            <circle cx="12" cy="9" r="2.5" fill="#ffffff"/>
          </svg>
        </div>
      `,
      iconSize: [32, 42],
      iconAnchor: [16, 42],
      popupAnchor: [0, -42],
    });

    const marker = L.marker([city.lat, city.lon], { icon: pinIcon }).addTo(map);
    marker.bindPopup(`<b>${city.name}</b><br/><small>${city.region}</small>`);
    markerRef.current = marker;

    // Ensure map tiles and container measure accurately
    const invalidate = () => {
      try {
        map.invalidateSize();
      } catch (e) {}
    };
    invalidate();
    const t1 = setTimeout(invalidate, 100);
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

      onToast?.(`Locating ${lat.toFixed(2)}°, ${lng.toFixed(2)}...`);
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
  }, []); // Run once on mountnt

  // Update base tile layer on mode change
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (tileLayerRef.current) {
      map.removeLayer(tileLayerRef.current);
    }
    const cfg = TILES[mode] || TILES.dark;
    tileLayerRef.current = L.tileLayer(cfg.url, cfg.options).addTo(map);

    // Radar precipitation overlay
    if (mode === 'radar' && radarPath) {
      if (radarLayerRef.current) {
        map.removeLayer(radarLayerRef.current);
      }
      const rLayer = L.tileLayer(`https://tilecache.rainviewer.com${radarPath}/256/{z}/{x}/{y}/2/1_1.png`, {
        opacity: 0.75,
        zIndex: 50,
      }).addTo(map);
      radarLayerRef.current = rLayer;
    } else if (radarLayerRef.current) {
      map.removeLayer(radarLayerRef.current);
      radarLayerRef.current = null;
    }
  }, [mode, radarPath]);

  // Sync map center and marker when city changes externally
  useEffect(() => {
    const map = mapRef.current;
    const marker = markerRef.current;
    if (!map || !marker) return;

    const cur = marker.getLatLng();
    if (Math.abs(cur.lat - city.lat) > 0.001 || Math.abs(cur.lng - city.lon) > 0.001) {
      marker.setLatLng([city.lat, city.lon]);
      marker.bindPopup(`<b>${city.name}</b><br/><small>${city.region}</small>`);
      map.flyTo([city.lat, city.lon], Math.max(map.getZoom(), 8), { duration: 1.4 });
    }
  }, [city]);

  // Resize map container on expand/contract
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const t = setTimeout(() => map.invalidateSize(), 300);
    return () => clearTimeout(t);
  }, [big]);

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
          map.flyTo([latitude, longitude], 11, { duration: 1.5 });
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

  const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${city.lat},${city.lon}`;

  return (
    <div className="realMapCard">
      <header className="mapHeader">
        <div className="mapTitle" onClick={onOpenMap} style={{ cursor: onOpenMap ? 'pointer' : 'default' }}>
          <Compass size={20} color="#ffb454" />
          <h3>Interactive Global Map</h3>
        </div>
        <div className="mapControls">
          {onOpenMap && (
            <button
              className="openFullMapBtn"
              onClick={onOpenMap}
              title="Open full interactive map modal"
            >
              <Maximize2 size={14} />
              <span>Open Map</span>
            </button>
          )}
          <a
            href={googleMapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="googleMapsBtn"
            title="Open in Google Maps"
          >
            <span>Google Maps</span>
            <ExternalLink size={14} />
          </a>
          <button
            className={`mapIconBtn ${locating ? 'spin' : ''}`}
            onClick={handleLocate}
            title="Locate my position"
            aria-label="Locate my position"
          >
            <Locate size={18} />
          </button>
          <button
            className="mapIconBtn"
            onClick={onOpenMap ? onOpenMap : () => setBig(!big)}
            aria-label="Open full map"
            title="Open full map"
          >
            <Maximize2 size={18} />
          </button>
        </div>
      </header>

      {/* Layer selector tabs */}
      <div className="mapLayerTabs" role="tablist">
        {Object.entries(TILES).map(([k, item]) => (
          <button
            key={k}
            role="tab"
            aria-selected={mode === k}
            className={`mapLayerTab ${mode === k ? 'on' : ''}`}
            onClick={() => setMode(k)}
          >
            {mode === k && <motion.span layoutId="mapLayerTabPill" className="tabBg" transition={spring} />}
            <span>{item.name}</span>
          </button>
        ))}
      </div>

      {/* Leaflet map viewport */}
      <motion.div
        className="mapViewportWrapper"
        initial={false}
        animate={{ height: big ? 420 : 210 }}
        transition={{ type: 'spring', stiffness: 160, damping: 22 }}
      >
        <div ref={containerRef} className="leafletMapCanvas" />

        {/* Floating coordinates indicator & instructions */}
        <div className="mapFloatingBar">
          <span className="coordText">
            <b>{city.name}</b> • {city.lat.toFixed(2)}°, {city.lon.toFixed(2)}°
          </span>
          <span className="mapHelp">Click anywhere to explore</span>
        </div>
      </motion.div>
    </div>
  );
}
