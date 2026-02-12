import { api } from '../services/auth';

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface LeaderboardRankingDto {
  userId: string;
  nickname: string;
  rank: number;
  carbonSaved: number;
  isVip: boolean;
  rewardPoints: number;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface LeaderboardStatsDto {
  rankingsPage: Page<LeaderboardRankingDto>;
  totalCarbonSaved: number;
  totalVipUsers: number;
  totalRewardsDistributed: number;
}

export type LeaderboardType = 'DAILY' | 'MONTHLY';

export interface FacultyCarbonResponse {
  faculty: string;
  totalCarbon: number;
}

/**
 * Get rankings by type and optional date.
 * Admin can pass date (DAILY: "2026-02-07", MONTHLY: "2026-02").
 * Empty date = current day/month.
 */
export async function getRankingsByType(
  type: LeaderboardType,
  date: string = '',
  name: string = '',
  page: number = 0,
  size: number = 10
): Promise<LeaderboardStatsDto> {
  const response = await api.get<ApiResponse<LeaderboardStatsDto>>('/leaderboards/rankings', {
    params: { type, date, name, page, size },
  });
  return response.data.data;
}

/**
 * Get monthly faculty carbon rankings (current month).
 */
export async function getFacultyRankings(): Promise<FacultyCarbonResponse[]> {
  const response = await api.get<ApiResponse<FacultyCarbonResponse[]>>('/faculties/stats/carbon/monthly');
  return response.data.data;
}
