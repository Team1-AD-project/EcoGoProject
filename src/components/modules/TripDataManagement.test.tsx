import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { TripDataManagement, parsePolylinePoints, buildRoutePoints, hasValidPoint } from './TripDataManagement';
import { fetchUserList } from '@/services/userService';
import { fetchAllTrips, fetchUserTrips } from '@/services/tripService';
import { toast } from 'sonner';

// Mock services
vi.mock('@/services/userService', () => ({
    fetchUserList: vi.fn(),
}));

vi.mock('@/services/tripService', () => ({
    fetchAllTrips: vi.fn(),
    fetchUserTrips: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        info: vi.fn(),
        success: vi.fn(),
    }
}));

// Mock Leaflet with hoisted variables
const { mockMap, mockLayerGroup } = vi.hoisted(() => {
    const mockMap = {
        setView: vi.fn().mockReturnThis(),
        remove: vi.fn(),
        invalidateSize: vi.fn(),
        fitBounds: vi.fn(),
    };
    const mockLayerGroup = {
        addTo: vi.fn(),
        clearLayers: vi.fn(),
    };
    mockLayerGroup.addTo.mockReturnValue(mockLayerGroup);

    return { mockMap, mockLayerGroup };
});

vi.mock('leaflet', () => {
    return {
        default: {
            map: vi.fn(() => mockMap),
            tileLayer: vi.fn(() => ({
                addTo: vi.fn(),
            })),
            control: {
                zoom: vi.fn(() => ({
                    addTo: vi.fn(),
                }))
            },
            layerGroup: vi.fn(() => mockLayerGroup),
            divIcon: vi.fn(),
            marker: vi.fn(() => ({
                bindPopup: vi.fn().mockReturnThis(),
                addTo: vi.fn(),
            })),
            polyline: vi.fn(() => ({
                addTo: vi.fn(),
            })),
            latLngBounds: vi.fn(() => ({
                extend: vi.fn(),
                isValid: vi.fn(() => true),
            })),
            Icon: {
                Default: {
                    prototype: { _getIconUrl: vi.fn() },
                    mergeOptions: vi.fn(),
                },
            },
        },
    };
});

// Mock ResizeObserver
globalThis.ResizeObserver = class ResizeObserver {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
};

const mockUsers = [
    { id: '1', userid: 'u1', nickname: 'Alice', vip: { active: true } },
    { id: '2', userid: 'u2', nickname: 'Bob', vip: { active: false } }
] as any[];

const mockTrips = [
    {
        id: 't1',
        userId: 'u1',
        distance: 5.5,
        carbonSaved: 1.2,
        isGreenTrip: true,
        carbonStatus: 'completed',
        startTime: '2026-01-01 10:00',
        startPoint: { lat: 1.3, lng: 103.8 },
        endPoint: { lat: 1.31, lng: 103.81 },
        startLocation: { placeName: 'Home' },
        endLocation: { placeName: 'Work' }
    },
    {
        id: 't2',
        userId: 'u1',
        distance: 3.0,
        carbonSaved: 0.5,
        isGreenTrip: false,
        carbonStatus: 'completed',
        startTime: '2026-01-02 11:00',
        startPoint: { lat: 1.32, lng: 103.82 },
        endPoint: { lat: 1.33, lng: 103.83 },
        startLocation: { placeName: 'Park' },
        endLocation: { placeName: 'Mall' }
    }
] as any[];

