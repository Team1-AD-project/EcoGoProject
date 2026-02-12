import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getAllBadges,
  getAllBadgeItems,
  getAllClothItems,
  createBadge,
  updateBadge,
  deleteBadge,
  getBadgePurchaseStats,
  getBadgesBySubCategory,
  getBadgesByAcquisitionMethod,
} from './collectiblesApi';
import { api } from '../services/auth';

vi.mock('../services/auth', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockBadge = {
  id: 'b1',
  badgeId: 'badge001',
  name: { zh: '绿色战士', en: 'Green Warrior' },
  description: { zh: '完成10次绿色出行', en: 'Complete 10 green trips' },
  purchaseCost: 100,
  category: 'badge',
  subCategory: 'rank badge',
  acquisitionMethod: 'achievement',
  carbonThreshold: 50,
  icon: { url: 'https://example.com/icon.png', colorScheme: 'green' },
  isActive: true,
  createdAt: '2026-01-01T00:00:00',
};

const mockCloth = {
  ...mockBadge,
  id: 'cl1',
  badgeId: 'cloth001',
  category: 'cloth',
  subCategory: 'clothes_Hat',
  acquisitionMethod: 'purchase',
};

describe('collectiblesApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- getAllBadges ----------
  describe('getAllBadges', () => {
    it('should call GET /badges without category when not provided', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockBadge] } });

      const result = await getAllBadges();

      expect(api.get).toHaveBeenCalledWith('/badges');
      expect(result).toEqual([mockBadge]);
    });

    it('should call GET /badges?category=badge when category is provided', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockBadge] } });

      const result = await getAllBadges('badge');

      expect(api.get).toHaveBeenCalledWith('/badges?category=badge');
      expect(result).toEqual([mockBadge]);
    });

    it('should return empty array when data is null', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: null } });

      const result = await getAllBadges();

      expect(result).toEqual([]);
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Server Error'));

      await expect(getAllBadges()).rejects.toThrow('Server Error');
    });
  });

  // ---------- getAllBadgeItems ----------
  describe('getAllBadgeItems', () => {
    it('should call getAllBadges with category "badge"', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockBadge] } });

      const result = await getAllBadgeItems();

      expect(api.get).toHaveBeenCalledWith('/badges?category=badge');
      expect(result).toEqual([mockBadge]);
    });
  });

  // ---------- getAllClothItems ----------
  describe('getAllClothItems', () => {
    it('should call getAllBadges with category "cloth"', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockCloth] } });

      const result = await getAllClothItems();

      expect(api.get).toHaveBeenCalledWith('/badges?category=cloth');
      expect(result).toEqual([mockCloth]);
    });
  });

  // ---------- createBadge ----------
  describe('createBadge', () => {
    it('should call POST /badges with badge data', async () => {
      const { id, ...createData } = mockBadge;
      const created = { ...mockBadge, id: 'b2' };
      vi.mocked(api.post).mockResolvedValue({ data: { code: 200, message: 'success', data: created } });

      const result = await createBadge(createData);

      expect(api.post).toHaveBeenCalledWith('/badges', createData);
      expect(result).toEqual(created);
    });
  });

  // ---------- updateBadge ----------
  describe('updateBadge', () => {
    it('should call PUT /badges/{id} with partial data', async () => {
      const updateData = { isActive: false };
      const updated = { ...mockBadge, isActive: false };
      vi.mocked(api.put).mockResolvedValue({ data: { code: 200, message: 'success', data: updated } });

      const result = await updateBadge('b1', updateData);

      expect(api.put).toHaveBeenCalledWith('/badges/b1', updateData);
      expect(result.isActive).toBe(false);
    });
  });

  // ---------- deleteBadge ----------
  describe('deleteBadge', () => {
    it('should call DELETE /badges/{id}', async () => {
      vi.mocked(api.delete).mockResolvedValue({});

      await deleteBadge('b1');

      expect(api.delete).toHaveBeenCalledWith('/badges/b1');
    });

    it('should propagate error on delete failure', async () => {
      vi.mocked(api.delete).mockRejectedValue(new Error('Forbidden'));

      await expect(deleteBadge('b1')).rejects.toThrow('Forbidden');
    });
  });

  // ---------- getBadgePurchaseStats ----------
  describe('getBadgePurchaseStats', () => {
    it('should call GET /badges/stats/purchases and return stats', async () => {
      const stats = [{ badgeId: 'badge001', purchaseCount: 42 }];
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: stats } });

      const result = await getBadgePurchaseStats();

      expect(api.get).toHaveBeenCalledWith('/badges/stats/purchases');
      expect(result).toEqual(stats);
    });

    it('should return empty array when data is null', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: null } });

      const result = await getBadgePurchaseStats();

      expect(result).toEqual([]);
    });
  });

  // ---------- getBadgesBySubCategory ----------
  describe('getBadgesBySubCategory', () => {
    it('should call GET /badges/sub-category/{subCategory}', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockBadge] } });

      const result = await getBadgesBySubCategory('rank badge');

      expect(api.get).toHaveBeenCalledWith('/badges/sub-category/rank%20badge');
      expect(result).toEqual([mockBadge]);
    });

    it('should return empty array when data is null', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: null } });

      const result = await getBadgesBySubCategory('nonexistent');

      expect(result).toEqual([]);
    });
  });

  // ---------- getBadgesByAcquisitionMethod ----------
  describe('getBadgesByAcquisitionMethod', () => {
    it('should call GET /badges/acquisition-method/{method}', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockBadge] } });

      const result = await getBadgesByAcquisitionMethod('achievement');

      expect(api.get).toHaveBeenCalledWith('/badges/acquisition-method/achievement');
      expect(result).toEqual([mockBadge]);
    });

    it('should handle "purchase" method', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockCloth] } });

      const result = await getBadgesByAcquisitionMethod('purchase');

      expect(api.get).toHaveBeenCalledWith('/badges/acquisition-method/purchase');
      expect(result).toEqual([mockCloth]);
    });

    it('should return empty array when data is null', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: null } });

      const result = await getBadgesByAcquisitionMethod('vip');

      expect(result).toEqual([]);
    });
  });
});
