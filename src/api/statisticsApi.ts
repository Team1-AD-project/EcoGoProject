import { api } from '../services/auth';

// API响应结构
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// --- Types for the Management Analytics Page ---

export interface Metric {
  currentValue: number;
  previousValue: number;
  growthRate: number;
}

export interface UserGrowthPoint {
  date: string;
  users: number;
  newUsers: number;
  activeUsers: number;
}
export interface RevenueGrowthPoint {
  date: string;
  vipRevenue: number;
  shopRevenue: number;
}
export interface DistributionPoint {
  name: string;
  value: number;
}

// Renamed from StepsGrowthPoint
export interface CarbonGrowthPoint {
  date: string;
  carbonSaved: number;
  avgPerUser: number;
}

export interface ManagementAnalyticsData {
  totalUsers: Metric;
  newUsers: Metric;
  activeUsers: Metric;
  totalCarbonSaved: Metric;
  averageCarbonPerUser: Metric;
  totalRevenue: Metric;
  vipRevenue: Metric;
  shopRevenue: Metric;
  userGrowthTrend: UserGrowthPoint[];
  carbonGrowthTrend: CarbonGrowthPoint[];
  revenueGrowthTrend: RevenueGrowthPoint[];
  vipDistribution: DistributionPoint[];
  categoryRevenue: DistributionPoint[];
}

export async function getManagementAnalytics(timeRange: string): Promise<ManagementAnalyticsData> {
  const response = await api.get<ApiResponse<ManagementAnalyticsData>>('/statistics/management-analytics', {
    params: { timeRange },
  });
  return response.data.data;
}

export async function getRedemptionVolume(): Promise<number> {
  const response = await api.get<ApiResponse<number>>('/statistics/redemption-volume');
  return response.data.data;
}

export interface AnalyticsSummary {
  activeUsers7d: number;
  activeUsers30d: number;
  totalTrips: number;
  totalCarbonSaved: number;
  totalRedemptions: number;
  transportDistribution: Record<string, number>;
  topUsers: {
    nickname: string;
    trips: number;
    carbonSaved: number;
  }[];
}

export async function getAnalyticsSummary(): Promise<AnalyticsSummary> {
  const response = await api.get<ApiResponse<AnalyticsSummary>>('/statistics/summary');
  return response.data.data;
}
