import { api } from '../services/auth';

// Challenge数据结构
export interface Challenge {
  id?: string;
  title: string;
  description: string;
  type: string; // GREEN_TRIPS_DISTANCE, CARBON_SAVED, GREEN_TRIPS_COUNT
  target: number;
  reward: number;
  badge?: string;
  icon: string;
  status: string; // ACTIVE, EXPIRED
  participants: number;
  startTime?: string;
  endTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

// 用户挑战进度DTO（从Trip表实时计算）
export interface UserChallengeProgressDTO {
  id: string;
  challengeId: string;
  userId: string;
  status: string; // IN_PROGRESS, COMPLETED
  current: number; // 实时计算的进度值
  target: number;
  progressPercent: number;
  joinedAt: string;
  completedAt?: string;
  rewardClaimed: boolean;
  // 用户信息
  userNickname?: string;
  userEmail?: string;
  userAvatar?: string;
}

// API响应结构
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Challenge API
export const challengeApi = {
  // 获取所有挑战
  async getAllChallenges(): Promise<Challenge[]> {
    const response = await api.get<ApiResponse<Challenge[]>>('/challenges');
    return response.data.data;
  },

  // 根据ID获取挑战
  async getChallengeById(id: string): Promise<Challenge> {
    const response = await api.get<ApiResponse<Challenge>>(`/challenges/${id}`);
    return response.data.data;
  },

  // 创建挑战
  async createChallenge(challenge: Omit<Challenge, 'id' | 'createdAt' | 'updatedAt'>): Promise<Challenge> {
    const response = await api.post<ApiResponse<Challenge>>('/challenges', challenge);
    return response.data.data;
  },

  // 更新挑战
  async updateChallenge(id: string, challenge: Partial<Challenge>): Promise<Challenge> {
    const response = await api.put<ApiResponse<Challenge>>(`/challenges/${id}`, challenge);
    return response.data.data;
  },

  // 删除挑战
  async deleteChallenge(id: string): Promise<void> {
    await api.delete(`/challenges/${id}`);
  },

  // 根据状态获取挑战
  async getChallengesByStatus(status: string): Promise<Challenge[]> {
    const response = await api.get<ApiResponse<Challenge[]>>(`/challenges/status/${status}`);
    return response.data.data;
  },

  // 根据类型获取挑战
  async getChallengesByType(type: string): Promise<Challenge[]> {
    const response = await api.get<ApiResponse<Challenge[]>>(`/challenges/type/${type}`);
    return response.data.data;
  },

  // 获取挑战的所有参与者及其进度（从Trip表实时计算）
  async getChallengeParticipants(challengeId: string): Promise<UserChallengeProgressDTO[]> {
    const response = await api.get<ApiResponse<UserChallengeProgressDTO[]>>(`/challenges/${challengeId}/participants`);
    return response.data.data;
  },
};

export default challengeApi;
