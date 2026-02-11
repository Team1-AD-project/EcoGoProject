import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getAllAdvertisements,
  getAdvertisementById,
  createAdvertisement,
  updateAdvertisement,
  deleteAdvertisement,
  updateAdvertisementStatus,
  getActiveAdvertisements,
} from './advertisementApi';
import { api } from '../services/auth';

vi.mock('../services/auth', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

const mockAd = {
  id: 'ad1',
  name: 'Summer Sale',
  description: 'Big summer sale',
  status: 'ACTIVE',
  startDate: '2026-06-01',
  endDate: '2026-08-31',
  imageUrl: 'https://example.com/ad.png',
  linkUrl: 'https://example.com',
  position: 'banner' as const,
  impressions: 1000,
  clicks: 50,
  clickRate: 5.0,
};

const mockPage = {
  content: [mockAd],
  totalPages: 1,
  totalElements: 1,
  size: 10,
  number: 0,
};

describe('advertisementApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- getAllAdvertisements ----------
  describe('getAllAdvertisements', () => {
    it('should call GET /advertisements with default params', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockPage } });

      const result = await getAllAdvertisements();

      expect(api.get).toHaveBeenCalledWith('/advertisements', {
        params: { name: '', page: 0, size: 10 },
      });
      expect(result).toEqual(mockPage);
    });

    it('should pass custom name, page, size params', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockPage } });

      await getAllAdvertisements('sale', 2, 20);

      expect(api.get).toHaveBeenCalledWith('/advertisements', {
        params: { name: 'sale', page: 2, size: 20 },
      });
    });

    it('should propagate error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Server Error'));

      await expect(getAllAdvertisements()).rejects.toThrow('Server Error');
    });
  });

  // ---------- getAdvertisementById ----------
  describe('getAdvertisementById', () => {
    it('should call GET /advertisements/{id} and return data', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockAd } });

      const result = await getAdvertisementById('ad1');

      expect(api.get).toHaveBeenCalledWith('/advertisements/ad1');
      expect(result).toEqual(mockAd);
    });

    it('should propagate error for invalid id', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Not Found'));

      await expect(getAdvertisementById('bad')).rejects.toThrow('Not Found');
    });
  });

  // ---------- createAdvertisement ----------
  describe('createAdvertisement', () => {
    it('should call POST /advertisements with ad data', async () => {
      const createData = {
        name: 'New Ad',
        description: 'Desc',
        status: 'DRAFT',
        startDate: '2026-07-01',
        endDate: '2026-07-31',
        imageUrl: 'https://example.com/new.png',
        linkUrl: 'https://example.com/new',
        position: 'sidebar' as const,
      };
      const created = { ...mockAd, ...createData, id: 'ad2' };
      vi.mocked(api.post).mockResolvedValue({ data: { code: 200, message: 'success', data: created } });

      const result = await createAdvertisement(createData);

      expect(api.post).toHaveBeenCalledWith('/advertisements', createData);
      expect(result).toEqual(created);
    });
  });

  // ---------- updateAdvertisement ----------
  describe('updateAdvertisement', () => {
    it('should call PUT /advertisements/{id} with partial data', async () => {
      const updateData = { name: 'Updated Ad' };
      const updated = { ...mockAd, name: 'Updated Ad' };
      vi.mocked(api.put).mockResolvedValue({ data: { code: 200, message: 'success', data: updated } });

      const result = await updateAdvertisement('ad1', updateData);

      expect(api.put).toHaveBeenCalledWith('/advertisements/ad1', updateData);
      expect(result).toEqual(updated);
    });
  });

  // ---------- deleteAdvertisement ----------
  describe('deleteAdvertisement', () => {
    it('should call DELETE /advertisements/{id}', async () => {
      vi.mocked(api.delete).mockResolvedValue({});

      await deleteAdvertisement('ad1');

      expect(api.delete).toHaveBeenCalledWith('/advertisements/ad1');
    });

    it('should propagate error on delete failure', async () => {
      vi.mocked(api.delete).mockRejectedValue(new Error('Forbidden'));

      await expect(deleteAdvertisement('ad1')).rejects.toThrow('Forbidden');
    });
  });

  // ---------- updateAdvertisementStatus ----------
  describe('updateAdvertisementStatus', () => {
    it('should call PATCH /advertisements/{id}/status with status param', async () => {
      const updated = { ...mockAd, status: 'PAUSED' };
      vi.mocked(api.patch).mockResolvedValue({ data: { code: 200, message: 'success', data: updated } });

      const result = await updateAdvertisementStatus('ad1', 'PAUSED');

      expect(api.patch).toHaveBeenCalledWith('/advertisements/ad1/status', null, {
        params: { status: 'PAUSED' },
      });
      expect(result).toEqual(updated);
    });
  });

  // ---------- getActiveAdvertisements ----------
  describe('getActiveAdvertisements', () => {
    it('should call GET /mobile/advertisements/active with overridden baseURL', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockAd] } });

      const result = await getActiveAdvertisements();

      expect(api.get).toHaveBeenCalledWith('/mobile/advertisements/active', { baseURL: '/api/v1' });
      expect(result).toEqual([mockAd]);
    });
  });
});
