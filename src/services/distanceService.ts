import {
  ClassifiedLocation,
  DistanceCategory,
  GeoCoordinates,
  TargetLocation,
} from '../types';

/**
 * Earth radius in kilometers (WGS-84 mean radius)
 */
export const EARTH_RADIUS_KM = 6371;

/**
 * Calculates the great-circle distance between two points on a sphere
 * using the Haversine formula in kilometers.
 *
 * @param lat1 Latitude of first point in decimal degrees
 * @param lon1 Longitude of first point in decimal degrees
 * @param lat2 Latitude of second point in decimal degrees
 * @param lon2 Longitude of second point in decimal degrees
 * @returns Distance in kilometers rounded to 2 decimal places
 */
export function calculateHaversineDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  if (lat1 === lat2 && lon1 === lon2) {
    return 0;
  }

  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const lat1Rad = toRad(lat1);
  const lat2Rad = toRad(lat2);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const distance = EARTH_RADIUS_KM * c;

  return Math.round(distance * 100) / 100;
}

/**
 * Classifies a distance in kilometers strictly according to user rules:
 * - 0–10 km: "Usual distance" (Treat exactly 10 km as "Usual distance")
 * - More than 10 km and up to 20 km: "Medium distance" (Treat exactly 20 km as "Medium distance")
 * - More than 20 km and up to 30 km: "Far distance" (Treat exactly 30 km as "Far distance")
 * - More than 30 km: "High distance" (Treat distances greater than 30 km as "High distance")
 *
 * Indicators:
 * - Green for Usual distance
 * - Yellow for Medium distance
 * - Orange for Far distance
 * - Red for High distance
 */
export function classifyDistance(distanceKm: number): {
  category: DistanceCategory;
  colorIndicator: 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED';
  hexColor: string;
  bgClass: string;
  textClass: string;
  borderClass: string;
} {
  // 0–10 km: "Usual distance" (Treat exactly 10 km as "Usual distance")
  if (distanceKm <= 10) {
    return {
      category: 'Usual distance',
      colorIndicator: 'GREEN',
      hexColor: '#10B981',
      bgClass: 'bg-emerald-500/10 dark:bg-emerald-500/20',
      textClass: 'text-emerald-700 dark:text-emerald-400',
      borderClass: 'border-emerald-500/30',
    };
  }

  // More than 10 km and up to 20 km: "Medium distance" (Treat exactly 20 km as "Medium distance")
  if (distanceKm <= 20) {
    return {
      category: 'Medium distance',
      colorIndicator: 'YELLOW',
      hexColor: '#EAB308',
      bgClass: 'bg-yellow-500/10 dark:bg-yellow-500/20',
      textClass: 'text-yellow-700 dark:text-yellow-400',
      borderClass: 'border-yellow-500/30',
    };
  }

  // More than 20 km and up to 30 km: "Far distance" (Treat exactly 30 km as "Far distance")
  if (distanceKm <= 30) {
    return {
      category: 'Far distance',
      colorIndicator: 'ORANGE',
      hexColor: '#F97316',
      bgClass: 'bg-orange-500/10 dark:bg-orange-500/20',
      textClass: 'text-orange-700 dark:text-orange-400',
      borderClass: 'border-orange-500/30',
    };
  }

  // More than 30 km: "High distance"
  return {
    category: 'High distance',
    colorIndicator: 'RED',
    hexColor: '#EF4444',
    bgClass: 'bg-rose-500/10 dark:bg-rose-500/20',
    textClass: 'text-rose-700 dark:text-rose-400',
    borderClass: 'border-rose-500/30',
  };
}

/**
 * Formats distance display label according to specifications:
 * e.g.:
 * - 5 km — Usual distance
 * - 15 km — Medium distance
 * - 25 km — Far distance
 * - 35 km — High distance
 */
export function formatDistanceDisplay(distanceKm: number, category: DistanceCategory): {
  formattedDistance: string;
  displayLabel: string;
} {
  // If whole number, format as integer; otherwise 1 decimal place if < 100, else integer
  const isWhole = Math.abs(distanceKm - Math.round(distanceKm)) < 0.05;
  const formattedDistance = isWhole
    ? `${Math.round(distanceKm)} km`
    : `${distanceKm.toFixed(1)} km`;

  const displayLabel = `${formattedDistance} — ${category}`;

  return { formattedDistance, displayLabel };
}

