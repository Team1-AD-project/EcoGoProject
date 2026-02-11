import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  fetchActivities,
  fetchActivitiesByStatus,
  fetchActivityById,
  createActivity,
  updateActivity,
  deleteActivity,
  publishActivity,
} from './activityService';
import { api } from './auth';

// Mock the auth module's api instance
vi.mock('./auth', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockActivity = {
  id: 'a1',
  title: 'Beach Cleanup',
  description: 'Clean the beach',
  type: 'OFFLINE' as const,
  status: 'DRAFT' as const,
  rewardCredits: 50,
  maxParticipants: 30,
  currentParticipants: 5,
  participantIds: ['u1', 'u2'],
  startTime: '2026-03-01T09:00:00',
  endTime: '2026-03-01T12:00:00',
  latitude: 1.35,
  longitude: 103.82,
  locationName: 'East Coast Park',
  createdAt: '2026-02-01T00:00:00',
  updatedAt: '2026-02-01T00:00:00',
};

describe('activityService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ---------- fetchActivities ----------
  describe('fetchActivities', () => {
    it('should call GET /activities and return response data', async () => {
      const mockResponse = { data: { code: 200, message: 'success', data: [mockActivity] } };
      vi.mocked(api.get).mockResolvedValue(mockResponse);

      const result = await fetchActivities();

      expect(api.get).toHaveBeenCalledWith('/activities');
      expect(result).toEqual({ code: 200, message: 'success', data: [mockActivity] });
    });

    it('should propagate error when api fails', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network Error'));

      await expect(fetchActivities()).rejects.toThrow('Network Error');
    });
  });

  // ---------- fetchActivitiesByStatus ----------
  describe('fetchActivitiesByStatus', () => {
    it('should call GET /activities/status/{status}', async () => {
      const mockResponse = { data: { code: 200, message: 'success', data: [mockActivity] } };
      vi.mocked(api.get).mockResolvedValue(mockResponse);

      const result = await fetchActivitiesByStatus('PUBLISHED');

      expect(api.get).toHaveBeenCalledWith('/activities/status/PUBLISHED');
      expect(result).toEqual({ code: 200, message: 'success', data: [mockActivity] });
    });

    it('should handle ONGOING status', async () => {
      const mockResponse = { data: { code: 200, message: 'success', data: [] } };
      vi.mocked(api.get).mockResolvedValue(mockResponse);

      await fetchActivitiesByStatus('ONGOING');

      expect(api.get).toHaveBeenCalledWith('/activities/status/ONGOING');
    });
  });

  // ---------- fetchActivityById ----------
  describe('fetchActivityById', () => {
    it('should call GET /activities/{id} and return data', async () => {
      const mockResponse = { data: { code: 200, message: 'success', data: mockActivity } };
      vi.mocked(api.get).mockResolvedValue(mockResponse);

      const result = await fetchActivityById('a1');

      expect(api.get).toHaveBeenCalledWith('/activities/a1');
      expect(result).toEqual({ code: 200, message: 'success', data: mockActivity });
    });

    it('should propagate error for non-existent id', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Not Found'));

      await expect(fetchActivityById('nonexistent')).rejects.toThrow('Not Found');
    });
  });

  // ---------- createActivity ----------
  describe('createActivity', () => {
    it('should call POST /activities with data', async () => {
      const createData = {
        title: 'New Activity',
        description: 'Description',
        type: 'ONLINE' as const,
        rewardCredits: 100,
        maxParticipants: 50,
        startTime: '2026-04-01T09:00:00',
        endTime: '2026-04-01T12:00:00',
      };
      const created = { ...mockActivity, ...createData, id: 'a2' };
      const mockResponse = { data: { code: 200, message: 'success', data: created } };
      vi.mocked(api.post).mockResolvedValue(mockResponse);

      const result = await createActivity(createData);

      expect(api.post).toHaveBeenCalledWith('/activities', createData);
      expect(result).toEqual({ code: 200, message: 'success', data: created });
    });

    it('should propagate validation error', async () => {
      vi.mocked(api.post).mockRejectedValue(new Error('Validation failed'));

      await expect(
        createActivity({ title: '', description: '', type: 'ONLINE', rewardCredits: 0, maxParticipants: 0, startTime: '', endTime: '' })
      ).rejects.toThrow('Validation failed');
    });
  });

  // ---------- updateActivity ----------
  describe('updateActivity', () => {
    it('should call PUT /activities/{id} with update data', async () => {
      const updateData = { title: 'Updated Title' };
      const updated = { ...mockActivity, title: 'Updated Title' };
      const mockResponse = { data: { code: 200, message: 'success', data: updated } };
      vi.mocked(api.put).mockResolvedValue(mockResponse);

      const result = await updateActivity('a1', updateData);

      expect(api.put).toHaveBeenCalledWith('/activities/a1', updateData);
      expect(result).toEqual({ code: 200, message: 'success', data: updated });
    });
  });

  // ---------- deleteActivity ----------
  describe('deleteActivity', () => {
    it('should call DELETE /activities/{id}', async () => {
      vi.mocked(api.delete).mockResolvedValue({});

      await deleteActivity('a1');

      expect(api.delete).toHaveBeenCalledWith('/activities/a1');
    });

    it('should propagate error on delete failure', async () => {
      vi.mocked(api.delete).mockRejectedValue(new Error('Forbidden'));

      await expect(deleteActivity('a1')).rejects.toThrow('Forbidden');
    });
  });

  // ---------- publishActivity ----------
  describe('publishActivity', () => {
    it('should call POST /activities/{id}/publish', async () => {
      const published = { ...mockActivity, status: 'PUBLISHED' as const };
      const mockResponse = { data: { code: 200, message: 'success', data: published } };
      vi.mocked(api.post).mockResolvedValue(mockResponse);

      const result = await publishActivity('a1');

      expect(api.post).toHaveBeenCalledWith('/activities/a1/publish', {});
      expect(result).toEqual({ code: 200, message: 'success', data: published });
    });

    it('should propagate error when publish fails', async () => {
      vi.mocked(api.post).mockRejectedValue(new Error('Cannot publish'));

      await expect(publishActivity('a1')).rejects.toThrow('Cannot publish');
    });
  });
});
