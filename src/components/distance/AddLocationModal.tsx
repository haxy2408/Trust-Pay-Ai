import React, { useState, useMemo } from 'react';
import { GeoCoordinates, TargetLocation } from '../../types';
import {
  calculateHaversineDistanceKm,
  classifyDistance,
  formatDistanceDisplay,
} from '../../services/distanceService';
import { MapPin, X, Plus, Sparkles, Navigation, AlertCircle } from 'lucide-react';

interface AddLocationModalProps {
  userCoords: GeoCoordinates | null;
  isDarkTheme?: boolean;
  onClose: () => void;
  onAddLocation: (location: TargetLocation) => void;
}

export const AddLocationModal: React.FC<AddLocationModalProps> = ({
  userCoords,
  isDarkTheme = false,
  onClose,
  onAddLocation,
}) => {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Custom Point');
  const [address, setAddress] = useState('');
  const [latInput, setLatInput] = useState('');
  const [lonInput, setLonInput] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Real-time live distance preview
  const livePreview = useMemo(() => {
    const lat = parseFloat(latInput);
    const lon = parseFloat(lonInput);

    if (isNaN(lat) || isNaN(lon) || !userCoords) return null;
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;

    const dist = calculateHaversineDistanceKm(
      userCoords.latitude,
      userCoords.longitude,
      lat,
      lon
    );
    const classification = classifyDistance(dist);
    const { formattedDistance, displayLabel } = formatDistanceDisplay(
      dist,
      classification.category
    );

    return {
      distanceKm: dist,
      formattedDistance,
      displayLabel,
      classification,
    };
  }, [latInput, lonInput, userCoords]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError('Please provide a name for the location.');
      return;
    }

    const lat = parseFloat(latInput);
    const lon = parseFloat(lonInput);

    if (isNaN(lat) || lat < -90 || lat > 90) {
      setError('Please enter a valid latitude between -90 and 90.');
      return;
    }

    if (isNaN(lon) || lon < -180 || lon > 180) {
      setError('Please enter a valid longitude between -180 and 180.');
      return;
    }

    const newLocation: TargetLocation = {
      id: `custom_${Date.now()}`,
      name: name.trim(),
      category: category.trim() || 'Custom Point',
      address: address.trim() || undefined,
      latitude: lat,
      longitude: lon,
    };

    onAddLocation(newLocation);
    onClose();
  };

  // Quick preset offsets from user
  const setQuickOffset = (km: number) => {
    if (!userCoords) return;
    // ~1 deg lat ~ 111 km
    const latOffset = km / 111.0;
    setLatInput((userCoords.latitude + latOffset).toFixed(5));
    setLonInput(userCoords.longitude.toFixed(5));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in">
      <div
        className={`w-full max-w-md rounded-3xl border shadow-2xl overflow-hidden ${
          isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'
        }`}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-600 dark:text-cyan-400">
              <Plus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-extrabold">Add Custom Location</h3>
              <p className="text-xs text-slate-400">Calculate distance & classification</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-400 text-xs font-semibold flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
              Location Name
            </label>
            <input
              type="text"
              placeholder="e.g., Downtown Partner Hub"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-medium focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                isDarkTheme
                  ? 'bg-slate-800/80 border-slate-700 text-slate-100 placeholder:text-slate-500'
                  : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400'
              }`}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
                Latitude (-90 to 90)
              </label>
              <input
                type="number"
                step="any"
                placeholder="e.g., 12.9716"
                value={latInput}
                onChange={(e) => setLatInput(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                  isDarkTheme
                    ? 'bg-slate-800/80 border-slate-700 text-slate-100 placeholder:text-slate-500'
                    : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400'
                }`}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
                Longitude (-180 to 180)
              </label>
              <input
                type="number"
                step="any"
                placeholder="e.g., 77.5946"
                value={lonInput}
                onChange={(e) => setLonInput(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-mono focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                  isDarkTheme
                    ? 'bg-slate-800/80 border-slate-700 text-slate-100 placeholder:text-slate-500'
                    : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400'
                }`}
                required
              />
            </div>
          </div>

          {/* Quick Target Distance Buttons */}
          {userCoords && (
            <div>
              <div className="text-[11px] font-semibold text-slate-400 mb-1.5 flex items-center justify-between">
                <span>Quick Distance Presets (from user):</span>
                <Sparkles className="w-3.5 h-3.5 text-cyan-500" />
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                <button
                  type="button"
                  onClick={() => setQuickOffset(5)}
                  className="px-2 py-1.5 rounded-lg border border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 text-[10px] font-bold"
                >
                  ~5 km (Usual)
                </button>
                <button
                  type="button"
                  onClick={() => setQuickOffset(15)}
                  className="px-2 py-1.5 rounded-lg border border-yellow-500/30 bg-yellow-500/10 text-yellow-700 dark:text-yellow-400 text-[10px] font-bold"
                >
                  ~15 km (Medium)
                </button>
                <button
                  type="button"
                  onClick={() => setQuickOffset(25)}
                  className="px-2 py-1.5 rounded-lg border border-orange-500/30 bg-orange-500/10 text-orange-700 dark:text-orange-400 text-[10px] font-bold"
                >
                  ~25 km (Far)
                </button>
                <button
                  type="button"
                  onClick={() => setQuickOffset(35)}
                  className="px-2 py-1.5 rounded-lg border border-rose-500/30 bg-rose-500/10 text-rose-700 dark:text-rose-400 text-[10px] font-bold"
                >
                  ~35 km (High)
                </button>
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
                Category
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-medium focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                  isDarkTheme
                    ? 'bg-slate-800/80 border-slate-700 text-slate-100'
                    : 'bg-slate-50 border-slate-200 text-slate-900'
                }`}
              >
                <option value="Custom Point">Custom Point</option>
                <option value="Merchant Point">Merchant Point</option>
                <option value="Branch / Office">Branch / Office</option>
                <option value="ATM / Cash Point">ATM / Cash Point</option>
                <option value="Warehouse / Hub">Warehouse / Hub</option>
                <option value="Landmark">Landmark</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
                Address / Note
              </label>
              <input
                type="text"
                placeholder="Optional address"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border text-xs font-medium focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                  isDarkTheme
                    ? 'bg-slate-800/80 border-slate-700 text-slate-100 placeholder:text-slate-500'
                    : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400'
                }`}
              />
            </div>
          </div>

          {/* Live Classification Preview Card */}
          {livePreview && (
            <div
              className={`p-3.5 rounded-2xl border flex items-center justify-between ${livePreview.classification.bgClass} ${livePreview.classification.borderClass}`}
            >
              <div>
                <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Calculated Distance & Classification
                </div>
                <div className="text-sm font-black text-slate-900 dark:text-slate-100 mt-0.5">
                  {livePreview.displayLabel}
                </div>
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-black border shadow-sm ${livePreview.classification.bgClass} ${livePreview.classification.textClass} ${livePreview.classification.borderClass}`}
              >
                {livePreview.classification.category}
              </span>
            </div>
          )}

          {/* Actions */}
          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-700 text-white text-xs font-bold flex items-center gap-2 shadow-md transition"
            >
              <Plus className="w-4 h-4" />
              Add to Locations
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
