import { api } from './auth';

export interface AdminAction {
    operator_id: string;
    reason: string;
    approval_status: string;
}

export interface PointsTransaction {
    id: string;
    change_type: 'gain' | 'deduct';
    points: number;
    source: 'trip' | 'badge' | 'admin' | 'store' | 'leaderboard' | 'challenges';
    description: string | null;
    balance_after: number;
    created_at: string;
    admin_action: AdminAction | null;
}

export interface TransactionListResponse {
    code: number;
    message: string;
    data: PointsTransaction[];
}

export interface PointsSummary {
    userId: string;
    currentPoints: number;
    totalPoints: number;
}

export interface PointsSummaryResponse {
    code: number;
    message: string;
    data: PointsSummary[];
}

// Fetch all points history (Filtered by userId if provided)
export const fetchUserTransactions = async (userId: string): Promise<TransactionListResponse> => {
    const response = await api.get<TransactionListResponse>(`/points/user/${userId}/history`);
    return response.data;
};

// Fetch global points summary
export const fetchPointsSummary = async (): Promise<PointsSummaryResponse> => {
    const response = await api.get<PointsSummaryResponse>('/points/all');
    return response.data;
};

// Fetch all points history (all users) for analytics
export const fetchAllPointsHistory = async (): Promise<TransactionListResponse> => {
    const response = await api.get<TransactionListResponse>('/points/history/all');
    return response.data;
};
