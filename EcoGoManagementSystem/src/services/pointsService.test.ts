import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchUserTransactions, adjustUserPoints, fetchPointsSummary, fetchAllPointsHistory } from './pointsService';
import { api } from './auth';

vi.mock('./auth', () => ({
    api: {
        get: vi.fn(),
        post: vi.fn(),
    },
}));

describe('pointsService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('fetchUserTransactions', () => {
        it('should call GET /points/user/{userId}/history', async () => {
            const mockData = { code: 200, data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchUserTransactions('u1');

            expect(api.get).toHaveBeenCalledWith('/points/user/u1/history');
            expect(result).toEqual(mockData);
        });
    });

    describe('fetchPointsSummary', () => {
        it('should call GET /points/all', async () => {
            const mockData = { code: 200, data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchPointsSummary();

            expect(api.get).toHaveBeenCalledWith('/points/all');
            expect(result).toEqual(mockData);
        });
    });

    describe('fetchAllPointsHistory', () => {
        it('should call GET /points/history/all', async () => {
            const mockData = { code: 200, data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockData });

            const result = await fetchAllPointsHistory();

            expect(api.get).toHaveBeenCalledWith('/points/history/all');
            expect(result).toEqual(mockData);
        });
    });

    describe('adjustUserPoints', () => {
        it('should call POST /users/{userid}/points/adjust with payload', async () => {
            const payload = {
                points: 100,
                source: 'admin',
                description: 'Bonus',
                reason: 'Good behavior'
            };
            const mockResponse = { code: 200, message: 'adjusted' };
            vi.mocked(api.post).mockResolvedValue({ data: mockResponse });

            const result = await adjustUserPoints('u1', payload);

            expect(api.post).toHaveBeenCalledWith('/users/u1/points/adjust', payload);
            expect(result).toEqual(mockResponse);
        });
    });
});
