import { useState, useEffect, useCallback, useRef } from 'react';
import { GeoCoordinates, GeolocationStatus } from '../types';

interface UseGeolocationReturn {
  coordinates: GeoCoordinates | null;
  status: GeolocationStatus;
  errorMessage: string | null;
  isWatching: boolean;
  isSimulated: boolean;
  requestLocation: () => void;
  setSimulatedLocation: (coords: GeoCoordinates, label?: string) => void;
  clearSimulation: () => void;
  retry: () => void;
}

export function useGeolocation(): UseGeolocationReturn {
  const [coordinates, setCoordinates] = useState<GeoCoordinates | null>(null);
  const [status, setStatus] = useState<GeolocationStatus>('IDLE');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isWatching, setIsWatching] = useState<boolean>(false);
  const [isSimulated, setIsSimulated] = useState<boolean>(false);

  const watchIdRef = useRef<number | null>(null);

  const clearWatch = useCallback(() => {
    if (watchIdRef.current !== null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
      setIsWatching(false);
    }
  }, []);

  const handleSuccess = useCallback(
    (position: GeolocationPosition) => {
      // If user has explicitly overridden with simulated coordinates, do not overwrite unless reset
      if (isSimulated) return;

      const coords: GeoCoordinates = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy,
        altitude: position.coords.altitude,
        timestamp: position.timestamp,
      };

      setCoordinates(coords);
      setStatus('LOCATED');
      setErrorMessage(null);
    },
    [isSimulated]
  );

  const handleError = useCallback((error: GeolocationPositionError) => {
    let message = 'Unable to detect your location.';
    let newStatus: GeolocationStatus = 'UNAVAILABLE';

    switch (error.code) {
      case error.PERMISSION_DENIED:
        newStatus = 'DENIED';
        message =
          'Location access was denied. Please allow location access in your browser settings or select a simulated location.';
        break;
      case error.POSITION_UNAVAILABLE:
        newStatus = 'UNAVAILABLE';
        message =
          'Location signal is unavailable. Check your device GPS or internet connectivity, or try selecting a preset location.';
        break;
      case error.TIMEOUT:
        newStatus = 'TIMEOUT';
        message =
          'Location detection timed out. Please retry or pick a simulated location.';
        break;
      default:
        newStatus = 'UNAVAILABLE';
        message = error.message || 'An unexpected error occurred while detecting location.';
        break;
    }

    setStatus(newStatus);
    setErrorMessage(message);
  }, []);

  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setStatus('UNSUPPORTED');
      setErrorMessage('Geolocation is not supported by your browser.');
      return;
    }

    setStatus('DETECTING');
    setErrorMessage(null);
    setIsSimulated(false);

    // Initial query
    navigator.geolocation.getCurrentPosition(handleSuccess, handleError, {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
    });

    // Start watching position changes
    clearWatch();
    try {
      const watchId = navigator.geolocation.watchPosition(handleSuccess, handleError, {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 1000,
      });
      watchIdRef.current = watchId;
      setIsWatching(true);
    } catch {
      // Ignore watchPosition errors
    }
  }, [clearWatch, handleError, handleSuccess]);

  const setSimulatedLocation = useCallback(
    (coords: GeoCoordinates) => {
      clearWatch();
      setIsSimulated(true);
      setCoordinates(coords);
      setStatus('LOCATED');
      setErrorMessage(null);
    },
    [clearWatch]
  );

  const clearSimulation = useCallback(() => {
    setIsSimulated(false);
    requestLocation();
  }, [requestLocation]);

  const retry = useCallback(() => {
    requestLocation();
  }, [requestLocation]);

  // Request on initial mount
  useEffect(() => {
    requestLocation();
    return () => {
      clearWatch();
    };
  }, [requestLocation, clearWatch]);

  return {
    coordinates,
    status,
    errorMessage,
    isWatching,
    isSimulated,
    requestLocation,
    setSimulatedLocation,
    clearSimulation,
    retry,
  };
}
