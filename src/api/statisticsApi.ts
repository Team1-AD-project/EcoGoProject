// API 基础配置
const API_BASE_URL = 'http://localhost:8090/api/v1';

// 响应类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 通用请求函数
async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  return response.json();
}

// 仪表盘统计数据
export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalAdvertisements: number;
  activeAdvertisements: number;
  totalActivities: number;
  ongoingActivities: number;
  totalCarbonCredits: number;
  totalCarbonReduction: number;
  redemptionVolume: number;
}

// 热力图数据点
export interface HeatmapDataPoint {
  region: string;
  latitude: number;
  longitude: number;
  emissionValue: number;
  reductionValue: number;
  intensity: 'LOW' | 'MEDIUM' | 'HIGH';
}

// 热力图汇总数据
export interface HeatmapSummary {
  dataPoints: HeatmapDataPoint[];
  regionStats: Record<string, number>;
  totalEmissions: number;
  totalReductions: number;
}

// 获取仪表盘综合统计数据
export async function getDashboardStats(): Promise<DashboardStats> {
  const response = await fetchApi<ApiResponse<DashboardStats>>('/statistics/dashboard');
  return response.data;
}

// 获取总碳减排量
export async function getCarbonReduction(): Promise<number> {
  const response = await fetchApi<ApiResponse<number>>('/statistics/carbon-reduction');
  return response.data;
}

// 获取活跃用户数
export async function getActiveUsers(days: number = 30): Promise<number> {
  const response = await fetchApi<ApiResponse<number>>(`/statistics/active-users?days=${days}`);
  return response.data;
}

// 获取奖励兑换量
export async function getRedemptionVolume(): Promise<number> {
  const response = await fetchApi<ApiResponse<number>>('/statistics/redemption-volume');
  return response.data;
}

// 获取碳排热力图数据
export async function getEmissionHeatmap(): Promise<HeatmapSummary> {
  const response = await fetchApi<ApiResponse<HeatmapSummary>>('/statistics/emission-heatmap');
  return response.data;
}
