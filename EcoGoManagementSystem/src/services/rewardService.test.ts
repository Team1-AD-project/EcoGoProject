import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
    fetchRewards,
    createReward,
    updateReward,
    deleteReward,
    fetchVouchers,
    createVoucher,
    updateVoucher,
    deleteVoucher,
    fetchOrders,
    fetchCategories,
    CreateRewardRequest
} from './rewardService';
import { api } from './auth';

vi.mock('./auth', () => ({
    api: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

describe('rewardService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    // --- Rewards ---
    describe('fetchRewards', () => {
        it('should call GET /goods with parameters', async () => {
            const mockData = { code: 200, data: [], pagination: {} };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchRewards(2, 5);

            expect(api.get).toHaveBeenCalledWith(
                expect.stringContaining('/goods?page=2&size=5'),
                { baseURL: '/api/v1' }
            );
            expect(result).toEqual(mockData);
        });
    });

    describe('createReward', () => {
        it('should call POST /goods', async () => {
            const payload: CreateRewardRequest = {
                name: 'Test Reward',
                description: 'Desc',
                price: 10,
                stock: 100,
                category: 'Cat',
                brand: 'Brand',
                imageUrl: 'url',
                isActive: true,
                isForRedemption: true,
                redemptionPoints: 50,
                vipLevelRequired: 0,
                redemptionLimit: 1
            };
            const mockResponse = { code: 200, message: 'created' };
            vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

            const result = await createReward(payload);

            expect(api.post).toHaveBeenCalledWith('/goods', payload, { baseURL: '/api/v1' });
            expect(result).toEqual(mockResponse);
        });
    });

    describe('updateReward', () => {
        it('should call PUT /goods/{id}', async () => {
            const payload = { name: 'Updated' };
            const mockResponse = { code: 200 };
            vi.mocked(api.put).mockResolvedValue({ data: mockResponse });

            const result = await updateReward('r1', payload);

            expect(api.put).toHaveBeenCalledWith('/goods/r1', payload, { baseURL: '/api/v1' });
            expect(result).toEqual(mockResponse);
        });
    });

    describe('deleteReward', () => {
        it('should call DELETE /goods/{id}', async () => {
            const mockResponse = { code: 200 };
            vi.mocked(api.delete).mockResolvedValue({ data: mockResponse });

            const result = await deleteReward('r1');

            expect(api.delete).toHaveBeenCalledWith('/goods/r1', { baseURL: '/api/v1' });
            expect(result).toEqual(mockResponse);
        });
    });

    // --- Vouchers ---
    describe('fetchVouchers', () => {
        it('should call GET /goods/admin/vouchers', async () => {
            const mockData = { code: 200, data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchVouchers();

            expect(api.get).toHaveBeenCalledWith(
                expect.stringContaining('/goods/admin/vouchers'),
                { baseURL: '/api/v1' }
            );
            expect(result).toEqual(mockData);
        });
    });

    describe('createVoucher', () => {
        it('should call POST /goods/admin/vouchers', async () => {
            const payload = {} as CreateRewardRequest;
            const mockResponse = { code: 200 };
            vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

            await createVoucher(payload);

            expect(api.post).toHaveBeenCalledWith('/goods/admin/vouchers', payload, { baseURL: '/api/v1' });
        });
    });

    describe('updateVoucher', () => {
        it('should call PUT /goods/admin/vouchers/{id}', async () => {
            const payload = { name: 'Upd' };
            const mockResponse = { code: 200 };
            vi.mocked(api.put).mockResolvedValue({ data: mockResponse });

            await updateVoucher('v1', payload);

            expect(api.put).toHaveBeenCalledWith('/goods/admin/vouchers/v1', payload, { baseURL: '/api/v1' });
        });
    });

    describe('deleteVoucher', () => {
        it('should call DELETE /goods/admin/vouchers/{id}', async () => {
            const mockResponse = { code: 200 };
            vi.mocked(api.delete).mockResolvedValue({ data: mockResponse });

            await deleteVoucher('v1');

            expect(api.delete).toHaveBeenCalledWith('/goods/admin/vouchers/v1', { baseURL: '/api/v1' });
        });
    });

    // --- Orders & Categories ---
    describe('fetchOrders', () => {
        it('should call GET /orders with pagination', async () => {
            const mockData = { code: 200, data: {} };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchOrders(1, 10);

            expect(api.get).toHaveBeenCalledWith('/orders?page=1&size=10', { baseURL: '/api/v1' });
            expect(result).toEqual(mockData);
        });
    });

    describe('fetchCategories', () => {
        it('should call GET /goods/categories', async () => {
            const mockData = { code: 200, data: { categories: [] } };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchCategories();

            expect(api.get).toHaveBeenCalledWith(
                expect.stringContaining('/goods/categories'),
                { baseURL: '/api/v1' }
            );
            expect(result).toEqual(mockData);
        });
    });
});
