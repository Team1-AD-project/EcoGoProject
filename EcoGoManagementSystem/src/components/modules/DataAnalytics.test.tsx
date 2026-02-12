import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import { DataAnalytics } from './DataAnalytics';
import * as statisticsApi from '@/api/statisticsApi';

vi.mock('@/api/statisticsApi', () => ({
    getAnalyticsSummary: vi.fn(),
}));

describe('DataAnalytics Component', () => {

    const mockSummary = {
        activeUsers7d: 100,
        activeUsers30d: 500,
        totalTrips: 1000,
        totalCarbonSaved: 5000,
        totalRedemptions: 200,
        transportDistribution: { Walking: 60, Bicycle: 40 },
        topUsers: [
            { nickname: 'User1', trips: 10, carbonSaved: 50 }
        ]
    };

    beforeEach(() => {
        vi.clearAllMocks();
        // Mock Radix Select pointer APIs for jsdom
        window.HTMLElement.prototype.hasPointerCapture = vi.fn(() => false);
        window.HTMLElement.prototype.setPointerCapture = vi.fn();
        window.HTMLElement.prototype.releasePointerCapture = vi.fn();
        window.HTMLElement.prototype.scrollIntoView = vi.fn();
    });

    it('renders summary data', async () => {
        (statisticsApi.getAnalyticsSummary as any).mockResolvedValue(mockSummary);
        render(<DataAnalytics />);

        await waitFor(() => {
            // Check title
            expect(screen.getByText(/TESTING:/i)).toBeInTheDocument();
            // Check data (active users 7d default)
            expect(screen.getByText('100')).toBeInTheDocument();
            expect(screen.getByText('5,000 kg')).toBeInTheDocument();
        });
    });

    it('switches time period', async () => {
        (statisticsApi.getAnalyticsSummary as any).mockResolvedValue(mockSummary);
        const user = userEvent.setup();
        render(<DataAnalytics />);

        await waitFor(() => expect(screen.getByText('100')).toBeInTheDocument());

        // Switch to 30d
        // The select trigger shows "7d" initially.
        // The SelectValue doesn't render 7d text directly maybe? "Last 7 Days" is option text.

        const trigger = screen.getByRole('combobox');
        await user.click(trigger);

        // Select content is rendered in a portal; scope query to it
        const content = document.querySelector('[data-slot="select-content"]') as HTMLElement | null;
        expect(content).toBeTruthy();
        const option30 = within(content as HTMLElement).getByText('Last 30 Days');
        await user.click(option30);

        await waitFor(() => {
            // Should show 30d users: 500
            expect(screen.getByText('500')).toBeInTheDocument();
        });
    });

    it('handles error state', async () => {
        (statisticsApi.getAnalyticsSummary as any).mockRejectedValue(new Error('Failed to fetch'));
        render(<DataAnalytics />);

        await waitFor(() => {
            expect(screen.getByText(/Error: Failed to fetch/i)).toBeInTheDocument();
        });
    });
});
