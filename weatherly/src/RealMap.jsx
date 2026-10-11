import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { motion } from 'framer-motion';
import { Maximize2 } from 'lucide-react';
import { reverseGeocode } from './weather.js';

const spring = { type: 'spring', stiffness: 420, damping: 34 };

const TILES = {
  radar: {
    name: 'Radar',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}',
    options: {
      maxZoom: 16,
      attribution: '&copy; Esri &copy; RainViewer',
    },
    referenceUrl: 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}',
  },
  temperature: {
    name: 'Satellite',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    options: {
      maxZoom: 18,
      attribution: '&copy; Esri World Imagery',
    },
  },
  clouds: {
    name: 'Streets',
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    options: {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    },
  },
  wind: {
    name: 'Topo',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}',
    options: {
      maxZoom: 18,
      attribution: '&copy; Esri',
    },
  },
};

export default function RealMap({ city, onSelectLocation, onToast, onOpenMap }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const radarLayerRef = useRef(null);
  const tileLayerRef = useRef(null);
  const referenceLayerRef = useRef(null);

  const [mode, setMode] = useState('radar');
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

    // Initial base tile layer
    const cfg = TILES[mode] || TILES.radar;
    const tileLayer = L.tileLayer(cfg.url, cfg.options).addTo(map);
    tileLayerRef.current = tileLayer;

    // Custom glowing Kolkata location marker pin with label
    const pinIcon = L.divIcon({
      className: 'live-map-pin',
      html: `
        <div class="live-pin-beacon"></div>
        <div class="live-pin-dot"></div>
        <div class="live-pin-tag">
          <span>${city.name}</span>
        </div>
      `,
      iconSize: [20, 20],
      iconAnchor: [10, 10],
    });

    const marker = L.marker([city.lat, city.lon], { icon: pinIcon }).addTo(map);
    markerRef.current = marker;

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

    // Click on map to open full modal or select location
    map.on('click', () => {
      if (onOpenMap) {
        onOpenMap();
      }
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
  }, []); // Run once on mount

  // Update base tile layer on mode change
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (tileLayerRef.current) {
      try {
        map.removeLayer(tileLayerRef.current);
      } catch (e) {}
      tileLayerRef.current = null;
    }
    if (referenceLayerRef.current) {
      try {
        map.removeLayer(referenceLayerRef.current);
      } catch (e) {}
      referenceLayerRef.current = null;
    }

    const cfg = TILES[mode] || TILES.radar;
    tileLayerRef.current = L.tileLayer(cfg.url, { ...cfg.options, zIndex: 1 }).addTo(map);

    if (cfg.referenceUrl) {
      referenceLayerRef.current = L.tileLayer(cfg.referenceUrl, {
        maxZoom: cfg.options.maxZoom,
        zIndex: 15,
      }).addTo(map);
    }

    // Radar precipitation overlay
    if (mode === 'radar' && radarPath) {
      if (radarLayerRef.current) {
        try {
          map.removeLayer(radarLayerRef.current);
        } catch (e) {}
      }
      const rLayer = L.tileLayer(`https://tilecache.rainviewer.com${radarPath}/256/{z}/{x}/{y}/2/1_1.png`, {
        opacity: 0.85,
        zIndex: 30,
        maxNativeZoom: 7,
        maxZoom: 18,
      }).addTo(map);
      radarLayerRef.current = rLayer;
    } else if (radarLayerRef.current) {
      try {
        map.removeLayer(radarLayerRef.current);
      } catch (e) {}
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
      map.flyTo([city.lat, city.lon], 9, { duration: 1.4 });
    }
  }, [city]);

  return (
    <div className="realMapCard">
      <header className="mapHeader">
        <div className="mapTitle">
          <h3>Live Weather Map</h3>
        </div>
        <div className="mapControls">
          <button
            className="mapIconBtn"
            onClick={onOpenMap}
            aria-label="Open full map"
            title="Open full map"
          >
            <Maximize2 size={16} />
          </button>
        </div>
      </header>

      {/* Layer selector tabs: Radar | Temperature | Clouds | Wind */}
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
      <div className="mapViewportWrapper" onClick={onOpenMap} style={{ cursor: 'pointer' }}>
        <div ref={containerRef} className="leafletMapCanvas" />

        {/* Bottom scale gradient bar: Light [===gradient===] Heavy */}
        <div className="mapBottomBar">
          <span className="scaleWord">Light</span>
          <div className="scaleGradient" />
          <span className="scaleWord">Heavy</span>
          <button className="mapExpandIcon" onClick={(e) => { e.stopPropagation(); onOpenMap?.(); }} title="Expand Map">
            <Maximize2 size={13} />
          </button>
        </div>
      </div>
    </div>
  );
}
