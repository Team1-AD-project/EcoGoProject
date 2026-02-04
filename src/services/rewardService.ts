import { api } from './auth';

export interface Reward {
    id: string;
    name: string | null;
    description: string | null;
    price: number | null; // This seems to be cash price, but we care about redemptionPoints
    stock: number;
    category: string | null;
    brand: string | null;
    imageUrl: string | null;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
    isForRedemption: boolean;
    redemptionPoints: number;
    vipLevelRequired: number;
    redemptionLimit: number;
    totalRedemptionCount: number;
}

export interface RewardListResponse {
    code: number;
    message?: string;
    pagination: {
        page: number;
        totalPages: number;
        size: number;
        total: number;
    };
    data: Reward[];
}

export interface OrderItem {
    goodsId: string;
    goodsName: string;
    quantity: number;
    price: number;
    subtotal: number;
}

export interface Order {
    id: string;
    orderNumber: string;
    userId: string;
    items: OrderItem[];
    totalAmount: number;
    shippingFee: number;
    finalAmount: number;
    status: 'PENDING' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'; // Added likely statuses
    paymentMethod: string;
    paymentStatus: string;
    shippingAddress: string | null;
    recipientName: string | null;
    recipientPhone: string | null;
    remark: string | null;
    createdAt: string;
    updatedAt: string;
    trackingNumber: string | null;
    carrier: string | null;
    isRedemptionOrder: boolean;
    pointsUsed: number;
    pointsEarned: number;
}

export interface OrderListResponse {
    code: number;
    message: string;
    data: {
        pagination: {
            page: number;
            total: number;
            size: number;
            totalPages: number;
        };
        orders: Order[];
    };
}

// Fetch rewards list with pagination
export const fetchRewards = async (page: number = 1, size: number = 20): Promise<RewardListResponse> => {
    // Note: The API is /goods, user indicates base is /api/v1 for orders, assuming same for goods
    const response = await api.get<RewardListResponse>(`/goods?page=${page}&size=${size}&_t=${new Date().getTime()}`, { baseURL: '/api/v1' });
    return response.data;
};

export interface CreateRewardRequest {
    name: string;
    description: string;
    price: number;
    stock: number;
    category: string;
    brand: string;
    imageUrl: string;
    isActive: boolean;
    isForRedemption: boolean;
    redemptionPoints: number;
    vipLevelRequired: number;
    redemptionLimit: number;
}

// Fetch orders list with pagination
export const fetchOrders = async (page: number = 1, size: number = 10): Promise<OrderListResponse> => {
    const response = await api.get<OrderListResponse>(`/orders?page=${page}&size=${size}`, { baseURL: '/api/v1' });
    return response.data;
};

export const createReward = async (data: CreateRewardRequest): Promise<any> => {
    const response = await api.post('/goods', data, { baseURL: '/api/v1' });
    return response.data;
};

export const updateReward = async (id: string, data: Partial<CreateRewardRequest>): Promise<any> => {
    const response = await api.put(`/goods/${id}`, data, { baseURL: '/api/v1' });
    return response.data;
};

export const deleteReward = async (id: string): Promise<any> => {
    const response = await api.delete(`/goods/${id}`, { baseURL: '/api/v1' });
    return response.data;
};
