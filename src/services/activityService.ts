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
}

// Fetch all activities
export const fetchActivities = async (): Promise<ActivityListResponse> => {
    const response = await api.get<ActivityListResponse>('/activities', { baseURL: '/api/v1' });
    return response.data;
};

// Fetch activities by status
export const fetchActivitiesByStatus = async (status: ActivityStatus): Promise<ActivityListResponse> => {
    const response = await api.get<ActivityListResponse>(`/activities/status/${status}`, { baseURL: '/api/v1' });
    return response.data;
};

// Fetch single activity by ID
export const fetchActivityById = async (id: string): Promise<ActivityResponse> => {
    const response = await api.get<ActivityResponse>(`/activities/${id}`, { baseURL: '/api/v1' });
    return response.data;
};

// Create new activity
export const createActivity = async (data: CreateActivityRequest): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>('/activities', data, { baseURL: '/api/v1' });
    return response.data;
};

// Update activity
export const updateActivity = async (id: string, data: UpdateActivityRequest): Promise<ActivityResponse> => {
    const response = await api.put<ActivityResponse>(`/activities/${id}`, data, { baseURL: '/api/v1' });
    return response.data;
};

// Delete activity
export const deleteActivity = async (id: string): Promise<void> => {
    await api.delete(`/activities/${id}`, { baseURL: '/api/v1' });
};

// Publish activity
export const publishActivity = async (id: string): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>(`/activities/${id}/publish`, {}, { baseURL: '/api/v1' });
    return response.data;
};

// Join activity
export const joinActivity = async (activityId: string, userId: string): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>(`/activities/${activityId}/join?userId=${userId}`, {}, { baseURL: '/api/v1' });
    return response.data;
};

// Leave activity
export const leaveActivity = async (activityId: string, userId: string): Promise<ActivityResponse> => {
    const response = await api.post<ActivityResponse>(`/activities/${activityId}/leave?userId=${userId}`, {}, { baseURL: '/api/v1' });
    return response.data;
};
