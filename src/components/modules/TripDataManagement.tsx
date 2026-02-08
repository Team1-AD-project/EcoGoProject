import { useState, useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { fetchUserList, type User } from '@/services/userService';
import { fetchUserTrips, type TripDetail } from '@/services/tripService';
import { ChevronLeft, ChevronRight, Search, RefreshCw, MapPin } from 'lucide-react';

// Fix for default marker icons
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

export function TripDataManagement() {
  const mapRef = useRef<L.Map | null>(null);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const layerGroupRef = useRef<L.LayerGroup | null>(null);

  const [users, setUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [currentTrips, setCurrentTrips] = useState<TripDetail[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchId, setSearchId] = useState('');

  // Aggregated Stats State
  const [userStatsMap, setUserStatsMap] = useState<Record<string, { completed: number, carbon: number }>>({});

  // Pagination State
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const pageSize = 20;

  // Load Stats for Displayed Users (Per-User Fetch Strategy)
  useEffect(() => {
    if (users.length === 0) return;

    const loadPerUserStats = async () => {
      const statsMap: Record<string, { completed: number, carbon: number }> = {};

      // Fetch stats for each user in the current list
      await Promise.all(users.map(async (user) => {
        try {
          const trips = await fetchUserTrips(user.userid);
          let completed = 0;
          let carbon = 0;

          if (trips && trips.length > 0) {
            trips.forEach(t => {
              if ((t.carbonStatus || '').toLowerCase() === 'completed') {
                completed++;
                // Handle snake_case or camelCase
                const saved = Number((t as any).carbon_saved ?? t.carbonSaved ?? 0);
                carbon += saved;
              }
            });
          }
          statsMap[user.userid] = { completed, carbon };
        } catch (e) {
          console.error(`Failed stats for ${user.userid}`, e);
        }
      }));

      setUserStatsMap(prev => ({ ...prev, ...statsMap }));
    };

    loadPerUserStats();
  }, [users]);

  // Fetch Users with Pagination
  const loadUsers = async (pageNo: number) => {
    try {
      const res = await fetchUserList(pageNo, pageSize);
      if (res.data && res.data.list) {
        setUsers(res.data.list);
        setTotalPages(res.data.totalPages);
      }
    } catch (error) {
      console.error(error);
      toast.error("Failed to load users");
    }
  };

  useEffect(() => {
    loadUsers(page);
  }, [page]);

  // Map Initialization
  useEffect(() => {
    if (mapContainerRef.current && !mapRef.current) {
      mapRef.current = L.map(mapContainerRef.current, { zoomControl: false }).setView([1.3521, 103.8198], 11); // Singapore default
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(mapRef.current);
      L.control.zoom({ position: 'bottomright' }).addTo(mapRef.current);
      layerGroupRef.current = L.layerGroup().addTo(mapRef.current);

      // Force map resize after mount
      setTimeout(() => {
        mapRef.current?.invalidateSize();
      }, 100);
    }

    // Cleanup on unmount
    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, []);

  // Fetch Trips when User Selected
  useEffect(() => {
    if (!selectedUserId) {
      setCurrentTrips([]);
      setSelectedTripId(null);
      return;
    }

    const loadTrips = async () => {
      setLoading(true);
      try {
        const trips = await fetchUserTrips(selectedUserId);
        // Handle potential null/undefined
        const tripList = trips || [];

        console.log("[DEBUG] Raw Trip List:", tripList);
        if (tripList.length > 0) {
          console.log("[DEBUG] First Trip Sample:", tripList[0]);
          console.log("[DEBUG] Carbon Values:", "carbonSaved:", tripList[0].carbonSaved, "carbon_saved:", (tripList[0] as any).carbon_saved);
        }

        setCurrentTrips(tripList);

        if (tripList.length > 0) {
          // Default select the first completed trip
          const firstCompleted = tripList.find(t => t.carbonStatus === 'completed');
          if (firstCompleted) {
            setSelectedTripId(firstCompleted.id);
          } else {
            setSelectedTripId(null); // No completed trips to show
            toast.info("User has trips, but none are completed.");
          }
        } else {
          setSelectedTripId(null);
          toast.info(`No trips found for User ID: ${selectedUserId}`);
        }
      } catch (error) {
        console.error(error);
        toast.error("Failed to load user trips");
      } finally {
        setLoading(false);
      }
    };
    loadTrips();
  }, [selectedUserId]);

  // Update Map Layers - Draw ONLY Selected Trip
  useEffect(() => {
    if (mapRef.current && layerGroupRef.current) {
      if (!selectedTripId) return;

      const trip = currentTrips.find(t => t.id === selectedTripId);
      if (!trip) return;

      console.log("[DEBUG] Selected Trip for Map:", trip);

      // Clear existing layers FIRST
      layerGroupRef.current.clearLayers();

      const bounds = L.latLngBounds([]);
      const hasStart = !!trip.startPoint && (trip.startPoint.lat !== undefined && trip.startPoint.lng !== undefined);
      const hasEnd = !!trip.endPoint && (trip.endPoint.lat !== undefined && trip.endPoint.lng !== undefined);

      if (hasStart && trip.startPoint) {
        const startLat = Number(trip.startPoint.lat);
        const startLng = Number(trip.startPoint.lng);
        const startIcon = L.divIcon({
          className: 'bg-transparent',
          html: `<div class="w-4 h-4 rounded-full bg-green-500 border-2 border-white shadow-md"></div>`
        });
        L.marker([startLat, startLng], { icon: startIcon })
          .bindPopup(`<b>Start</b><br>${trip.startLocation?.placeName || 'Unknown'}`)
          .addTo(layerGroupRef.current!);
        bounds.extend([startLat, startLng]);
      }

      if (hasEnd && trip.endPoint) {
        const endLat = Number(trip.endPoint.lat);
        const endLng = Number(trip.endPoint.lng);
        const endIcon = L.divIcon({
          className: 'bg-transparent',
          html: `<div class="w-4 h-4 rounded-full bg-red-500 border-2 border-white shadow-md"></div>`
        });
        L.marker([endLat, endLng], { icon: endIcon })
          .bindPopup(`<b>End</b><br>${trip.endLocation?.placeName || 'Unknown'}`)
          .addTo(layerGroupRef.current!);
        bounds.extend([endLat, endLng]);
      }

      let routePoints: L.LatLngExpression[] = [];

      // Try to get detailed polyline points (check both camelCase and snake_case)
      const rawPoints = trip.polylinePoints || (trip as any).polyline_points;

      if (rawPoints) {
        try {
          let points: any = rawPoints;
          // If it's a string, parse it
          if (typeof points === 'string') {
            // Handle double encoded strings if necessary
            try {
              points = JSON.parse(points);
            } catch (e) {
              console.warn("[DEBUG] Initial JSON parse failed, it might be raw string?", points);
            }
          }

          if (typeof points === 'string') {
            // Second attempt if still string
            try { points = JSON.parse(points); } catch (e) { }
          }

          if (Array.isArray(points) && points.length > 0) {
            routePoints = points.map((p: any) => {
              const lat = Number(p.lat);
              const lng = Number(p.lng);
              if (isNaN(lat) || isNaN(lng)) {
                console.warn("[DEBUG] Invalid point:", p);
                return null;
              }
              return [lat, lng] as L.LatLngExpression;
            }).filter((p): p is L.LatLngExpression => p !== null);

            console.log("[DEBUG] Parsed routePoints count:", routePoints.length);
            if (routePoints.length > 0) {
              console.log("[DEBUG] First Point:", routePoints[0]);
              console.log("[DEBUG] Last Point:", routePoints[routePoints.length - 1]);
            }
          } else {
            console.warn("[DEBUG] polylinePoints is not a valid array:", points);
          }
        } catch (e) {
          console.warn("[DEBUG] Failed to parse polylinePoints:", e);
        }
      }

      // Fallback to straight line if no polyline
      if (routePoints.length === 0 && hasStart && hasEnd && trip.startPoint && trip.endPoint) {
        console.log("[DEBUG] Using fallback straight line (RED)");
        const startLat = Number(trip.startPoint.lat);
        const startLng = Number(trip.startPoint.lng);
        const endLat = Number(trip.endPoint.lat);
        const endLng = Number(trip.endPoint.lng);

        if (!isNaN(startLat) && !isNaN(startLng) && !isNaN(endLat) && !isNaN(endLng)) {
          routePoints = [
            [startLat, startLng],
            [endLat, endLng]
          ];
        } else {
          console.error("[DEBUG] Start/End coordinates are invalid numbers", trip.startPoint, trip.endPoint);
        }
      } else if (routePoints.length > 0) {
        // Connect Start/End to Polyline if missing (Gap Filling)
        if (hasStart && trip.startPoint) {
          const startLat = Number(trip.startPoint.lat);
          const startLng = Number(trip.startPoint.lng);
          const firstPoint = routePoints[0] as [number, number];

          // Check distance (epsilon) to avoid duplicates
          if (!firstPoint || Math.abs(firstPoint[0] - startLat) > 0.0001 || Math.abs(firstPoint[1] - startLng) > 0.0001) {
            routePoints.unshift([startLat, startLng]);
          }
        }

        if (hasEnd && trip.endPoint) {
          const endLat = Number(trip.endPoint.lat);
          const endLng = Number(trip.endPoint.lng);
          const lastPoint = routePoints[routePoints.length - 1] as [number, number];

          if (!lastPoint || Math.abs(lastPoint[0] - endLat) > 0.0001 || Math.abs(lastPoint[1] - endLng) > 0.0001) {
            routePoints.push([endLat, endLng]);
          }
        }
      }

      if (routePoints.length > 0) {
        // Draw the line
        // Use Red for straight line (fallback), Green/Blue for actual polyline path
        const isFallback = routePoints.length === 2 && !rawPoints;
        const lineColor = isFallback ? '#ef4444' : (trip.isGreenTrip ? '#22c55e' : '#3b82f6');

        console.log("[DEBUG] Drawing Polyline with points:", routePoints.length, "Color:", lineColor);

        const polyline = L.polyline(routePoints, {
          color: lineColor,
          weight: 5,
          opacity: 0.8,
          dashArray: isFallback ? '10, 10' : undefined // Dashed for fallback
        }).addTo(layerGroupRef.current!);

        console.log("[DEBUG] Polyline added to map:", polyline);

        // Extend bounds to include the route
        routePoints.forEach((p: any) => {
          bounds.extend(p);
        });
      } else {
        console.warn("[DEBUG] No route points to draw at all.");
      }

      if (bounds.isValid()) {
        console.log("[DEBUG] Fitting bounds:", bounds.toBBoxString());
        mapRef.current.fitBounds(bounds, { padding: [50, 50] });
      }
    }
  }, [selectedTripId, currentTrips]);

  const getInitials = (name: string) => name ? name.substring(0, 2).toUpperCase() : '??';

  const formatRouteLabel = (t: TripDetail) => {
    const start = t.startLocation?.placeName || 'Unknown Origin';
    const end = t.endLocation?.placeName || 'Unknown Dest';
    return `${start} → ${end}`;
  };

  const getSelectedUserDetails = () => {
    const fromList = users.find(u => u.userid === selectedUserId);
    if (fromList) return fromList;
    // Fallback if not in list (e.g., searched by ID)
    return { nickname: 'User ' + selectedUserId, userid: selectedUserId, vip: { active: false } } as User;
  };

  const activeUser = getSelectedUserDetails();
  const selectedTrip = currentTrips.find(t => t.id === selectedTripId);

  const getCarbon = (t: TripDetail | any) => {
    // Check both potential field names, prefer snake_case
    const val = t.carbon_saved ?? t.carbonSaved;
    // Ensure number
    return val !== undefined && val !== null ? Number(val) : 0;
  };

  const formatCarbon = (val: number) => {
    // Always show in kg as requested
    return { value: val.toFixed(2), unit: 'kg' };
  };

  return (
    <div className="flex flex-col h-full p-6 gap-6">
      <div className="flex-none flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Trip Data Management</h2>
          <p className="text-gray-600">View user trip routes and carbon emission data</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => loadUsers(page)}>
          <RefreshCw className="h-4 w-4 mr-2" /> Refresh List
        </Button>
      </div>

      <div className="flex-1 flex gap-6 min-h-0">
        {/* Map Area - Left Side */}
        <Card className="flex-1 min-w-0 border-0 shadow-sm relative h-[600px]">
          <div ref={mapContainerRef} className="w-full h-full z-0" />

          {/* Floating Trip Selection Card */}
          {selectedUserId && currentTrips.length > 0 && (
            <div className="absolute top-4 left-4 ml-2 w-[340px] bg-white/95 backdrop-blur-sm shadow-xl rounded-xl p-5 z-[500] space-y-5 border border-gray-100 transition-all duration-300 animate-in fade-in slide-in-from-top-2">
              {/* User Header */}
              <div className="flex items-center gap-4 border-b border-gray-100 pb-4">
                <Avatar className="h-12 w-12 border-2 border-white shadow-md ring-1 ring-gray-100">
                  <AvatarFallback className={activeUser.vip?.active ? "bg-purple-100 text-purple-700 font-bold" : "bg-blue-100 text-blue-700 font-bold"}>
                    {getInitials(activeUser.nickname)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <div className="font-bold text-lg text-gray-900 truncate">{activeUser.nickname}</div>
                    {activeUser.vip?.active && (
                      <Badge variant="secondary" className="bg-purple-100 text-purple-700 text-[10px] px-1.5 h-5">VIP</Badge>
                    )}
                  </div>
                  <div className="text-xs text-gray-500 font-mono truncate mb-1">ID: {activeUser.userid}</div>
                  <div className="flex gap-2 text-[10px] text-gray-500">
                    <span className="bg-gray-100 px-1.5 py-0.5 rounded text-gray-600 font-medium">
                      Completed: {currentTrips.filter(t => (t.carbonStatus || '').toLowerCase() === 'completed').length}
                    </span>
                    <span className="bg-green-50 px-1.5 py-0.5 rounded text-green-700 font-medium">
                      Saved: {currentTrips
                        .filter(t => (t.carbonStatus || '').toLowerCase() === 'completed')
                        .reduce((sum, t) => sum + (t.carbonSaved || (t as any).carbon_saved || 0), 0).toFixed(2)} kg
                    </span>
                  </div>
                </div>
              </div>

              {/* Route Selector */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1">
                  <MapPin className="h-3 w-3" /> Select Route
                </label>
                <Select value={selectedTripId || ''} onValueChange={setSelectedTripId}>
                  <SelectTrigger className="w-full bg-gray-50/80 border-gray-200 h-11 focus:ring-1 focus:ring-blue-500">
                    <SelectValue placeholder="Select a route to view" />
                  </SelectTrigger>
                  <SelectContent className="max-h-[200px] z-[600]">
                    {currentTrips.filter(t => t.carbonStatus === 'completed').map(trip => (
                      <SelectItem key={trip.id} value={trip.id} className="py-3">
                        <div className="flex flex-col gap-1">
                          <span className="font-semibold text-sm text-gray-900">{formatRouteLabel(trip)}</span>
                          <span className="text-xs text-gray-500 flex gap-2">
                            <span>{trip.distance.toFixed(1)} km</span>
                            <span>•</span>
                            <span>{formatCarbon(getCarbon(trip)).value} {formatCarbon(getCarbon(trip)).unit} CO₂</span>
                          </span>
                        </div>
                      </SelectItem>
                    ))}
                    {currentTrips.filter(t => t.carbonStatus !== 'completed').length > 0 && (
                      <div className="px-2 py-2 text-xs text-gray-400 italic text-center border-t">
                        {currentTrips.filter(t => t.carbonStatus !== 'completed').length} other incomplete trips hidden
                      </div>
                    )}
                  </SelectContent>
                </Select>
              </div>

              {/* Selected Trip Stats */}
              {selectedTrip && (
                <div className="space-y-3 pt-2">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
                      <div className="text-xs text-gray-500 mb-1">Distance</div>
                      <div className="text-lg font-bold text-gray-900">{selectedTrip.distance.toFixed(2)} <span className="text-xs font-normal text-gray-500">km</span></div>
                    </div>
                    <div className="bg-green-50 p-3 rounded-lg border border-green-100">
                      <div className="text-xs text-green-600 mb-1">Carbon Saved</div>
                      <div className="text-lg font-bold text-green-700">
                        {formatCarbon(getCarbon(selectedTrip)).value} <span className="text-xs font-normal text-green-600">{formatCarbon(getCarbon(selectedTrip)).unit}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex justify-between items-center text-xs text-gray-400 pt-1">
                    <span>{selectedTrip.startTime}</span>
                    <Badge variant="outline" className={selectedTrip.isGreenTrip ? "text-green-600 border-green-200 bg-green-50" : "text-blue-600"}>
                      {selectedTrip.isGreenTrip ? 'Green Trip' : 'Standard'}
                    </Badge>
                  </div>
                </div>
              )}
            </div>
          )}
        </Card>

        {/* Sidebar User List - Right Side */}
        <Card className="w-96 flex flex-col border-0 shadow-sm bg-white h-[600px]">
          <CardHeader className="py-4 border-b space-y-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">User List</CardTitle>
              <p className="text-xs text-gray-500">Page {page} of {totalPages}</p>
            </div>
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-gray-500" />
              <Input
                type="search"
                placeholder="Search by User ID..."
                className="pl-9 h-9"
                value={searchId}
                onChange={(e) => setSearchId(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && searchId.trim()) {
                    setSelectedUserId(searchId.trim());
                  }
                }}
              />
            </div>
          </CardHeader>
          <CardContent className="flex-1 overflow-y-auto p-0">
            <div className="divide-y h-full flex flex-col">
              {users.map(user => (
                <div
                  key={user.id}
                  className={`p-4 cursor-pointer transition-colors hover:bg-gray-50 ${selectedUserId === user.userid ? 'bg-blue-50 border-l-4 border-blue-500' : 'border-l-4 border-transparent'}`}
                  onClick={() => {
                    setSelectedUserId(user.userid);
                  }}
                >
                  <div className="flex items-start gap-3">
                    <Avatar className={user.vip?.active ? "border-2 border-purple-500" : ""}>
                      <AvatarFallback className={user.vip?.active ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"}>
                        {getInitials(user.nickname)}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-1">
                        <div className="font-medium text-sm truncate">{user.nickname}</div>
                        {user.vip?.active ? (
                          <Badge variant="secondary" className="bg-purple-100 text-purple-700 h-5 text-[10px]">VIP</Badge>
                        ) : (
                          <Badge variant="secondary" className="bg-gray-100 text-gray-600 h-5 text-[10px]">Normal</Badge>
                        )}
                      </div>
                      <div className="text-xs text-gray-500 space-y-1">
                        <div className="truncate" title={user.userid}>ID: {user.userid}</div>
                        <div className="flex justify-between">
                          <span className="font-semibold text-green-700">Completed: {userStatsMap[user.userid]?.completed || 0}</span>
                          <span>{userStatsMap[user.userid]?.carbon.toFixed(1) || '0.0'} kg</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              {users.length === 0 && (
                <div className="p-8 text-center text-gray-400 text-sm flex-1 flex items-center justify-center">No users found.</div>
              )}
            </div>
          </CardContent>

          {/* Pagination Controls */}
          <div className="p-3 border-t flex items-center justify-between bg-gray-50 mt-auto">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
              className="h-8 px-2"
            >
              <ChevronLeft className="h-4 w-4 mr-1" />
              Prev
            </Button>
            <span className="text-xs font-medium text-gray-600">
              {page} / {totalPages}
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages}
              className="h-8 px-2"
            >
              Next
              <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
