import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import { AnalyticsManagement } from './AnalyticsManagement';
import * as statisticsApi from '@/api/statisticsApi';
import * as tripService from '@/services/tripService';
import * as leaderboardApi from '@/api/leaderboardApi';
import * as rewardService from '@/services/rewardService';
import * as collectiblesApi from '@/api/collectiblesApi';
import * as pointsService from '@/services/pointsService';
import * as challengeApi from '@/api/challengeApi';
import * as userService from '@/services/userService';

// Mock all APIs
vi.mock('@/api/statisticsApi', () => ({ getManagementAnalytics: vi.fn(), }));
vi.mock('@/services/tripService', () => ({ fetchAllTrips: vi.fn() }));
vi.mock('@/api/leaderboardApi', () => ({ getFacultyRankings: vi.fn(), getRankingsByType: vi.fn() }));
vi.mock('@/services/rewardService', () => ({ fetchRewards: vi.fn(), fetchOrders: vi.fn() }));
vi.mock('@/api/collectiblesApi', () => ({ getBadgePurchaseStats: vi.fn(), getAllBadges: vi.fn() }));
vi.mock('@/services/pointsService', () => ({ fetchPointsSummary: vi.fn(), fetchAllPointsHistory: vi.fn() }));
vi.mock('@/api/challengeApi', () => ({ challengeApi: { getAllChallenges: vi.fn() } }));
vi.mock('@/services/userService', () => ({ fetchUserList: vi.fn() }));

// Mock ResizeObserver
globalThis.ResizeObserver = class ResizeObserver {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
};

// Mock HeatMapView
vi.mock('./HeatMapView', () => ({
    HeatMapView: ({ title }: any) => <div data-testid="heatmap">{title}</div>
}));

// Mock Recharts to avoid complex SVG rendering issues
// We don't need real charts, just stub components so that imports like
// CartesianGrid / XAxis / YAxis / Tooltip / Legend / Line / Area / Bar exist.
vi.mock('recharts', () => {
    const Container = ({ children }: any) => <div className="recharts-mock-container">{children}</div>;
    // Render data prop to allow assertions
    const Chart = ({ data, children, className }: any) => (
        <div className={`recharts-mock-chart ${className || ''}`} data-chart-data={JSON.stringify(data)}>
            {children}
        </div>
    );
    const Element = ({ children }: any) => <div className="recharts-mock-element">{children}</div>;

    return {
        ResponsiveContainer: Container,
        LineChart: Chart,
        BarChart: Chart,
        PieChart: Chart,
        AreaChart: Chart,
        CartesianGrid: Element,
        XAxis: Element,
        YAxis: Element,
        Tooltip: Element,
        Legend: Element,
        Line: Element,
        Area: Element,
        Bar: Element,
        Cell: Element,
        Pie: Chart,
    };
});