/**
 * Classifies and sorts locations from nearest to farthest.
 */
export function classifyAndSortLocations(
  userCoords: GeoCoordinates,
  locations: TargetLocation[]
): ClassifiedLocation[] {
  const classified: ClassifiedLocation[] = locations.map((loc) => {
    const dist = calculateHaversineDistanceKm(
      userCoords.latitude,
      userCoords.longitude,
      loc.latitude,
      loc.longitude
    );

    const classification = classifyDistance(dist);
    const { formattedDistance, displayLabel } = formatDistanceDisplay(
      dist,
      classification.category
    );

    return {
      ...loc,
      distanceKm: dist,
      formattedDistance,
      displayLabel,
      categoryClassification: classification.category,
      colorIndicator: classification.colorIndicator,
      hexColor: classification.hexColor,
      bgClass: classification.bgClass,
      textClass: classification.textClass,
      borderClass: classification.borderClass,
    };
  });

  // Sort from nearest to farthest
  return classified.sort((a, b) => a.distanceKm - b.distanceKm);
}

/**
 * Computes destination coordinates given starting point, distance (km), and bearing (degrees).
 */
export function computeOffsetCoordinates(
  lat: number,
  lon: number,
  distanceKm: number,
  bearingDeg: number
): { latitude: number; longitude: number } {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const toDeg = (r: number) => (r * 180) / Math.PI;

  const δ = distanceKm / EARTH_RADIUS_KM;
  const θ = toRad(bearingDeg);
  const φ1 = toRad(lat);
  const λ1 = toRad(lon);

  const sinφ2 = Math.sin(φ1) * Math.cos(δ) + Math.cos(φ1) * Math.sin(δ) * Math.cos(θ);
  const φ2 = Math.asin(sinφ2);
  const y = Math.sin(θ) * Math.sin(δ) * Math.cos(φ1);
  const x = Math.cos(δ) - Math.sin(φ1) * Math.sin(φ2);
  const λ2 = λ1 + Math.atan2(y, x);

  return {
    latitude: Math.round(toDeg(φ2) * 10000) / 10000,
    longitude: Math.round(toDeg(λ2) * 10000) / 10000,
  };
}

/**
 * Generates an assortment of realistic nearby locations centered around the user's coordinates,
 * ensuring all four distance classification tiers are represented (including exact boundary markers:
 * exactly 10km, exactly 20km, exactly 30km, and >30km).
 */
