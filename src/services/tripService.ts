import { api } from './auth';

// /web/trips/all Response Item
export interface TripSummary {
    id: string;
    userId: string;
    startPlaceName: string | null;
    endPlaceName: string | null;
    detectedMode: string | null;
    distance: number;
    carbonSaved: number;
    pointsGained: number;
    isGreenTrip: boolean;
    carbonStatus: string; // 'completed', 'tracking', 'canceled'
    startTime: string;
    endTime: string | null;
}

// /web/trips/user/{userid} Response Item
export interface Coordinate {
    lat: number;
    lng: number;
}

export interface LocationInfo {
    address: string;
    placeName: string;
    campusZone?: string;
}

export interface TripDetail {
    id: string;
    userId: string;
    startPoint: Coordinate | null;
    endPoint: Coordinate | null;
    startLocation: LocationInfo | null;
    endLocation: LocationInfo | null;
    startTime: string;
    endTime: string | null;
    transportModes: string | null;
    detectedMode: string | null;
    mlConfidence: number;
    isGreenTrip: boolean;
    distance: number;
    polylinePoints: string | null;
    carbonSaved: number;
    carbon_saved?: number;
    pointsGained: number;
    carbonStatus: string;
    createdAt: string;
}

// API Response Wrappers
export interface TripSummaryListResponse {
    code: number;
    message: string;
    data: TripSummary[];
}

export interface TripDetailListResponse {
    code: number;
    message: string;
    data: TripDetail[];
}

export const fetchAllTrips = async (): Promise<TripSummary[]> => {
    try {
        console.log("[TripService] Fetching ALL trips...");
        const response = await api.get<TripSummaryListResponse>('/trips/all');
        console.log("[TripService] fetchAllTrips Response:", response.data);
        if (response.data.code === 200) {
            return response.data.data || []; // Ensure array is returned even if data is null
        }
        console.warn("[TripService] fetchAllTrips non-200:", response.data);
        return [];
    } catch (e) {
        console.error("[TripService] fetchAllTrips failed:", e);
        return [];
    }
};

export const fetchUserTrips = async (userid: string): Promise<TripDetail[]> => {
    try {
        console.log(`[TripService] Fetching trips for user: ${userid}`);
        const response = await api.get<TripDetailListResponse>(`/trips/user/${userid}`);
        console.log(`[TripService] Response for ${userid}:`, response.data);
        if (response.data.code === 200) {
            return response.data.data;
        }
        console.warn(`[TripService] API returned non-200 code:`, response.data);
        return [];
    } catch (error) {
        console.error(`[TripService] Error fetching trips for ${userid}:`, error);
        return [];
    }
};