describe('TripDataManagement', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        // Mock Pointer Capture methods for Radix UI
        window.HTMLElement.prototype.hasPointerCapture = vi.fn(() => false);
        window.HTMLElement.prototype.setPointerCapture = vi.fn();
        window.HTMLElement.prototype.releasePointerCapture = vi.fn();
        window.HTMLElement.prototype.scrollIntoView = vi.fn();

        vi.mocked(fetchUserList).mockResolvedValue({
            code: 200,
            message: 'success',
            data: { list: mockUsers, total: 2, totalPages: 1, page: 1, size: 10 }
        });
        vi.mocked(fetchAllTrips).mockResolvedValue(mockTrips);
        vi.mocked(fetchUserTrips).mockResolvedValue(mockTrips);
    });

    it('should render and load users', async () => {
        render(<TripDataManagement />);

        await waitFor(() => expect(screen.getByText('Trip Data Management')).toBeInTheDocument());
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());
        expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    it('should filter users by ID search', async () => {
        const user = userEvent.setup();
        render(<TripDataManagement />);
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

        const searchInput = screen.getByPlaceholderText('Search by User ID...');
        await user.type(searchInput, 'u2');
        await user.keyboard('{Enter}');

        // Logic in component: on Enter, setSelectedUserId('u2')
        // It doesn't filter the list locally, but selects the user.

        await waitFor(() => {
            // Accessing the selected user state implies fetchUserTrips is called
            expect(fetchUserTrips).toHaveBeenCalledWith('u2');
        });
    });

    it('should select user and load trips', async () => {
        const user = userEvent.setup();
        render(<TripDataManagement />);
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

        // Click on Alice using accessibility role
        const aliceUser = screen.getByRole('button', { name: /select user alice/i });
        await user.click(aliceUser);

        await waitFor(() => {
            expect(fetchUserTrips).toHaveBeenCalledWith('u1');
        });

        // Check if trip details are shown (distance 5.50)
        await waitFor(() => expect(screen.getByText('5.50')).toBeInTheDocument());

        // Map should be cleared and updated
        await waitFor(() => {
            expect(mockLayerGroup.clearLayers).toHaveBeenCalled();
        });
    });

    it('should change selected trip', async () => {
        const user = userEvent.setup();
        render(<TripDataManagement />);
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

        // Select Alice
        await user.click(screen.getByRole('button', { name: /select user alice/i }));

        // Wait for trips to load
        await waitFor(() => expect(screen.getByText('Home → Work')).toBeInTheDocument());

        // Open Select Dropdown (Trigger)
        // The SelectTrigger usually has the current value or placeholder.
        // Current value for t1 is "Home → Work"
        const trigger = screen.getByRole('combobox');
        await user.click(trigger);

        // Select second trip (Park -> Mall)
        // SelectItem text would be "Park → Mall"
        const secondTripOption = await screen.findByText(/Park → Mall/i);
        await user.click(secondTripOption);

        // Verify content updates
        await waitFor(() => {
            expect(screen.getByText('3.00')).toBeInTheDocument(); // Distance of t2
        });
    });

    describe('Helper Functions', () => {
        describe('parsePolylinePoints', () => {
            it('should parse valid array of points', () => {
                const input = [{ lat: 1.2, lng: 103.8 }, { lat: 1.3, lng: 103.9 }];
                expect(parsePolylinePoints(input)).toEqual([[1.2, 103.8], [1.3, 103.9]]);
            });

            it('should parse JSON string', () => {
                const input = JSON.stringify([{ lat: 1.2, lng: 103.8 }]);
                expect(parsePolylinePoints(input)).toEqual([[1.2, 103.8]]);
            });

            it('should parse double JSON string', () => {
                const inner = JSON.stringify([{ lat: 1.2, lng: 103.8 }]);
                const input = JSON.stringify(inner);
                expect(parsePolylinePoints(input)).toEqual([[1.2, 103.8]]);
            });

            it('should handle invalid inputs gracefully', () => {
                expect(parsePolylinePoints(null)).toEqual([]);
                expect(parsePolylinePoints(undefined)).toEqual([]);
                expect(parsePolylinePoints('invalid json')).toEqual([]);
                expect(parsePolylinePoints([])).toEqual([]);
            });

            it('should filter out invalid coordinates', () => {
                const input = [{ lat: 'nan', lng: 103.8 }, { lat: 1.2, lng: 103.8 }];
                expect(parsePolylinePoints(input)).toEqual([[1.2, 103.8]]);
            });
        });

        describe('buildRoutePoints', () => {
            const trip = {
                startPoint: { lat: 1.0, lng: 100.0 },
                endPoint: { lat: 1.2, lng: 100.2 },
            } as any;

            it('should connect start and end if gap is large', () => {
                const parsed = [[1.1, 100.1]] as any; // Middle point
                // Start (1.0) -> Middle (1.1) -> End (1.2)
                const result = buildRoutePoints(trip, true, true, parsed);
                expect(result).toHaveLength(3);
                expect(result[0]).toEqual([1.0, 100.0]); // Prepend Start
                expect(result[2]).toEqual([1.2, 100.2]); // Append End
            });

            it('should not duplicate points if close enough', () => {
                const parsed = [[1.000001, 100.000001], [1.2, 100.2]] as any;
                // First point is basically start point. Last point is end point.
                const result = buildRoutePoints(trip, true, true, parsed);
                // Should not add start again, should not add end again.
                // Wait, logic says > 0.0001. 
                // 1.0 vs 1.000001 diff is 0.000001 < 0.0001. So no prepend.
                expect(result).toHaveLength(2);
                expect(result[0]).toEqual([1.000001, 100.000001]);
            });

            it('should fallback to straight line if no parsed points', () => {
                const result = buildRoutePoints(trip, true, true, []);
                expect(result).toHaveLength(2);
                expect(result[0]).toEqual([1.0, 100.0]);
                expect(result[1]).toEqual([1.2, 100.2]);
            });
        });

        describe('hasValidPoint', () => {
            it('should return true for valid points', () => {
                expect(hasValidPoint({ lat: 0, lng: 0 })).toBe(true);
            });
            it('should return false for incomplete points', () => {
                expect(hasValidPoint({ lat: 0 } as any)).toBe(false);
                expect(hasValidPoint(null)).toBe(false);
            });
        });
    });

    describe('Component Error Handling & Edge Cases', () => {
        it('should handle user list fetch error', async () => {
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
            vi.mocked(fetchUserList).mockRejectedValueOnce(new Error('API Fail'));

            render(<TripDataManagement />);

            // Initial load fails.
            // We can check if toast.error was called if we mock toast, or check if user list is empty.
            // But component doesn't show "Error" text, just toasts.
            // We can assume empty list.
            await waitFor(() => expect(fetchUserList).toHaveBeenCalled());
            expect(toast.error).toHaveBeenCalledWith("Failed to load users");
            consoleSpy.mockRestore();
        });

        it('should handle trip fetch error', async () => {
            const user = userEvent.setup();
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
            render(<TripDataManagement />);
            await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

            // Mock trip fetch failure
            vi.mocked(fetchUserTrips).mockRejectedValueOnce(new Error('Trip Fail'));

            // Click user
            await user.click(screen.getByText('Alice'));

            await waitFor(() => expect(fetchUserTrips).toHaveBeenCalledWith('u1'));
            expect(toast.error).toHaveBeenCalledWith("Failed to load user trips");
            consoleSpy.mockRestore();
        });

        it('should handle user with no trips', async () => {
            const user = userEvent.setup();
            vi.mocked(fetchUserTrips).mockResolvedValueOnce([]);

            render(<TripDataManagement />);
            await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

            await user.click(screen.getByText('Alice'));

            // "No trips found for User ID: u1" toast would appear.
            // Check that no current trips state is reflected (e.g. selector not shown)
            await waitFor(() => expect(fetchUserTrips).toHaveBeenCalled());
            expect(toast.info).toHaveBeenCalledWith(expect.stringContaining("No trips found for User ID: u1"));
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('should handle user with trips but no completed trips', async () => {
            const user = userEvent.setup();
            const pendingTrips = [{
                id: 't_pending', userId: 'u1', carbonStatus: 'pending', distance: 10
            }];
            vi.mocked(fetchUserTrips).mockResolvedValueOnce(pendingTrips as any);

            render(<TripDataManagement />);
            await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

            await user.click(screen.getByText('Alice'));

            // Should load trips but not select any
            // Logic: setSelectedTripId(null)
            await waitFor(() => expect(fetchUserTrips).toHaveBeenCalled());

            // Check if selector exists (it might, but empty or "hidden" text?)
            // Code: {currentTrips.filter(t => t.carbonStatus !== 'completed').length > 0 && ...}
            // It shows "other incomplete trips hidden" in dropdown content, but dropdown itself?
            // Select logic:
            /*
              <Select value={selectedTripId || ''} ...>
            */
            // It renders Select even if no valid selection? Yes.
            expect(screen.getByRole('combobox')).toBeInTheDocument();
            expect(toast.info).toHaveBeenCalledWith("User has trips, but none are completed.");
        });
    });

    describe('User Interactions', () => {
        it('should refresh user list', async () => {
            const user = userEvent.setup();
            render(<TripDataManagement />);
            await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

            await user.click(screen.getByText('Refresh List'));
            await waitFor(() => expect(fetchUserList).toHaveBeenCalledTimes(2));
        });

        it('should navigate pagination', async () => {
            const user = userEvent.setup();
            vi.mocked(fetchUserList).mockResolvedValue({
                code: 200, message: 'success', data: { list: [], total: 20, totalPages: 2, page: 1, size: 10 }
            });

            render(<TripDataManagement />);
            await waitFor(() => expect(fetchUserList).toHaveBeenCalled());

            const nextBtn = screen.getByText('Next');
            await user.click(nextBtn);

            await waitFor(() => expect(fetchUserList).toHaveBeenCalledWith(2, 20));
        });

        it('should select user via keyboard (Enter)', async () => {
            const user = userEvent.setup();
            render(<TripDataManagement />);
            await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument());

            const aliceRow = screen.getByRole('button', { name: /select user alice/i });
            aliceRow.focus();
            await user.keyboard('{Enter}');

            await waitFor(() => expect(fetchUserTrips).toHaveBeenCalledWith('u1'));
        });

        it('should handle batch stats load error', async () => {
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
            vi.mocked(fetchAllTrips).mockRejectedValueOnce(new Error('Batch Fail'));

            render(<TripDataManagement />);
            await waitFor(() => expect(fetchAllTrips).toHaveBeenCalled());
            expect(consoleSpy).toHaveBeenCalledWith("Failed to load batch stats", expect.any(Error));
            consoleSpy.mockRestore();
        });
    });
});
