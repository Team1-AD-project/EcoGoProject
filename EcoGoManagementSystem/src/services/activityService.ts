import { api } from './auth';

export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'ENDED';
export type ActivityType = 'ONLINE' | 'OFFLINE';

export interface Activity {
    id: string;
    title: string;
    description: string;
    type: ActivityType;
    status: ActivityStatus;
    rewardCredits: number;
    maxParticipants: number;
    currentParticipants: number;
    participantIds: string[];
    startTime: string;
    endTime: string;
    latitude: number | null;
    longitude: number | null;
    locationName: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface ActivityResponse {
    code: number;
    message: string;
    data: Activity;
}

export interface ActivityListResponse {
    code: number;
    message: string;
    data: Activity[];
}

export interface CreateActivityRequest {
    title: string;
    description: string;
    type: ActivityType;
    status?: ActivityStatus;
    rewardCredits: number;
    maxParticipants: number;
    startTime: string;
    endTime: string;
    latitude?: number | null;
    longitude?: number | null;
    locationName?: string | null;
}

export interface UpdateActivityRequest {
    title?: string;
    description?: string;
    type?: ActivityType;
    status?: ActivityStatus;
    rewardCredits?: number;
    maxParticipants?: number;
    startTime?: string;
    endTime?: string;
    latitude?: number | null;
    longitude?: number | null;
    locationName?: string | null;
}

// auth.ts baseURL = '/api/v1/web', 所以这里不需要再加 /web 前缀

export const fetchActivities = async (): Promise<ActivityListResponse> => {
    const response = await api.get<ActivityListResponse>('/activities');
    return response.data;
};

export const fetchActivitiesByStatus = async (status: ActivityStatus): Promise<ActivityListResponse> => {
    const response = await api.get<ActivityListResponse>(`/activities/status/${status}`);
    return response.data;
};

export const fetchActivityById = async (id: string): Promise<ActivityResponse> => {
    const response = await api.get<ActivityResponse>(`/activities/${id}`);
    return response.data;
};

export const createActivity = async (data: CreateActivityRequest): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>('/activities', data);
    return response.data;
};

export const updateActivity = async (id: string, data: UpdateActivityRequest): Promise<ActivityResponse> => {
    const response = await api.put<ActivityResponse>(`/activities/${id}`, data);
    return response.data;
};

export const deleteActivity = async (id: string): Promise<void> => {
    await api.delete(`/activities/${id}`);
};

export const publishActivity = async (id: string): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>(`/activities/${id}/publish`, {});
    return response.data;
};
