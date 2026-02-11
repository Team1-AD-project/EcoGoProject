import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getRankingsByType, getFacultyRankings } from './leaderboardApi';
import { api } from '../services/auth';

vi.mock('../services/auth', () => ({
  api: {
    get: vi.fn(),
  },
}));

const mockRanking = {
  userId: 'u1',
  nickname: 'Alice',
  rank: 1,
  carbonSaved: 120.5,
  isVip: true,
  rewardPoints: 500,
};

const mockStatsDto = {
  rankingsPage: {
    content: [mockRanking],
    totalPages: 1,
    totalElements: 1,
    size: 10,
    number: 0,
  },
  totalCarbonSaved: 5000,
  totalVipUsers: 12,
  totalRewardsDistributed: 300,
};

const mockFacultyRanking = {
  faculty: 'Engineering',
  totalCarbon: 2500.0,
};

describe('leaderboardApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- getRankingsByType ----------
  describe('getRankingsByType', () => {
    it('should call GET /leaderboards/rankings with DAILY type and default params', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockStatsDto } });

      const result = await getRankingsByType('DAILY');

      expect(api.get).toHaveBeenCalledWith('/leaderboards/rankings', {
        params: { type: 'DAILY', date: '', name: '', page: 0, size: 10 },
      });
      expect(result).toEqual(mockStatsDto);
      expect(result.rankingsPage.content[0].nickname).toBe('Alice');
    });

    it('should call with MONTHLY type and custom date', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockStatsDto } });

      await getRankingsByType('MONTHLY', '2026-02');

      expect(api.get).toHaveBeenCalledWith('/leaderboards/rankings', {
        params: { type: 'MONTHLY', date: '2026-02', name: '', page: 0, size: 10 },
      });
    });

    it('should pass name filter and pagination params', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockStatsDto } });

      await getRankingsByType('DAILY', '2026-02-11', 'Alice', 1, 20);

      expect(api.get).toHaveBeenCalledWith('/leaderboards/rankings', {
        params: { type: 'DAILY', date: '2026-02-11', name: 'Alice', page: 1, size: 20 },
      });
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Server Error'));

      await expect(getRankingsByType('DAILY')).rejects.toThrow('Server Error');
    });
  });

  // ---------- getFacultyRankings ----------
  describe('getFacultyRankings', () => {
    it('should call GET /faculties/stats/carbon/monthly and return faculty data', async () => {
      vi.mocked(api.get).mockResolvedValue({
        data: { code: 200, message: 'success', data: [mockFacultyRanking] },
      });

      const result = await getFacultyRankings();

      expect(api.get).toHaveBeenCalledWith('/faculties/stats/carbon/monthly');
      expect(result).toEqual([mockFacultyRanking]);
      expect(result[0].faculty).toBe('Engineering');
    });

    it('should return empty array when no faculties', async () => {
      vi.mocked(api.get).mockResolvedValue({
        data: { code: 200, message: 'success', data: [] },
      });

      const result = await getFacultyRankings();

      expect(result).toEqual([]);
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network Error'));

      await expect(getFacultyRankings()).rejects.toThrow('Network Error');
    });
  });
});
