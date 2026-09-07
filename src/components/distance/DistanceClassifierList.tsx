import React, { useState, useMemo } from 'react';
import { ClassifiedLocation, DistanceCategory } from '../../types';
import {
  MapPin,
  Search,
  CheckCircle2,
  AlertTriangle,
  Compass,
  ArrowUpDown,
  Trash2,
  Crosshair,
  SlidersHorizontal,
} from 'lucide-react';

interface DistanceClassifierListProps {
  locations: ClassifiedLocation[];
  selectedLocationId: string | null;
  onSelectLocation: (loc: ClassifiedLocation) => void;
  onDeleteLocation?: (id: string) => void;
  isDarkTheme?: boolean;
}

export const DistanceClassifierList: React.FC<DistanceClassifierListProps> = ({
  locations,
  selectedLocationId,
  onSelectLocation,
  onDeleteLocation,
  isDarkTheme = false,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeCategoryFilter, setActiveCategoryFilter] = useState<string>('ALL');
  const [sortOrder, setSortOrder] = useState<'ASC' | 'DESC'>('ASC');

  // Tier counts
  const categoryCounts = useMemo(() => {
    return {
      total: locations.length,
      usual: locations.filter((l) => l.categoryClassification === 'Usual distance').length,
      medium: locations.filter((l) => l.categoryClassification === 'Medium distance').length,
      far: locations.filter((l) => l.categoryClassification === 'Far distance').length,
      high: locations.filter((l) => l.categoryClassification === 'High distance').length,
    };
  }, [locations]);

  // Filtered & Sorted locations
  const displayedLocations = useMemo(() => {
    let result = locations.filter((loc) => {
      const matchesCategory =
        activeCategoryFilter === 'ALL' || loc.categoryClassification === activeCategoryFilter;

      const matchesSearch =
        searchTerm.trim() === '' ||
        loc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        loc.category?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        loc.address?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        loc.displayLabel.toLowerCase().includes(searchTerm.toLowerCase());

      return matchesCategory && matchesSearch;
    });

    result.sort((a, b) => {
      if (sortOrder === 'ASC') {
        return a.distanceKm - b.distanceKm;
      }
      return b.distanceKm - a.distanceKm;
    });

    return result;
  }, [locations, activeCategoryFilter, searchTerm, sortOrder]);

  return (
    <div className="flex flex-col h-full space-y-4">
      {/* Category Metric Quick-Filters */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {/* Usual distance (0–10 km) - Green */}
        <button
          onClick={() =>
            setActiveCategoryFilter(
              activeCategoryFilter === 'Usual distance' ? 'ALL' : 'Usual distance'
            )
          }
          className={`p-3 rounded-xl border text-left transition-all relative overflow-hidden ${
            activeCategoryFilter === 'Usual distance'
              ? 'ring-2 ring-emerald-500 bg-emerald-500/10 border-emerald-500/40'
              : isDarkTheme
              ? 'bg-slate-900/60 border-slate-800 hover:border-emerald-500/30'
              : 'bg-white border-slate-200 hover:border-emerald-500/30'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
              0–10 km
            </span>
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-slate-100 mt-0.5">
            {categoryCounts.usual}
          </div>
          <div className="text-[11px] font-bold text-emerald-700 dark:text-emerald-400 truncate">
            Usual distance
          </div>
        </button>

        {/* Medium distance (10–20 km) - Yellow */}
        <button
          onClick={() =>
            setActiveCategoryFilter(
              activeCategoryFilter === 'Medium distance' ? 'ALL' : 'Medium distance'
            )
          }
          className={`p-3 rounded-xl border text-left transition-all relative overflow-hidden ${
            activeCategoryFilter === 'Medium distance'
              ? 'ring-2 ring-yellow-500 bg-yellow-500/10 border-yellow-500/40'
              : isDarkTheme
              ? 'bg-slate-900/60 border-slate-800 hover:border-yellow-500/30'
              : 'bg-white border-slate-200 hover:border-yellow-500/30'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-yellow-600 dark:text-yellow-400">
              10–20 km
            </span>
            <span className="w-2 h-2 rounded-full bg-yellow-500" />
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-slate-100 mt-0.5">
            {categoryCounts.medium}
          </div>
          <div className="text-[11px] font-bold text-yellow-700 dark:text-yellow-400 truncate">
            Medium distance
          </div>
        </button>

        {/* Far distance (20–30 km) - Orange */}
        <button
          onClick={() =>
            setActiveCategoryFilter(
              activeCategoryFilter === 'Far distance' ? 'ALL' : 'Far distance'
            )
          }
          className={`p-3 rounded-xl border text-left transition-all relative overflow-hidden ${
            activeCategoryFilter === 'Far distance'
              ? 'ring-2 ring-orange-500 bg-orange-500/10 border-orange-500/40'
              : isDarkTheme
              ? 'bg-slate-900/60 border-slate-800 hover:border-orange-500/30'
              : 'bg-white border-slate-200 hover:border-orange-500/30'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-orange-600 dark:text-orange-400">
              20–30 km
            </span>
            <span className="w-2 h-2 rounded-full bg-orange-500" />
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-slate-100 mt-0.5">
            {categoryCounts.far}
          </div>
          <div className="text-[11px] font-bold text-orange-700 dark:text-orange-400 truncate">
            Far distance
          </div>
        </button>

        {/* High distance (>30 km) - Red */}
        <button
          onClick={() =>
            setActiveCategoryFilter(
              activeCategoryFilter === 'High distance' ? 'ALL' : 'High distance'
            )
          }
          className={`p-3 rounded-xl border text-left transition-all relative overflow-hidden ${
            activeCategoryFilter === 'High distance'
              ? 'ring-2 ring-rose-500 bg-rose-500/10 border-rose-500/40'
              : isDarkTheme
              ? 'bg-slate-900/60 border-slate-800 hover:border-rose-500/30'
              : 'bg-white border-slate-200 hover:border-rose-500/30'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-rose-600 dark:text-rose-400">
              &gt; 30 km
            </span>
            <span className="w-2 h-2 rounded-full bg-rose-500" />
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-slate-100 mt-0.5">
            {categoryCounts.high}
          </div>
          <div className="text-[11px] font-bold text-rose-700 dark:text-rose-400 truncate">
            High distance
          </div>
        </button>
      </div>

      {/* Search and Sort Toolbar */}
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search locations, categories, or distances..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className={`w-full pl-9 pr-3 py-2 rounded-xl text-xs border transition focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
              isDarkTheme
                ? 'bg-slate-900/90 border-slate-800 text-slate-100 placeholder:text-slate-500'
                : 'bg-white border-slate-200 text-slate-800 placeholder:text-slate-400'
            }`}
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              ×
            </button>
          )}
        </div>

        <button
          onClick={() => setSortOrder(sortOrder === 'ASC' ? 'DESC' : 'ASC')}
          title={sortOrder === 'ASC' ? 'Sorted nearest first' : 'Sorted farthest first'}
          className={`px-3 py-2 rounded-xl border text-xs font-bold flex items-center gap-1.5 transition ${
            isDarkTheme
              ? 'bg-slate-900 border-slate-800 text-slate-300 hover:bg-slate-800'
              : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
          }`}
        >
          <ArrowUpDown className="w-3.5 h-3.5 text-cyan-500" />
          <span>{sortOrder === 'ASC' ? 'Nearest' : 'Farthest'}</span>
        </button>

        {activeCategoryFilter !== 'ALL' && (
          <button
            onClick={() => setActiveCategoryFilter('ALL')}
            className="px-2.5 py-2 rounded-xl border border-dashed border-slate-300 dark:border-slate-700 text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
          >
            Clear Filter
          </button>
        )}
      </div>

      {/* Locations List */}
      <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 max-h-[500px]">
        {displayedLocations.length === 0 ? (
          <div className="text-center py-12 px-4 border border-dashed rounded-2xl border-slate-300 dark:border-slate-800">
            <Compass className="w-8 h-8 text-slate-400 mx-auto mb-2 opacity-50" />
            <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              No locations found
            </p>
            <p className="text-xs text-slate-400 mt-1">
              Try adjusting your search query or category filter.
            </p>
          </div>
        ) : (
          displayedLocations.map((loc) => {
            const isSelected = selectedLocationId === loc.id;

            // Color specifics
            let badgeBg = 'bg-emerald-500/10 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border-emerald-500/30';
            let dotBg = 'bg-emerald-500';
            let barColor = 'bg-emerald-500';

            if (loc.categoryClassification === 'Medium distance') {
              badgeBg = 'bg-yellow-500/10 dark:bg-yellow-500/20 text-yellow-700 dark:text-yellow-400 border-yellow-500/30';
              dotBg = 'bg-yellow-500';
              barColor = 'bg-yellow-500';
            } else if (loc.categoryClassification === 'Far distance') {
              badgeBg = 'bg-orange-500/10 dark:bg-orange-500/20 text-orange-700 dark:text-orange-400 border-orange-500/30';
              dotBg = 'bg-orange-500';
              barColor = 'bg-orange-500';
            } else if (loc.categoryClassification === 'High distance') {
              badgeBg = 'bg-rose-500/10 dark:bg-rose-500/20 text-rose-700 dark:text-rose-400 border-rose-500/30';
              dotBg = 'bg-rose-500';
              barColor = 'bg-rose-500';
            }

            // Calculate progress bar percentage (0 to 40km visual cap)
            const progressPercent = Math.min(100, Math.max(5, (loc.distanceKm / 40) * 100));

            return (
              <div
                key={loc.id}
                onClick={() => onSelectLocation(loc)}
                className={`p-3.5 rounded-2xl border transition-all cursor-pointer relative group ${
                  isSelected
                    ? isDarkTheme
                      ? 'bg-slate-800/90 border-cyan-500 shadow-md ring-1 ring-cyan-500'
                      : 'bg-cyan-50/70 border-cyan-500 shadow-md ring-1 ring-cyan-500'
                    : isDarkTheme
                    ? 'bg-slate-900/60 border-slate-800 hover:border-slate-700 hover:bg-slate-800/40'
                    : 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/80'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3 min-w-0">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 border ${
                        isSelected
                          ? 'bg-cyan-500/20 border-cyan-500 text-cyan-500'
                          : isDarkTheme
                          ? 'bg-slate-800 border-slate-700 text-slate-300'
                          : 'bg-slate-100 border-slate-200 text-slate-600'
                      }`}
                    >
                      <MapPin className="w-4 h-4" />
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h4 className="text-sm font-extrabold text-slate-900 dark:text-slate-100 truncate">
                          {loc.name}
                        </h4>
                        {loc.category && (
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                            {loc.category}
                          </span>
                        )}
                      </div>

                      {loc.address && (
                        <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 truncate">
                          {loc.address}
                        </p>
                      )}

                      <div className="text-[10px] text-slate-400 font-mono mt-1">
                        {loc.latitude.toFixed(4)}°, {loc.longitude.toFixed(4)}°
                      </div>
                    </div>
                  </div>

                  {/* Primary Distance & Classification Badge */}
                  <div className="flex flex-col items-end shrink-0">
                    <span
                      className={`px-3 py-1 rounded-full border text-xs font-black flex items-center gap-1.5 shadow-sm ${badgeBg}`}
                    >
                      <span className={`w-2 h-2 rounded-full shrink-0 ${dotBg}`} />
                      {loc.displayLabel}
                    </span>

                    <span className="text-[10px] font-semibold text-slate-400 mt-1">
                      {loc.categoryClassification}
                    </span>
                  </div>
                </div>

                {/* Relative Distance Progress Line */}
                <div className="mt-3 pt-2.5 border-t border-slate-100 dark:border-slate-800/80 flex items-center gap-3">
                  <div className="flex-1 h-1.5 rounded-full bg-slate-100 dark:bg-slate-800 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${barColor}`}
                      style={{ width: `${progressPercent}%` }}
                    />
                  </div>
                  <span className="text-[10px] font-mono font-bold text-slate-400 shrink-0">
                    {loc.formattedDistance}
                  </span>

                  {onDeleteLocation && loc.id.startsWith('custom_') && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteLocation(loc.id);
                      }}
                      className="text-slate-400 hover:text-rose-500 transition p-1"
                      title="Remove custom location"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
