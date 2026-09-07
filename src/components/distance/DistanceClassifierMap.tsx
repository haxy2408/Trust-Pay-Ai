import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { ClassifiedLocation, GeoCoordinates } from '../../types';

interface DistanceClassifierMapProps {
  userCoords: GeoCoordinates | null;
  locations: ClassifiedLocation[];
  selectedLocationId?: string | null;
  onSelectLocation?: (location: ClassifiedLocation) => void;
  onMapClick?: (lat: number, lon: number) => void;
  isDarkTheme?: boolean;
}

export const DistanceClassifierMap: React.FC<DistanceClassifierMapProps> = ({
  userCoords,
  locations,
  selectedLocationId,
  onSelectLocation,
  onMapClick,
  isDarkTheme = false,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const layerGroupRef = useRef<L.LayerGroup | null>(null);
  const circleGroupRef = useRef<L.LayerGroup | null>(null);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;
    if (mapInstanceRef.current) return; // already created

    const initialLat = userCoords?.latitude ?? 12.9716;
    const initialLon = userCoords?.longitude ?? 77.5946;

    const map = L.map(mapContainerRef.current, {
      center: [initialLat, initialLon],
      zoom: 11,
      zoomControl: true,
      attributionControl: false,
    });

    const tileUrl = isDarkTheme
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png'
      : 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

    L.tileLayer(tileUrl, {
      maxZoom: 19,
    }).addTo(map);

    const circleGroup = L.layerGroup().addTo(map);
    const layerGroup = L.layerGroup().addTo(map);

    circleGroupRef.current = circleGroup;
    layerGroupRef.current = layerGroup;
    mapInstanceRef.current = map;

    // Handle Map click to set location
    map.on('click', (e: L.LeafletMouseEvent) => {
      if (onMapClick) {
        onMapClick(e.latlng.lat, e.latlng.lng);
      }
    });

    return () => {
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // Update center, circles, and markers
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layerGroup = layerGroupRef.current;
    const circleGroup = circleGroupRef.current;
    if (!map || !layerGroup || !circleGroup) return;

    layerGroup.clearLayers();
    circleGroup.clearLayers();

    if (userCoords) {
      const userLat = userCoords.latitude;
      const userLon = userCoords.longitude;

      // 1. Render Concentric Distance Circles (10km, 20km, 30km)
      // 10km circle (Usual distance - Green)
      L.circle([userLat, userLon], {
        radius: 10000,
        color: '#10B981',
        weight: 2,
        dashArray: '4, 6',
        fillColor: '#10B981',
        fillOpacity: 0.08,
      })
        .bindTooltip('Usual distance: 0 – 10 km', { permanent: false, direction: 'top' })
        .addTo(circleGroup);

      // 20km circle (Medium distance - Yellow)
      L.circle([userLat, userLon], {
        radius: 20000,
        color: '#EAB308',
        weight: 2,
        dashArray: '6, 6',
        fillColor: '#EAB308',
        fillOpacity: 0.05,
      })
        .bindTooltip('Medium distance: 10 – 20 km', { permanent: false, direction: 'top' })
        .addTo(circleGroup);

      // 30km circle (Far distance - Orange)
      L.circle([userLat, userLon], {
        radius: 30000,
        color: '#F97316',
        weight: 2,
        dashArray: '8, 8',
        fillColor: '#F97316',
        fillOpacity: 0.03,
      })
        .bindTooltip('Far distance: 20 – 30 km', { permanent: false, direction: 'top' })
        .addTo(circleGroup);

      // 2. Render User Location Pin (Blue beacon with pulsing radar)
      const userIconHtml = `
        <div style="position: relative; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;">
          <div style="position: absolute; width: 28px; height: 28px; border-radius: 50%; background-color: rgba(6, 182, 212, 0.4); animation: ping 2s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
          <div style="position: relative; width: 16px; height: 16px; border-radius: 50%; background-color: #0284C7; border: 3px solid #FFFFFF; box-shadow: 0 2px 6px rgba(0,0,0,0.3);"></div>
        </div>
      `;

      const userIcon = L.divIcon({
        className: 'custom-user-pin',
        html: userIconHtml,
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      });

      const userMarker = L.marker([userLat, userLon], { icon: userIcon, zIndexOffset: 1000 }).addTo(
        layerGroup
      );
      userMarker.bindPopup(`
        <div style="font-family: inherit; padding: 4px;">
          <div style="font-weight: 800; font-size: 13px; color: #0284C7;">Your Current Location</div>
          <div style="font-size: 11px; color: #64748B; margin-top: 2px;">
            ${userLat.toFixed(4)}°, ${userLon.toFixed(4)}°
          </div>
          <div style="font-size: 10px; color: #10B981; margin-top: 4px; font-weight: 600;">
            Center point for distance calculations
          </div>
        </div>
      `);

      // 3. Render Each Location Marker with Color Pin
      locations.forEach((loc) => {
        let pinBg = '#10B981'; // Green (Usual)
        if (loc.categoryClassification === 'Medium distance') pinBg = '#EAB308'; // Yellow
        else if (loc.categoryClassification === 'Far distance') pinBg = '#F97316'; // Orange
        else if (loc.categoryClassification === 'High distance') pinBg = '#EF4444'; // Red

        const isSelected = selectedLocationId === loc.id;

        const pinHtml = `
          <div style="
            position: relative;
            cursor: pointer;
            width: ${isSelected ? '36px' : '30px'};
            height: ${isSelected ? '44px' : '38px'};
            transition: transform 0.2s;
            filter: drop-shadow(0 3px 6px rgba(0,0,0,0.25));
          ">
            <svg viewBox="0 0 24 32" width="100%" height="100%">
              <path d="M12 0C5.373 0 0 5.373 0 12c0 8.5 12 20 12 20s12-11.5 12-20c0-6.627-5.373-12-12-12z" fill="${pinBg}" stroke="#FFFFFF" stroke-width="1.5" />
              <circle cx="12" cy="11" r="5" fill="#FFFFFF" />
            </svg>
            <div style="
              position: absolute;
              top: ${isSelected ? '7px' : '6px'};
              left: 50%;
              transform: translateX(-50%);
              font-size: 9px;
              font-weight: 900;
              color: ${pinBg};
            ">
              ${loc.distanceKm <= 99 ? Math.round(loc.distanceKm) : '99+'}
            </div>
          </div>
        `;

        const markerIcon = L.divIcon({
          className: 'custom-location-pin',
          html: pinHtml,
          iconSize: [isSelected ? 36 : 30, isSelected ? 44 : 38],
          iconAnchor: [isSelected ? 18 : 15, isSelected ? 44 : 38],
          popupAnchor: [0, -40],
        });

        const marker = L.marker([loc.latitude, loc.longitude], { icon: markerIcon }).addTo(
          layerGroup
        );

        const popupContent = `
          <div style="font-family: inherit; min-width: 180px; padding: 4px;">
            <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 800; color: ${pinBg};">
              ${loc.displayLabel}
            </div>
            <div style="font-weight: 800; font-size: 14px; margin-top: 2px; color: #1E293B;">
              ${loc.name}
            </div>
            ${
              loc.category
                ? `<div style="font-size: 11px; color: #64748B; margin-top: 2px;">${loc.category}</div>`
                : ''
            }
            ${
              loc.address
                ? `<div style="font-size: 11px; color: #94A3B8; margin-top: 2px;">${loc.address}</div>`
                : ''
            }
            <div style="margin-top: 8px; padding-top: 6px; border-top: 1px solid #E2E8F0; font-size: 10px; color: #64748B; display: flex; justify-content: space-between;">
              <span>Distance: <strong>${loc.formattedDistance}</strong></span>
              <span style="font-weight: 700; color: ${pinBg};">${loc.categoryClassification}</span>
            </div>
          </div>
        `;

        marker.bindPopup(popupContent);

        marker.on('click', () => {
          if (onSelectLocation) {
            onSelectLocation(loc);
          }
        });

        if (isSelected) {
          marker.openPopup();
        }
      });
    }
  }, [userCoords, locations, selectedLocationId, onSelectLocation, isDarkTheme]);

  // Adjust view when selectedLocation changes
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !selectedLocationId) return;

    const loc = locations.find((l) => l.id === selectedLocationId);
    if (loc) {
      map.setView([loc.latitude, loc.longitude], 13, { animate: true });
    }
  }, [selectedLocationId, locations]);

  return (
    <div className="relative w-full h-full min-h-[380px] rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-inner">
      <div ref={mapContainerRef} className="w-full h-full min-h-[380px] z-0" />

      {/* Map Legend Overlay */}
      <div className="absolute bottom-3 left-3 z-[400] p-2.5 rounded-xl bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border border-slate-200 dark:border-slate-800 shadow-lg text-xs space-y-1.5 max-w-[260px]">
        <div className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
          Distance Classifications
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3 h-3 rounded-full bg-emerald-500 shrink-0" />
          <span className="text-[11px] font-semibold text-slate-700 dark:text-slate-300">
            0–10 km: <strong className="text-emerald-600 dark:text-emerald-400">Usual distance</strong>
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3 h-3 rounded-full bg-yellow-500 shrink-0" />
          <span className="text-[11px] font-semibold text-slate-700 dark:text-slate-300">
            10–20 km: <strong className="text-yellow-600 dark:text-yellow-400">Medium distance</strong>
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3 h-3 rounded-full bg-orange-500 shrink-0" />
          <span className="text-[11px] font-semibold text-slate-700 dark:text-slate-300">
            20–30 km: <strong className="text-orange-600 dark:text-orange-400">Far distance</strong>
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3 h-3 rounded-full bg-rose-500 shrink-0" />
          <span className="text-[11px] font-semibold text-slate-700 dark:text-slate-300">
            &gt; 30 km: <strong className="text-rose-600 dark:text-rose-400">High distance</strong>
          </span>
        </div>
      </div>
    </div>
  );
};
