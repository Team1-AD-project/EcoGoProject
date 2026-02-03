import { api } from './auth';

export interface UserStats {
    totalTrips: number;
    totalDistance: number;
    greenDays: number;
    weeklyRank: number;
    monthlyRank: number;
    totalPointsFromTrips: number;
}

export interface UserPreferences {
    preferredTransport: string | null;
    enablePush: boolean;
    enableEmail: boolean;
    enableBusReminder: boolean;
    language: string;
    theme: string | null;
    shareLocation: boolean;
    showOnLeaderboard: boolean;
    shareAchievements: boolean;
}

export interface UserVip {
    expiryDate: string | null;
    plan: string | null;
    autoRenew: boolean;
    pointsMultiplier: number;
    active: boolean;
}

export interface User {
    id: string;
    userid: string;
    email: string | null;
    phone: string;
    nickname: string;
    avatar: string | null;
    vip: UserVip;
    stats: UserStats;
    preferences: UserPreferences;
    totalCarbon: number;
    totalPoints: number;
    currentPoints: number;
    lastLoginAt: string | null;
    createdAt: string;
    updatedAt: string;
    activityMetrics: any | null; // Define strictly if needed
    admin: boolean;
    deactivated: boolean;
    isAdmin: boolean;
    isDeactivated: boolean;
}

export interface UserListResponse {
    code: number;
    message: string;
    data: {
        list: User[];
        total: number;
        page: number;
        size: number;
        totalPages: number;
    };
}

// Fetch user list with pagination
export const fetchUserList = async (page: number = 1, size: number = 20): Promise<UserListResponse> => {
    const response = await api.get<UserListResponse>(`/users/list?page=${page}&size=${size}`);
    return response.data;
};
