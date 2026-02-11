import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getManagementAnalytics, getRedemptionVolume } from './statisticsApi';
import { api } from '../services/auth';

vi.mock('../services/auth', () => ({
  api: {
    get: vi.fn(),
  },
}));

const mockMetric = {
  currentValue: 1000,
  previousValue: 800,
  growthRate: 25.0,
};

const mockAnalytics = {
  totalUsers: mockMetric,
  newUsers: { currentValue: 50, previousValue: 40, growthRate: 25 },
  activeUsers: { currentValue: 300, previousValue: 250, growthRate: 20 },
  totalCarbonSaved: { currentValue: 5000, previousValue: 4000, growthRate: 25 },
  averageCarbonPerUser: { currentValue: 16.7, previousValue: 16.0, growthRate: 4.4 },
  totalRevenue: { currentValue: 10000, previousValue: 8000, growthRate: 25 },
  vipRevenue: { currentValue: 6000, previousValue: 5000, growthRate: 20 },
  shopRevenue: { currentValue: 4000, previousValue: 3000, growthRate: 33.3 },
  userGrowthTrend: [{ date: '2026-02-01', users: 950, newUsers: 10, activeUsers: 280 }],
  carbonGrowthTrend: [{ date: '2026-02-01', carbonSaved: 180, avgPerUser: 0.6 }],
  revenueGrowthTrend: [{ date: '2026-02-01', vipRevenue: 200, shopRevenue: 150 }],
  vipDistribution: [{ name: 'Premium', value: 30 }],
  categoryRevenue: [{ name: 'Badges', value: 2000 }],
};

describe('statisticsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- getManagementAnalytics ----------
  describe('getManagementAnalytics', () => {
    it('should call GET /statistics/management-analytics with timeRange param', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockAnalytics } });

      const result = await getManagementAnalytics('7d');

      expect(api.get).toHaveBeenCalledWith('/statistics/management-analytics', {
        params: { timeRange: '7d' },
      });
      expect(result).toEqual(mockAnalytics);
    });

    it('should handle 30d time range', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockAnalytics } });

      await getManagementAnalytics('30d');

      expect(api.get).toHaveBeenCalledWith('/statistics/management-analytics', {
        params: { timeRange: '30d' },
      });
    });

    it('should return all analytics fields correctly', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockAnalytics } });

      const result = await getManagementAnalytics('7d');

      expect(result.totalUsers.currentValue).toBe(1000);
      expect(result.totalUsers.growthRate).toBe(25.0);
      expect(result.userGrowthTrend).toHaveLength(1);
      expect(result.carbonGrowthTrend[0].carbonSaved).toBe(180);
      expect(result.vipDistribution[0].name).toBe('Premium');
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Server Error'));

      await expect(getManagementAnalytics('7d')).rejects.toThrow('Server Error');
    });
  });

  // ---------- getRedemptionVolume ----------
  describe('getRedemptionVolume', () => {
    it('should call GET /statistics/redemption-volume and return number', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: 42 } });

      const result = await getRedemptionVolume();

      expect(api.get).toHaveBeenCalledWith('/statistics/redemption-volume');
      expect(result).toBe(42);
    });

    it('should handle zero redemption volume', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: 0 } });

      const result = await getRedemptionVolume();

      expect(result).toBe(0);
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network Error'));

      await expect(getRedemptionVolume()).rejects.toThrow('Network Error');
    });
  });
});
