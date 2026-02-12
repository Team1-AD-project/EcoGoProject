import { describe, it, expect, vi, beforeEach } from 'vitest';
import challengeApi from './challengeApi';
import { api } from '../services/auth';

vi.mock('../services/auth', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockChallenge = {
  id: 'c1',
  title: '10km Green Trip',
  description: 'Complete 10km of green trips',
  type: 'GREEN_TRIPS_DISTANCE',
  target: 10,
  reward: 100,
  badge: 'green_warrior',
  icon: 'bike',
  status: 'ACTIVE',
  participants: 25,
  startTime: '2026-01-01',
  endTime: '2026-12-31',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
};

const mockProgress = {
  id: 'p1',
  challengeId: 'c1',
  userId: 'u1',
  status: 'IN_PROGRESS',
  current: 5.2,
  target: 10,
  progressPercent: 52,
  joinedAt: '2026-02-01T00:00:00',
  rewardClaimed: false,
  userNickname: 'Alice',
  userEmail: 'alice@example.com',
};

describe('challengeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- getAllChallenges ----------
  describe('getAllChallenges', () => {
    it('should call GET /challenges and return data array', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockChallenge] } });

      const result = await challengeApi.getAllChallenges();

      expect(api.get).toHaveBeenCalledWith('/challenges');
      expect(result).toEqual([mockChallenge]);
    });

    it('should return empty array when no challenges', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [] } });

      const result = await challengeApi.getAllChallenges();

      expect(result).toEqual([]);
    });
  });

  // ---------- getChallengeById ----------
  describe('getChallengeById', () => {
    it('should call GET /challenges/{id}', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: mockChallenge } });

      const result = await challengeApi.getChallengeById('c1');

      expect(api.get).toHaveBeenCalledWith('/challenges/c1');
      expect(result).toEqual(mockChallenge);
    });

    it('should propagate error for non-existent challenge', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Not Found'));

      await expect(challengeApi.getChallengeById('bad')).rejects.toThrow('Not Found');
    });
  });

  // ---------- createChallenge ----------
  describe('createChallenge', () => {
    it('should call POST /challenges with challenge data', async () => {
      const createData = {
        title: 'New Challenge',
        description: 'Desc',
        type: 'CARBON_SAVED',
        target: 50,
        reward: 200,
        icon: 'leaf',
        status: 'ACTIVE',
        participants: 0,
      };
      vi.mocked(api.post).mockResolvedValue({ data: { code: 200, message: 'success', data: { ...createData, id: 'c2' } } });

      const result = await challengeApi.createChallenge(createData);

      expect(api.post).toHaveBeenCalledWith('/challenges', createData);
      expect(result.id).toBe('c2');
    });
  });

  // ---------- updateChallenge ----------
  describe('updateChallenge', () => {
    it('should call PUT /challenges/{id} with partial data', async () => {
      const updateData = { title: 'Updated Challenge' };
      const updated = { ...mockChallenge, title: 'Updated Challenge' };
      vi.mocked(api.put).mockResolvedValue({ data: { code: 200, message: 'success', data: updated } });

      const result = await challengeApi.updateChallenge('c1', updateData);

      expect(api.put).toHaveBeenCalledWith('/challenges/c1', updateData);
      expect(result.title).toBe('Updated Challenge');
    });
  });

  // ---------- deleteChallenge ----------
  describe('deleteChallenge', () => {
    it('should call DELETE /challenges/{id}', async () => {
      vi.mocked(api.delete).mockResolvedValue({});

      await challengeApi.deleteChallenge('c1');

      expect(api.delete).toHaveBeenCalledWith('/challenges/c1');
    });

    it('should propagate error on delete failure', async () => {
      vi.mocked(api.delete).mockRejectedValue(new Error('Server Error'));

      await expect(challengeApi.deleteChallenge('c1')).rejects.toThrow('Server Error');
    });
  });

  // ---------- getChallengesByStatus ----------
  describe('getChallengesByStatus', () => {
    it('should call GET /challenges/status/{status}', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockChallenge] } });

      const result = await challengeApi.getChallengesByStatus('ACTIVE');

      expect(api.get).toHaveBeenCalledWith('/challenges/status/ACTIVE');
      expect(result).toEqual([mockChallenge]);
    });

    it('should handle EXPIRED status', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [] } });

      const result = await challengeApi.getChallengesByStatus('EXPIRED');

      expect(api.get).toHaveBeenCalledWith('/challenges/status/EXPIRED');
      expect(result).toEqual([]);
    });
  });

  // ---------- getChallengesByType ----------
  describe('getChallengesByType', () => {
    it('should call GET /challenges/type/{type}', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockChallenge] } });

      const result = await challengeApi.getChallengesByType('GREEN_TRIPS_DISTANCE');

      expect(api.get).toHaveBeenCalledWith('/challenges/type/GREEN_TRIPS_DISTANCE');
      expect(result).toEqual([mockChallenge]);
    });

    it('should handle CARBON_SAVED type', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [] } });

      const result = await challengeApi.getChallengesByType('CARBON_SAVED');

      expect(api.get).toHaveBeenCalledWith('/challenges/type/CARBON_SAVED');
      expect(result).toEqual([]);
    });
  });

  // ---------- getChallengeParticipants ----------
  describe('getChallengeParticipants', () => {
    it('should call GET /challenges/{id}/participants and return progress DTOs', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [mockProgress] } });

      const result = await challengeApi.getChallengeParticipants('c1');

      expect(api.get).toHaveBeenCalledWith('/challenges/c1/participants');
      expect(result).toEqual([mockProgress]);
      expect(result[0].progressPercent).toBe(52);
    });

    it('should return empty array when no participants', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { code: 200, message: 'success', data: [] } });

      const result = await challengeApi.getChallengeParticipants('c1');

      expect(result).toEqual([]);
    });
  });
});
