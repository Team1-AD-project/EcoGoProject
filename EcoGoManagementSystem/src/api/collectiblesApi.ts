// 使用与 userService.ts 相同的 api 实例，确保认证一致性
import { api } from '@/services/auth';

// 响应类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 后端 Badge 模型
export interface BadgeIcon {
  url: string;
  colorScheme: string;
}

export interface Badge {
  id: string;
  badgeId: string;
  name: { zh: string; en: string };
  description: { zh: string; en: string };
  purchaseCost: number | null;
  category: string;  // "badge" 或 "cloth"
  subCategory: string;  // "VIP badge", "rank badge", "clothes_Hat" 等
  acquisitionMethod: string;  // "purchase", "achievement", "vip", "event", "task", "free"
  carbonThreshold: number | null;
  icon: BadgeIcon;
  isActive: boolean;
  createdAt: string;
}

// 购买统计
export interface BadgePurchaseStat {
  badgeId: string;
  purchaseCount: number;
}

// 获取所有徽章/服饰
export async function getAllBadges(category?: string): Promise<Badge[]> {
  const params = category ? `?category=${encodeURIComponent(category)}` : '';
  const response = await api.get<ApiResponse<Badge[]>>(`/badges${params}`);
  return response.data.data || [];
}

// 获取所有徽章 (category="badge")
export async function getAllBadgeItems(): Promise<Badge[]> {
  return getAllBadges('badge');
}

// 获取所有服饰 (category="cloth")
export async function getAllClothItems(): Promise<Badge[]> {
  return getAllBadges('cloth');
}

// 创建徽章/服饰
export async function createBadge(badge: Omit<Badge, 'id'>): Promise<Badge> {
  const response = await api.post<ApiResponse<Badge>>('/badges', badge);
  return response.data.data;
}

// 更新徽章/服饰
export async function updateBadge(badgeId: string, badge: Partial<Badge>): Promise<Badge> {
  const response = await api.put<ApiResponse<Badge>>(`/badges/${badgeId}`, badge);
  return response.data.data;
}

// 删除徽章/服饰
export async function deleteBadge(badgeId: string): Promise<void> {
  await api.delete<ApiResponse<void>>(`/badges/${badgeId}`);
}

// 获取购买统计
export async function getBadgePurchaseStats(): Promise<BadgePurchaseStat[]> {
  const response = await api.get<ApiResponse<BadgePurchaseStat[]>>('/badges/stats/purchases');
  return response.data.data || [];
}

// 按子分类获取 (mobile endpoint - 不需要认证)
export async function getBadgesBySubCategory(subCategory: string): Promise<Badge[]> {
  const response = await api.get<ApiResponse<Badge[]>>(`/badges/sub-category/${encodeURIComponent(subCategory)}`);
  return response.data.data || [];
}

// 按获取方式获取 (mobile endpoint - 不需要认证)
export async function getBadgesByAcquisitionMethod(method: string): Promise<Badge[]> {
  const response = await api.get<ApiResponse<Badge[]>>(`/badges/acquisition-method/${encodeURIComponent(method)}`);
  return response.data.data || [];
}
