import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.heat';

declare module 'leaflet' {
  function heatLayer(
    latlngs: Array<[number, number, number]>,
    options?: any
  ): L.Layer;
}

interface HeatMapViewProps {
  title?: string;
  height?: string;
  heatmapData?: Array<[number, number, number]>; // [lat, lng, intensity]
  center?: [number, number];
  zoom?: number;
}

export function HeatMapView({
  title = 'Trip Activity Heatmap',
  height = '500px',
  heatmapData,
  center = [1.2966, 103.7764], // NUS default
  zoom = 15,
}: HeatMapViewProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const heatLayerRef = useRef<L.Layer | null>(null);

  // Initialize map
  useEffect(() => {
    if (!mapRef.current || mapInstanceRef.current) return;

    const map = L.map(mapRef.current).setView(center, zoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map);

    // Add legend
    const legend = (L.control as any)({ position: 'bottomright' });
    legend.onAdd = function () {
      const div = L.DomUtil.create('div', 'info legend');
      div.style.backgroundColor = 'white';
      div.style.padding = '10px';
      div.style.borderRadius = '5px';
      div.style.boxShadow = '0 0 15px rgba(0,0,0,0.2)';
      div.innerHTML = `
        <div style="font-weight: bold; margin-bottom: 5px;">Activity Level</div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: darkred; margin-right: 5px; border-radius: 3px;"></div>
          <span>Very High</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: red; margin-right: 5px; border-radius: 3px;"></div>
          <span>High</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: orange; margin-right: 5px; border-radius: 3px;"></div>
          <span>Medium</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: yellow; margin-right: 5px; border-radius: 3px;"></div>
          <span>Low</span>
        </div>
        <div style="display: flex; align-items: center;">
          <div style="width: 20px; height: 20px; background: green; margin-right: 5px; border-radius: 3px;"></div>
          <span>Very Low</span>
        </div>
      `;
      return div;
    };
    legend.addTo(map);

    mapInstanceRef.current = map;

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Update heat layer when data changes
  useEffect(() => {
    if (!mapInstanceRef.current) return;

    if (heatLayerRef.current) {
      mapInstanceRef.current.removeLayer(heatLayerRef.current);
      heatLayerRef.current = null;
    }

    const data = heatmapData && heatmapData.length > 0 ? heatmapData : [];

    if (data.length > 0) {
      const layer = (L as any).heatLayer(data, {
        radius: 25,
        blur: 15,
        maxZoom: 17,
        max: 1.0,
        gradient: {
          0.0: 'green',
          0.3: 'yellow',
          0.5: 'orange',
          0.7: 'red',
          1.0: 'darkred',
        },
      }).addTo(mapInstanceRef.current);
      heatLayerRef.current = layer;
    }
  }, [heatmapData]);

  const hasData = heatmapData && heatmapData.length > 0;

  return (
    <div className="w-full">
      {title && <h3 className="text-lg font-semibold mb-4">{title}</h3>}
      <div
        ref={mapRef}
        style={{ height, width: '100%' }}
        className="rounded-lg border border-gray-300 shadow-sm"
      />
      <p className="text-sm text-gray-500 mt-2">
        {hasData
          ? `Heatmap shows trip activity density across campus. ${heatmapData!.length} data points loaded.`
          : 'No trip coordinate data available. The heatmap will populate once trips with location data are recorded.'}
      </p>
    </div>
  );
}
