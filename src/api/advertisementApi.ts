import { api } from '../services/auth';

// 定义广告的数据结构
export interface Advertisement {
  id: string;
  name: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  imageUrl: string;
  linkUrl: string;
  position: 'banner' | 'sidebar' | 'popup' | 'feed';
  impressions: number;
  clicks: number;
  clickRate?: number;
}

// 定义分页数据结构
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

// 定义通用的API响应结构
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// --- API Functions ---
// auth.ts baseURL = '/api/v1/web', 所以这里不需要再加 /web 前缀

export async function getAllAdvertisements(name: string = '', page: number = 0, size: number = 10): Promise<Page<Advertisement>> {
  const response = await api.get<ApiResponse<Page<Advertisement>>>('/advertisements', {
    params: { name, page, size },
  });
  return response.data.data;
}

export async function getAdvertisementById(id: string): Promise<Advertisement> {
  const response = await api.get<ApiResponse<Advertisement>>(`/advertisements/${id}`);
  return response.data.data;
}

export async function createAdvertisement(ad: Omit<Advertisement, 'id' | 'impressions' | 'clicks' | 'clickRate'>): Promise<Advertisement> {
  const response = await api.post<ApiResponse<Advertisement>>('/advertisements', ad);
  return response.data.data;
}

export async function updateAdvertisement(id: string, ad: Partial<Advertisement>): Promise<Advertisement> {
  const response = await api.put<ApiResponse<Advertisement>>(`/advertisements/${id}`, ad);
  return response.data.data;
}

export async function deleteAdvertisement(id: string): Promise<void> {
  await api.delete(`/advertisements/${id}`);
}

export async function updateAdvertisementStatus(id: string, status: string): Promise<Advertisement> {
  const response = await api.patch<ApiResponse<Advertisement>>(`/advertisements/${id}/status`, null, { params: { status } });
  return response.data.data;
}

// Mobile端点需要覆盖 baseURL
export async function getActiveAdvertisements(): Promise<Advertisement[]> {
    const response = await api.get<ApiResponse<Advertisement[]>>('/mobile/advertisements/active', { baseURL: '/api/v1' });
    return response.data.data;
}