export function generateSurroundingLocations(userLat: number, userLon: number): TargetLocation[] {
  const baseLocations: Array<{
    name: string;
    distanceKm: number;
    bearing: number;
    category: string;
    icon: string;
    address: string;
    description: string;
  }> = [
    // 0–10 km: "Usual distance"
    {
      name: 'Central Metro Hub',
      distanceKm: 2.4,
      bearing: 35,
      category: 'Transit Point',
      icon: 'Train',
      address: 'Line 1 Downtown Interchange',
      description: 'Primary public rapid transit connector',
    },
    {
      name: 'TrustPay Financial Branch & ATM',
      distanceKm: 5.0,
      bearing: 110,
      category: 'Financial Service',
      icon: 'Building2',
      address: 'Suite 400, Commercial Avenue',
      description: 'Cash deposit, biometric KYC kiosk, ATM',
    },
    {
      name: 'City Tech Park - Tower B',
      distanceKm: 7.8,
      bearing: 215,
      category: 'Commercial Hub',
      icon: 'Briefcase',
      address: 'Plot 12, Innovation Corridor',
      description: 'Enterprise office complexes and dining',
    },
    {
      name: 'Northgate Medical Center (Boundary Check)',
      distanceKm: 10.0, // EXACT 10 km boundary test
      bearing: 340,
      category: 'Healthcare',
      icon: 'Cross',
      address: '100 North Ring Expressway',
      description: 'Exact 10 km boundary test point (Usual distance)',
    },

    // More than 10 km and up to 20 km: "Medium distance"
    {
      name: 'Greenfield Shopping Pavilion',
      distanceKm: 13.5,
      bearing: 75,
      category: 'Retail & Commerce',
      icon: 'ShoppingBag',
      address: 'East Arterial Bypass',
      description: 'Multi-level shopping center and food court',
    },
    {
      name: 'Suburban Logistics Depot',
      distanceKm: 17.2,
      bearing: 160,
      category: 'Logistics',
      icon: 'Truck',
      address: 'Sector 44 Freight Zone',
      description: 'Regional fulfillment and parcel terminal',
    },
    {
      name: 'Apex Sports Arena (Boundary Check)',
      distanceKm: 20.0, // EXACT 20 km boundary test
      bearing: 290,
      category: 'Sports & Entertainment',
      icon: 'Trophy',
      address: 'Western Stadium Way',
      description: 'Exact 20 km boundary test point (Medium distance)',
    },

    // More than 20 km and up to 30 km: "Far distance"
    {
      name: 'Lakeview University Campus',
      distanceKm: 24.5,
      bearing: 130,
      category: 'Education',
      icon: 'GraduationCap',
      address: '400 Scholar Parkway',
      description: 'State university science and research center',
    },
    {
      name: 'Cyber Valley Tech Park',
      distanceKm: 27.8,
      bearing: 220,
      category: 'Technology Hub',
      icon: 'Cpu',
      address: 'Corridor 9, Cyber Zone',
      description: 'Tier-4 data centers and software campuses',
    },
    {
      name: 'Valley Ridge Golf Club (Boundary Check)',
      distanceKm: 30.0, // EXACT 30 km boundary test
      bearing: 310,
      category: 'Recreation',
      icon: 'Trees',
      address: 'Highland Ridge Highway',
      description: 'Exact 30 km boundary test point (Far distance)',
    },

    // More than 30 km: "High distance"
    {
      name: 'International Airport Terminal 3',
      distanceKm: 35.0,
      bearing: 15,
      category: 'Aviation',
      icon: 'Plane',
      address: 'Airport Expressway Km 35',
      description: 'Domestic and international flights departure',
    },
    {
      name: 'Coastal Industrial Port',
      distanceKm: 48.0,
      bearing: 185,
      category: 'Industrial',
      icon: 'Anchor',
      address: 'Deepwater Berth 4',
      description: 'Container shipping terminal and customs warehouse',
    },
    {
      name: 'Mountain Pass Observatory',
      distanceKm: 65.5,
      bearing: 335,
      category: 'Tourism & Science',
      icon: 'Mountain',
      address: 'Summit Ridge Road',
      description: 'High altitude meteorological radar & research station',
    },
  ];

  return baseLocations.map((item, index) => {
    const coords = computeOffsetCoordinates(userLat, userLon, item.distanceKm, item.bearing);
    return {
      id: `loc_${index + 1}_${Date.now()}`,
      name: item.name,
      category: item.category,
      address: item.address,
      description: item.description,
      latitude: coords.latitude,
      longitude: coords.longitude,
      icon: item.icon,
    };
  });
}

/**
 * Preset City Coordinates for testing and simulation
 */
export const PRESET_TEST_CITIES: Array<{
  name: string;
  country: string;
  coords: GeoCoordinates;
  description: string;
}> = [
  {
    name: 'Mumbai (Nariman Point)',
    country: 'India',
    coords: { latitude: 18.9256, longitude: 72.8242 },
    description: 'Financial district hub',
  },
  {
    name: 'Bengaluru (MG Road)',
    country: 'India',
    coords: { latitude: 12.9756, longitude: 77.6066 },
    description: 'Tech capital center',
  },
  {
    name: 'New Delhi (Connaught Place)',
    country: 'India',
    coords: { latitude: 28.6315, longitude: 77.2167 },
    description: 'Capital central business circle',
  },
  {
    name: 'London (Trafalgar Square)',
    country: 'UK',
    coords: { latitude: 51.508, longitude: -0.1281 },
    description: 'Central London',
  },
  {
    name: 'San Francisco (Market St)',
    country: 'USA',
    coords: { latitude: 37.7897, longitude: -122.4014 },
    description: 'Silicon Valley gateway',
  },
  {
    name: 'Singapore (Marina Bay)',
    country: 'Singapore',
    coords: { latitude: 1.2847, longitude: 103.861 },
    description: 'Global financial center',
  },
  {
    name: 'Tokyo (Shinjuku)',
    country: 'Japan',
    coords: { latitude: 35.6909, longitude: 139.7003 },
    description: 'Tokyo metropolitan center',
  },
];
