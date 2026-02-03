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

// 后端 Advertisement 模型
export interface Advertisement {
  id: string;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
}

// 获取所有广告
export async function getAllAdvertisements(): Promise<Advertisement[]> {
  const response = await fetchApi<ApiResponse<Advertisement[]>>('/advertisements');
  return response.data;
}

// 根据 ID 获取广告
export async function getAdvertisementById(id: string): Promise<Advertisement> {
  const response = await fetchApi<ApiResponse<Advertisement>>(`/advertisements/${id}`);
  return response.data;
}

// 创建广告
export async function createAdvertisement(ad: Omit<Advertisement, 'id'>): Promise<Advertisement> {
  const response = await fetchApi<ApiResponse<Advertisement>>('/advertisements', {
    method: 'POST',
    body: JSON.stringify(ad),
  });
  return response.data;
}

// 更新广告
export async function updateAdvertisement(id: string, ad: Partial<Advertisement>): Promise<Advertisement> {
  const response = await fetchApi<ApiResponse<Advertisement>>(`/advertisements/${id}`, {
    method: 'PUT',
    body: JSON.stringify(ad),
  });
  return response.data;
}

// 删除广告
export async function deleteAdvertisement(id: string): Promise<void> {
  await fetchApi<ApiResponse<void>>(`/advertisements/${id}`, {
    method: 'DELETE',
  });
}

// 按状态获取广告
export async function getAdvertisementsByStatus(status: string): Promise<Advertisement[]> {
  const response = await fetchApi<ApiResponse<Advertisement[]>>(`/advertisements/status/${status}`);
  return response.data;
}

// 更新广告状态
export async function updateAdvertisementStatus(id: string, status: string): Promise<Advertisement> {
  const response = await fetchApi<ApiResponse<Advertisement>>(`/advertisements/${id}/status?status=${status}`, {
    method: 'PATCH',
  });
  return response.data;
}

// 获取活跃广告
export async function getActiveAdvertisements(): Promise<Advertisement[]> {
  const response = await fetchApi<ApiResponse<Advertisement[]>>('/advertisements/active');
  return response.data;
}
