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
        <div style="font-weight: bold; margin-bottom: 5px;">Visit Frequency</div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: #991b1b; margin-right: 5px; border-radius: 3px;"></div>
          <span>81-100%</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: #ef4444; margin-right: 5px; border-radius: 3px;"></div>
          <span>61-80%</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: #f97316; margin-right: 5px; border-radius: 3px;"></div>
          <span>41-60%</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 3px;">
          <div style="width: 20px; height: 20px; background: #eab308; margin-right: 5px; border-radius: 3px;"></div>
          <span>21-40%</span>
        </div>
        <div style="display: flex; align-items: center;">
          <div style="width: 20px; height: 20px; background: #22c55e; margin-right: 5px; border-radius: 3px;"></div>
          <span>1-20%</span>
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

  // Update heat layer when data changes + auto-fit to data bounds
  useEffect(() => {
    if (!mapInstanceRef.current) return;

    if (heatLayerRef.current) {
      mapInstanceRef.current.removeLayer(heatLayerRef.current);
      heatLayerRef.current = null;
    }

    const data = heatmapData && heatmapData.length > 0 ? heatmapData : [];

    if (data.length > 0) {
      // Auto-fit map to data bounds
      const lats = data.map(d => d[0]);
      const lngs = data.map(d => d[1]);
      const bounds = L.latLngBounds(
        [Math.min(...lats), Math.min(...lngs)],
        [Math.max(...lats), Math.max(...lngs)]
      );
      mapInstanceRef.current.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });

      const layer = (L as any).heatLayer(data, {
        radius: 35,
        blur: 22,
        maxZoom: 17,
        max: 1.0,
        minOpacity: 0.45,
        gradient: {
          0.2: '#22c55e',   // Very Low  - green
          0.4: '#eab308',   // Low       - yellow
          0.6: '#f97316',   // Medium    - orange
          0.8: '#ef4444',   // High      - red
          1.0: '#991b1b',   // Very High - darkred
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
