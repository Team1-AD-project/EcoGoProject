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

// 后端 Ranking 模型
export interface Ranking {
  id: string;
  period: string;
  type: string;
  status: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  rank: number;
  userId: string;
  nickname: string;
  steps: number;
  isVip: boolean;
}

// 获取所有可用的排行周期
export async function getLeaderboardPeriods(): Promise<string[]> {
  const response = await fetchApi<ApiResponse<string[]>>('/leaderboards/periods');
  return response.data;
}

// 获取特定周期的排行榜数据
export async function getRankingsByPeriod(period: string): Promise<Ranking[]> {
  const encodedPeriod = encodeURIComponent(period);
  const response = await fetchApi<ApiResponse<Ranking[]>>(`/leaderboards/rankings?period=${encodedPeriod}`);
  return response.data;
}
