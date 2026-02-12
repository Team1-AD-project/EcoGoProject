import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchAllTrips, fetchUserTrips } from './tripService';
import { api } from './auth';

vi.mock('./auth', () => ({
    api: {
        get: vi.fn(),
    },
}));

describe('tripService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('fetchAllTrips', () => {
        it('should call GET /trips/all and return data on success', async () => {
            const mockTrips = [{ id: 't1', distance: 10 }];
            const mockResponse = { code: 200, message: 'success', data: mockTrips };
            vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

            const result = await fetchAllTrips();

            expect(api.get).toHaveBeenCalledWith('/trips/all');
            expect(result).toEqual(mockTrips);
        });

        it('should return empty array if response code is not 200', async () => {
            const mockResponse = { code: 400, message: 'fail', data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

            const result = await fetchAllTrips();

            expect(result).toEqual([]);
        });

        it('should return empty array on exception', async () => {
            vi.mocked(api.get).mockRejectedValue(new Error('Network error'));

            const result = await fetchAllTrips();

            expect(result).toEqual([]);
        });
    });

    describe('fetchUserTrips', () => {
        it('should call GET /trips/user/{userid} and return data', async () => {
            const userid = 'u1';
            const mockTrips = [{ id: 't1', userId: userid }];
            const mockResponse = { code: 200, message: 'success', data: mockTrips };
            vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

            const result = await fetchUserTrips(userid);

            expect(api.get).toHaveBeenCalledWith(`/trips/user/${userid}`);
            expect(result).toEqual(mockTrips);
        });

        it('should return empty array if response code is not 200', async () => {
            const mockResponse = { code: 500, message: 'error', data: [] };
            vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

            const result = await fetchUserTrips('u1');

            expect(result).toEqual([]);
        });

        it('should return empty array on error', async () => {
            vi.mocked(api.get).mockRejectedValue(new Error('Fail'));

            const result = await fetchUserTrips('u1');

            expect(result).toEqual([]);
        });
    });
});