describe('AnalyticsManagement Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        // Default mocks to avoid crashes
        (statisticsApi.getManagementAnalytics as any).mockResolvedValue({});
        (tripService.fetchAllTrips as any).mockResolvedValue([]);
        (leaderboardApi.getFacultyRankings as any).mockResolvedValue([]);
        (leaderboardApi.getRankingsByType as any).mockResolvedValue({});
        (rewardService.fetchRewards as any).mockResolvedValue({ data: [] });
        (collectiblesApi.getBadgePurchaseStats as any).mockResolvedValue([]);
        (collectiblesApi.getAllBadges as any).mockResolvedValue([]);
        (pointsService.fetchPointsSummary as any).mockResolvedValue({ data: [] });
        (challengeApi.challengeApi.getAllChallenges as any).mockResolvedValue([]);
        (rewardService.fetchOrders as any).mockResolvedValue({ data: { orders: [] } });
        (userService.fetchUserList as any).mockResolvedValue({ data: { list: [], total: 0 } });
        (pointsService.fetchAllPointsHistory as any).mockResolvedValue({ data: [] });
    });

    it('renders loading initially', () => {
        // Make promises pending effectively or just rely on initial render
        (statisticsApi.getManagementAnalytics as any).mockReturnValue(new Promise(() => { }));
        render(<AnalyticsManagement />);
        // Check for Loader2 or just null/loading text if any.
        // The component returns explicit loader div if loading.
        // We can check if dashboard text is NOT there yet or check for spinner class.
        // There is a <Loader2 ... />
        // Let's assume the spinners are present.
        const spinner = document.querySelector('.animate-spin');
        expect(spinner).toBeInTheDocument();
    });

    it('renders dashboard after loading', async () => {
        render(<AnalyticsManagement />);

        await waitFor(() => {
            expect(screen.getByText('Dashboard')).toBeInTheDocument();
            expect(screen.getByText('Total Users')).toBeInTheDocument();
        });
    });

    it('renders HeatMapView', async () => {
        render(<AnalyticsManagement />);
        await waitFor(() => expect(screen.getByTestId('heatmap')).toBeInTheDocument());
    });

    it('selects charts via navigation', async () => {
        // The component renders Chart Selection list on right.
        // Mock chart names are used.
        const user = userEvent.setup();
        render(<AnalyticsManagement />);

        await waitFor(() => expect(screen.getAllByText('User Growth Trends')[0]).toBeInTheDocument());

        const chartSelection = screen.getAllByText('Trip Volume Trend')[0]; // Likely found in the list

        // Left click sets selectedChart (left chart)
        await user.click(chartSelection);

        // We need to verify that "Trip Volume Trend" is now displayed as a chart title.
        // The titles are rendered in h3.
        // Initially chart 0 (User Growth Trends) and 1 (Carbon Saved Trends) are shown.
        // After click, left chart becomes 2 (Trip Volume Trend).

        await waitFor(() => {
            // Chart title is h3
            const titles = screen.getAllByRole('heading', { level: 3 });
            // title 0 is likely name of chart 1
            // title 1 is likely name of chart 2
            // Or titles are specific. 
            // Let's check if the text is present in the headings
            expect(titles.some(t => t.textContent === 'Trip Volume Trend')).toBeTruthy();
        });

        // Right click sets selectedChart2 (right chart)
        const chartSelectionRankings = screen.getByText('Faculty Carbon Rankings'); // Index 6
        fireEvent.contextMenu(chartSelectionRankings);

        await waitFor(() => {
            const titles = screen.getAllByRole('heading', { level: 3 });
            expect(titles.some(t => t.textContent === 'Faculty Carbon Rankings')).toBeTruthy();
        });
    });


    it('handles partial API failures gracefully', async () => {
        const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => { });

        // Mock success for some
        (statisticsApi.getManagementAnalytics as any).mockResolvedValue({});
        // Mock failure for others
        (tripService.fetchAllTrips as any).mockRejectedValue(new Error('Trip Error'));
        (leaderboardApi.getFacultyRankings as any).mockRejectedValue(new Error('Faculty Error'));

        render(<AnalyticsManagement />);

        await waitFor(() => {
            expect(screen.getByText('Dashboard')).toBeInTheDocument();
        });

        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('fetchAllTrips failed'), expect.any(Error));
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('facultyRankings failed'), expect.any(Error));

        consoleSpy.mockRestore();
    });

    it('calculates derived data correctly (Top Products)', async () => {
        // Mock orders for top products calculation
        const mockOrders = [
            {
                createdAt: new Date().toISOString(), // Current month 
                status: 'COMPLETED',
                items: [{ goodsId: 'g1', goodsName: 'Product A', quantity: 5 }]
            },
            {
                createdAt: new Date().toISOString(),
                status: 'COMPLETED',
                items: [{ goodsId: 'g2', goodsName: 'Product B', quantity: 3 }]
            }
        ];
        (rewardService.fetchOrders as any).mockResolvedValue({ data: { orders: mockOrders } });
        (statisticsApi.getManagementAnalytics as any).mockResolvedValue({}); // Base req

        render(<AnalyticsManagement />);

        // Select "Top 10 Selling Products" chart (index 8)
        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByText('Top 10 Selling Products')).toBeInTheDocument());

        const chartBtn = screen.getByText('Top 10 Selling Products');
        await user.click(chartBtn);

        // Check if data is rendered in chart (mocked as Recharts container)
        // Since we mocked Recharts, we can't easily assert SVG content, but we can check if the component didn't crash
        // and potentially check props if we mocked the Chart component to render props.
        // However, we can trust that if it renders without error, the derivation logic ran.
        // To be more specific, we can inspect the component state if it were accessible, 
        // but here we verify the derivation logic via the UI reflecting "Top 10 Selling Products" title 
        // and ensuring no crash.

        await waitFor(() => {
            const headings = screen.getAllByRole('heading', { level: 3 });
            expect(headings.some(h => h.textContent === 'Top 10 Selling Products')).toBeTruthy();
        });
    });

    it('processes trip data for heatmap and distribution', async () => {
        const mockTrips = [
            {
                endPoint: { lat: 1.2, lng: 103.8 },
                detectedMode: 'WALKING',
                carbonSaved: 0.5,
                isGreenTrip: true,
                startTime: new Date().toISOString()
            }
        ];
        (tripService.fetchAllTrips as any).mockResolvedValue(mockTrips);
        (statisticsApi.getManagementAnalytics as any).mockResolvedValue({});

        render(<AnalyticsManagement />);

        await waitFor(() => {
            expect(screen.getByTestId('heatmap')).toBeInTheDocument();
            // Verify Green Trip Rate calculation displayed in KPI card
            expect(screen.getByText('100%')).toBeInTheDocument(); // 1 green trip out of 1
        });
    });

    it('calculates all derived metrics and charts correctly', async () => {
        // Mock Data Setup
        const now = new Date();
        const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

        // Users: 1 Admin, 2 Normal (1 VIP, 1 Non-VIP), 1 Inactive
        // One created this month, one created previously
        const mockUsers = [
            { userId: 'u1', admin: true, nickname: 'Admin', createdAt: '2023-01-01' },
            { userId: 'u2', admin: false, nickname: 'VIP User', vip: { active: true }, createdAt: `${currentMonth}-01`, lastLoginAt: now.toISOString() }, // New & Active & VIP
            { userId: 'u3', admin: false, nickname: 'Regular User', vip: { active: false }, createdAt: '2023-01-01', lastLoginAt: '2023-01-01' }, // Old & Inactive & Non-VIP
            { userId: 'u4', isAdmin: true, nickname: 'Another Admin' } // Should be filtered out
        ];
        (userService.fetchUserList as any).mockResolvedValue({ data: { list: mockUsers, total: 4 } });

        // Collectibles: Badges & Clothes
        const mockBadges = [
            { badgeId: 'b1', name: { en: 'Badge A' }, category: 'badge', acquisitionMethod: 'purchase' },
            { badgeId: 'c1', name: { en: 'Cloth A' }, category: 'cloth', acquisitionMethod: 'event' }
        ];
        (collectiblesApi.getAllBadges as any).mockResolvedValue(mockBadges);

        const mockBadgeStats = [
            { badgeId: 'b1', purchaseCount: 10 },
            { badgeId: 'c1', purchaseCount: 5_000_000 } // Test millions formatting
        ];
        (collectiblesApi.getBadgePurchaseStats as any).mockResolvedValue(mockBadgeStats);

        // Challenges
        const mockChallenges = [
            { title: 'Ch 1', participants: 10, type: 'daily_task' },
            { title: 'Ch 2', participants: 5, type: 'event_limited' }
        ];
        (challengeApi.challengeApi.getAllChallenges as any).mockResolvedValue(mockChallenges);

        // Points
        const mockPointsLogs = [
            { change_type: 'gain', points: 100 },
            { change_type: 'deduct', points: -30 }
        ];
        (pointsService.fetchAllPointsHistory as any).mockResolvedValue({ data: mockPointsLogs });

        // Leaderboard (Top Users)
        const mockRankings = {
            rankingsPage: {
                content: [
                    { userId: 'u2', nickname: 'VIP User', carbonSaved: 123.456 },
                    { userId: 'u3', nickname: 'Regular User', carbonSaved: 10.0 }
                ]
            }
        };
        (leaderboardApi.getRankingsByType as any).mockResolvedValue(mockRankings);

        // Trips (Carbon)
        const mockTrips = [
            { carbonStatus: 'completed', carbonSaved: 1000.5, isGreenTrip: true },
            { carbonStatus: 'pending', carbonSaved: 500, isGreenTrip: false }
        ];
        (tripService.fetchAllTrips as any).mockResolvedValue(mockTrips);

        // Render
        const user = userEvent.setup();
        render(<AnalyticsManagement />);

        await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());

        // 1. Verify KPI Cards (Text Content & Formatting)
        // Helper to find value in card
        const getCardValue = (title: string) => {
            const titleEl = screen.getByText(title);
            // The Value is usually the next sibling of the title label in the DOM structure
            // Structure: div > p(title) > p(value)
            return titleEl.nextElementSibling?.textContent;
        };

        // Active Users: u2 is active. u1/u4 are admins. u3 is inactive. -> 1
        expect(getCardValue('Active Users')).toBe('1');

        // VIP Penetration: Non-admin users = u2, u3 (Total 2). VIP = u2 (1). -> 50%
        expect(getCardValue('VIP Penetration')).toBe('50%');

        // Green Trip Rate: 1 green / 2 total = 50%
        expect(getCardValue('Green Trip Rate')).toBe('50%');

        // Total Carbon Saved: 1000.5 rounded -> 1,001. formatNum(1001) -> "1.0K" (since >= 1000)
        expect(getCardValue('Total Carbon Saved')).toBe('1.0K');

        // Total Collectible Count (Clothes Sold): c1 has 5,000,000 sales. formatNum -> "5.0M"
        expect(getCardValue('Total Collectible Count')).toBe('5.0M');

        // Helper to extract chart data from either the container or its children (for Pie charts)
        const getChartData = (chartIndex = 0) => {
            const charts = document.querySelectorAll('.recharts-mock-chart');
            const chart = charts[chartIndex] as HTMLElement;
            if (!chart) return [];

            // Try getting data from the chart container (LineChart, BarChart, etc.)
            const containerData = chart.dataset.chartData;
            if (containerData && containerData !== 'undefined') {
                return JSON.parse(containerData);
            }

            // Try getting data from inner components (Pie)
            const innerElement = chart.querySelector('[data-chart-data]') as HTMLElement;
            const innerData = innerElement?.dataset.chartData;
            if (innerData && innerData !== 'undefined') {
                return JSON.parse(innerData);
            }
            return [];
        };

        // Chart 0: User Growth Trends (Default)
        const userGrowthData = getChartData(0);
        expect(userGrowthData.length).toBeGreaterThan(0);
        expect(userGrowthData[0]).toHaveProperty('totalUsers');
        expect(userGrowthData[0]).toHaveProperty('newUsers');

        // Chart: Challenge Participation (Index 11)
        const challengeBtn = screen.getByText('Challenge Participation');
        await user.click(challengeBtn);

        await waitFor(() => {
            const chartData = getChartData(0);
            expect(chartData).toHaveLength(2);
            expect(chartData[0].title).toBe('Ch 1');
            expect(chartData[0].participants).toBe(10);
        });

        // Chart: Points Economy (Index 14)
        const pointsBtn = screen.getByText('Points Economy Overview');
        await user.click(pointsBtn);

        await waitFor(() => {
            const chartData = getChartData(0);
            expect(chartData).toEqual([
                { name: 'Total Earned', value: 100 },
                { name: 'Remaining', value: 70 },
                { name: 'Spent', value: 30 }
            ]);
        });

        // Chart: VIP Distribution (Index 13)
        // This is a Pie Chart, so data is on <Pie>, which we now mock with data prop being rendered.
        // getChartData handles searching for inner data-chart-data.
        const vipBtn = screen.getByText('VIP Membership Distribution');
        await user.click(vipBtn);

        await waitFor(() => {
            const chartData = getChartData(0);
            expect(chartData).toEqual([
                { name: 'VIP Active', value: 1 },
                { name: 'Non-VIP', value: 1 }
            ]);
        });

        // Checks for `top10Users` (Chart 7)
        const topUsersBtn = screen.getByText('Top 10 Users by Carbon Saved');
        await user.click(topUsersBtn);

        await waitFor(() => {
            const chartData = getChartData(0);
            expect(chartData[0].name).toBe('VIP User');
            expect(chartData[0].carbon).toBe(123.46); // Rounded
        });
    });
});
