import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  ClassifiedLocation,
  GeoCoordinates,
  TargetLocation,
} from '../../types';
import { useGeolocation } from '../../hooks/useGeolocation';
import {
  classifyAndSortLocations,
  generateSurroundingLocations,
  PRESET_TEST_CITIES,
} from '../../services/distanceService';
import { DistanceClassifierMap } from './DistanceClassifierMap';
import { DistanceClassifierList } from './DistanceClassifierList';
import { AddLocationModal } from './AddLocationModal';
import {
  MapPin,
  Compass,
  Navigation,
  RefreshCw,
  Plus,
  AlertTriangle,
  CheckCircle2,
  Sliders,
  Layers,
  Map as MapIcon,
  List as ListIcon,
  Radio,
  Info,
  ChevronRight,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';

interface LocationDistanceClassifierViewProps {
  isDarkTheme?: boolean;
  onBackToDashboard?: () => void;
}

export const LocationDistanceClassifierView: React.FC<LocationDistanceClassifierViewProps> = ({
  isDarkTheme = false,
  onBackToDashboard,
}) => {
  const {
    coordinates: userCoords,
    status: geoStatus,
    errorMessage,
    isWatching,
    isSimulated,
    requestLocation,
    setSimulatedLocation,
    clearSimulation,
    retry,
  } = useGeolocation();

  // Storage for target locations
  const [locations, setLocations] = useState<TargetLocation[]>([]);
  const [selectedLocationId, setSelectedLocationId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'SPLIT' | 'MAP' | 'LIST'>('SPLIT');
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [selectedPresetCity, setSelectedPresetCity] = useState<string>('GPS');

  // Generate initial locations whenever user coordinates first become available or change substantially
  useEffect(() => {
    if (userCoords) {
      // Keep any custom user-added locations, and regenerate surrounding ones
      setLocations((prev) => {
        const customLocs = prev.filter((l) => l.id.startsWith('custom_'));
        const autoLocs = generateSurroundingLocations(
          userCoords.latitude,
          userCoords.longitude
        );
        return [...autoLocs, ...customLocs];
      });
    }
  }, [userCoords?.latitude, userCoords?.longitude]);

  // Compute classified locations sorted nearest to farthest
  const classifiedLocations = useMemo(() => {
    if (!userCoords || locations.length === 0) return [];
    return classifyAndSortLocations(userCoords, locations);
  }, [userCoords, locations]);

  // Add custom location
  const handleAddLocation = useCallback((newLoc: TargetLocation) => {
    setLocations((prev) => [newLoc, ...prev]);
    setSelectedLocationId(newLoc.id);
  }, []);

  // Delete custom location
  const handleDeleteLocation = useCallback((id: string) => {
    setLocations((prev) => prev.filter((l) => l.id !== id));
    if (selectedLocationId === id) {
      setSelectedLocationId(null);
    }
  }, [selectedLocationId]);

  // Select a preset city
  const handleSelectPreset = (preset: (typeof PRESET_TEST_CITIES)[0]) => {
    setSelectedPresetCity(preset.name);
    setSimulatedLocation(preset.coords);
  };

  // Reset to default surrounding locations
  const handleResetLocations = () => {
    if (userCoords) {
      setLocations(generateSurroundingLocations(userCoords.latitude, userCoords.longitude));
      setSelectedLocationId(null);
    }
  };

  return (
    <div className="flex flex-col h-full space-y-5">
      {/* 1. Header Toolbar */}
      <div
        className={`p-5 rounded-3xl border shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4 ${
          isDarkTheme ? 'bg-slate-900/80 border-slate-800' : 'bg-white border-slate-200'
        }`}
      >
        <div className="flex items-start gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center text-white shadow-md shadow-cyan-500/20 shrink-0">
            <Compass className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
                Location-Based Distance Classifier
              </h2>
              <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 border border-cyan-500/20">
                Haversine Formula
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              Calculates real-time distances in kilometers and classifies into 4 standard categories.
            </p>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* View Mode Switcher */}
          <div
            className={`p-1 rounded-xl border flex items-center gap-1 text-xs ${
              isDarkTheme ? 'bg-slate-800/80 border-slate-700' : 'bg-slate-100 border-slate-200'
            }`}
          >
            <button
              onClick={() => setViewMode('SPLIT')}
              className={`px-2.5 py-1.5 rounded-lg font-bold flex items-center gap-1.5 transition ${
                viewMode === 'SPLIT'
                  ? 'bg-cyan-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
              }`}
              title="Split view (Map + List)"
            >
              <Layers className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Split</span>
            </button>
            <button
              onClick={() => setViewMode('MAP')}
              className={`px-2.5 py-1.5 rounded-lg font-bold flex items-center gap-1.5 transition ${
                viewMode === 'MAP'
                  ? 'bg-cyan-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
              }`}
              title="Map only"
            >
              <MapIcon className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Map</span>
            </button>
            <button
              onClick={() => setViewMode('LIST')}
              className={`px-2.5 py-1.5 rounded-lg font-bold flex items-center gap-1.5 transition ${
                viewMode === 'LIST'
                  ? 'bg-cyan-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
              }`}
              title="List only"
            >
              <ListIcon className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">List</span>
            </button>
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            disabled={!userCoords}
            className="px-3 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-700 disabled:opacity-50 text-white font-bold text-xs flex items-center gap-1.5 shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            <span>Add Location</span>
          </button>

          <button
            onClick={retry}
            className={`p-2 rounded-xl border transition ${
              isDarkTheme
                ? 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'
                : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-100'
            }`}
            title="Refresh GPS Coordinates"
          >
            <RefreshCw className="w-4 h-4 text-cyan-500" />
          </button>

          {onBackToDashboard && (
            <button
              onClick={onBackToDashboard}
              className={`px-3 py-2 rounded-xl border text-xs font-bold transition ${
                isDarkTheme
                  ? 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'
                  : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-100'
              }`}
            >
              Back to TrustPay
            </button>
          )}
        </div>
      </div>

      {/* 2. Geolocation Status & Live Location Bar */}
      <div
        className={`px-5 py-3.5 rounded-2xl border flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs ${
          isDarkTheme ? 'bg-slate-900/60 border-slate-800' : 'bg-slate-50 border-slate-200'
        }`}
      >
        <div className="flex items-center gap-2.5 flex-wrap">
          <div className="flex items-center gap-1.5">
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                geoStatus === 'LOCATED'
                  ? isSimulated
                    ? 'bg-amber-500'
                    : 'bg-emerald-500 animate-pulse'
                  : geoStatus === 'DETECTING'
                  ? 'bg-cyan-500 animate-ping'
                  : 'bg-rose-500'
              }`}
            />
            <span className="font-bold text-slate-700 dark:text-slate-300">
              {geoStatus === 'LOCATED'
                ? isSimulated
                  ? 'Simulated Coordinates'
                  : 'Live Device GPS'
                : geoStatus === 'DETECTING'
                ? 'Detecting Location...'
                : 'Location Error'}
            </span>
          </div>

          {userCoords && (
            <div className="font-mono text-slate-500 dark:text-slate-400 bg-slate-200/60 dark:bg-slate-800/80 px-2.5 py-1 rounded-lg">
              Lat: <strong>{userCoords.latitude.toFixed(5)}°</strong>, Lon:{' '}
              <strong>{userCoords.longitude.toFixed(5)}°</strong>
              {userCoords.accuracy && (
                <span className="ml-1 text-[10px] text-slate-400">
                  (±{Math.round(userCoords.accuracy)}m)
                </span>
              )}
            </div>
          )}

          {isWatching && !isSimulated && (
            <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
              <Radio className="w-3 h-3 animate-pulse" /> Live Watching
            </span>
          )}
        </div>

        {/* City Presets Dropdown/Chips for easy testing */}
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-slate-400 font-semibold">Test Presets:</span>
          <div className="flex items-center gap-1.5 flex-wrap">
            {PRESET_TEST_CITIES.slice(0, 4).map((city) => (
              <button
                key={city.name}
                onClick={() => handleSelectPreset(city)}
                className={`px-2 py-1 rounded-lg text-[11px] font-bold border transition ${
                  selectedPresetCity === city.name && isSimulated
                    ? 'bg-cyan-600 text-white border-cyan-600'
                    : isDarkTheme
                    ? 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'
                    : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-100'
                }`}
              >
                {city.name.split(' ')[0]}
              </button>
            ))}

            {isSimulated && (
              <button
                onClick={clearSimulation}
                className="px-2 py-1 rounded-lg text-[11px] font-bold text-cyan-600 dark:text-cyan-400 hover:underline"
              >
                Use Real GPS
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 3. Graceful Error & Permission Denial Banner */}
      {(geoStatus === 'DENIED' || geoStatus === 'UNAVAILABLE' || geoStatus === 'TIMEOUT') && (
        <div className="p-4 rounded-2xl border bg-amber-500/10 border-amber-500/30 text-amber-900 dark:text-amber-200 space-y-3 animate-in fade-in">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
            <div>
              <h4 className="text-sm font-bold">
                {geoStatus === 'DENIED'
                  ? 'Location Access Denied'
                  : 'Unable to Detect Current GPS Location'}
              </h4>
              <p className="text-xs text-amber-800 dark:text-amber-300 mt-1 leading-relaxed">
                {errorMessage ||
                  'The browser could not retrieve your physical GPS coordinates. To test the distance calculations, you can retry permissions or click any preset city below.'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 pt-1 flex-wrap">
            <button
              onClick={retry}
              className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition flex items-center gap-1.5"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Retry Permission
            </button>
            <span className="text-xs font-medium text-amber-700 dark:text-amber-300">
              Or pick a test location:
            </span>
            {PRESET_TEST_CITIES.map((city) => (
              <button
                key={city.name}
                onClick={() => handleSelectPreset(city)}
                className="px-2.5 py-1 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-900 dark:text-amber-200 text-xs font-bold transition"
              >
                {city.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 4. Strict Classification Rules Banner */}
      <div
        className={`p-4 rounded-2xl border ${
          isDarkTheme ? 'bg-slate-900/40 border-slate-800' : 'bg-slate-50 border-slate-200'
        }`}
      >
        <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2 flex items-center justify-between">
          <span className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-cyan-500" />
            Distance Classification Hierarchy & Color Indicators
          </span>
          <span className="text-[10px] font-mono text-slate-400">UNIT: KILOMETERS (KM)</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          {/* Usual distance (0–10 km) */}
          <div className="p-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10">
            <div className="flex items-center justify-between font-bold text-emerald-700 dark:text-emerald-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                Usual distance
              </span>
              <span className="font-mono text-[11px]">0 – 10 km</span>
            </div>
            <p className="text-[11px] text-emerald-800 dark:text-emerald-300 mt-1">
              Treats <strong>≤ 10 km</strong> as Usual distance (Green).
            </p>
          </div>

          {/* Medium distance (10–20 km) */}
          <div className="p-3 rounded-xl border border-yellow-500/30 bg-yellow-500/10">
            <div className="flex items-center justify-between font-bold text-yellow-700 dark:text-yellow-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-yellow-500" />
                Medium distance
              </span>
              <span className="font-mono text-[11px]">&gt;10 – 20 km</span>
            </div>
            <p className="text-[11px] text-yellow-800 dark:text-yellow-300 mt-1">
              Treats <strong>&gt;10 and ≤ 20 km</strong> as Medium (Yellow).
            </p>
          </div>

          {/* Far distance (20–30 km) */}
          <div className="p-3 rounded-xl border border-orange-500/30 bg-orange-500/10">
            <div className="flex items-center justify-between font-bold text-orange-700 dark:text-orange-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-orange-500" />
                Far distance
              </span>
              <span className="font-mono text-[11px]">&gt;20 – 30 km</span>
            </div>
            <p className="text-[11px] text-orange-800 dark:text-orange-300 mt-1">
              Treats <strong>&gt;20 and ≤ 30 km</strong> as Far (Orange).
            </p>
          </div>

          {/* High distance (>30 km) */}
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10">
            <div className="flex items-center justify-between font-bold text-rose-700 dark:text-rose-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500" />
                High distance
              </span>
              <span className="font-mono text-[11px]">&gt; 30 km</span>
            </div>
            <p className="text-[11px] text-rose-800 dark:text-rose-300 mt-1">
              Treats <strong>distances &gt; 30 km</strong> as High (Red).
            </p>
          </div>
        </div>
      </div>

      {/* 5. Main Responsive Workspace: Split, Map Only, or List Only */}
      <div className="flex-1 min-h-[500px]">
        {viewMode === 'SPLIT' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-full">
            {/* Map Column */}
            <div className="lg:col-span-6 min-h-[420px] h-[520px]">
              <DistanceClassifierMap
                userCoords={userCoords}
                locations={classifiedLocations}
                selectedLocationId={selectedLocationId}
                onSelectLocation={(loc) => setSelectedLocationId(loc.id)}
                isDarkTheme={isDarkTheme}
              />
            </div>

            {/* List Column */}
            <div className="lg:col-span-6 h-[520px]">
              <DistanceClassifierList
                locations={classifiedLocations}
                selectedLocationId={selectedLocationId}
                onSelectLocation={(loc) => setSelectedLocationId(loc.id)}
                onDeleteLocation={handleDeleteLocation}
                isDarkTheme={isDarkTheme}
              />
            </div>
          </div>
        )}

        {viewMode === 'MAP' && (
          <div className="w-full h-[620px]">
            <DistanceClassifierMap
              userCoords={userCoords}
              locations={classifiedLocations}
              selectedLocationId={selectedLocationId}
              onSelectLocation={(loc) => setSelectedLocationId(loc.id)}
              isDarkTheme={isDarkTheme}
            />
          </div>
        )}

        {viewMode === 'LIST' && (
          <div className="w-full max-w-4xl mx-auto">
            <DistanceClassifierList
              locations={classifiedLocations}
              selectedLocationId={selectedLocationId}
              onSelectLocation={(loc) => setSelectedLocationId(loc.id)}
              onDeleteLocation={handleDeleteLocation}
              isDarkTheme={isDarkTheme}
            />
          </div>
        )}
      </div>

      {/* Modal: Add Location */}
      {showAddModal && (
        <AddLocationModal
          userCoords={userCoords}
          isDarkTheme={isDarkTheme}
          onClose={() => setShowAddModal(false)}
          onAddLocation={handleAddLocation}
        />
      )}
    </div>
  );
};
