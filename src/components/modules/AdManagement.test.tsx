import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import { AdManagement } from './AdManagement';
import * as advertisementApi from '@/api/advertisementApi';

// Mock the API module
vi.mock('@/api/advertisementApi', () => ({
    getAllAdvertisements: vi.fn(),
    createAdvertisement: vi.fn(),
    updateAdvertisement: vi.fn(),
    deleteAdvertisement: vi.fn(),
    updateAdvertisementStatus: vi.fn(),
}));

// Mock ResizeObserver for Recharts or other responsive components if needed, though AdManagement seems simple
globalThis.ResizeObserver = class ResizeObserver {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
};

describe('AdManagement Component', () => {
    const mockAds = [
        {
            id: '1',
            name: 'Ad 1',
            description: 'Description 1',
            imageUrl: 'http://example.com/1.jpg',
            linkUrl: 'http://example.com/1',
            position: 'banner',
            status: 'Active',
            startDate: '2023-01-01T00:00:00.000Z',
            endDate: '2023-12-31T00:00:00.000Z',
            impressions: 1000,
            clicks: 50,
            clickRate: 5.0,
            createdAt: '2023-01-01', // Added missing required fields based on probable type definition
            updatedAt: '2023-01-01',
        },
        {
            id: '2',
            name: 'Ad 2',
            description: 'Description 2',
            imageUrl: 'http://example.com/2.jpg',
            linkUrl: 'http://example.com/2',
            position: 'sidebar',
            status: 'Inactive',
            startDate: '2023-01-01T00:00:00.000Z',
            endDate: '2023-12-31T00:00:00.000Z',
            impressions: 500,
            clicks: 10,
            clickRate: 2.0,
            createdAt: '2023-01-01',
            updatedAt: '2023-01-01',
        },
    ];

    const mockPageData = {
        content: mockAds,
        totalElements: 2,
        totalPages: 1,
        size: 10,
        number: 0,
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        (advertisementApi.getAllAdvertisements as any).mockReturnValue(new Promise(() => { /* intentional no-op */ })); // Never resolves
        render(<AdManagement />);
        expect(screen.getByText('Loading advertisements...')).toBeInTheDocument();
    });

    it('renders ads list after loading', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        render(<AdManagement />);

        await waitFor(() => {
            expect(screen.getByText('Ad 1')).toBeInTheDocument();
            expect(screen.getByText('Ad 2')).toBeInTheDocument();
        });
        expect(screen.queryByText('Loading advertisements...')).not.toBeInTheDocument();
    });

    it('renders empty state when no ads', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue({ ...mockPageData, content: [], totalElements: 0 });
        render(<AdManagement />);

        await waitFor(() => {
            expect(screen.getByText('No advertisements found')).toBeInTheDocument();
        });
    });

    it('handles search input', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        const user = userEvent.setup();
        render(<AdManagement />);

        const searchInput = await screen.findByPlaceholderText('Search by ad name...');
        await user.type(searchInput, 'Test');

        // Debounce wait
        await waitFor(() => {
            expect(advertisementApi.getAllAdvertisements).toHaveBeenLastCalledWith('Test', 0, 6);
        }, { timeout: 1000 });
    });

    it('opens add dialog and creates ad', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        (advertisementApi.createAdvertisement as any).mockResolvedValue({ code: 200, message: 'Success' });
        const user = userEvent.setup();
        render(<AdManagement />);

        await waitFor(() => expect(screen.getByText('Ad 1')).toBeInTheDocument());

        const addButton = screen.getByText('Add Advertisement');
        await user.click(addButton);

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Add New Advertisement')).toBeInTheDocument();

        // Scope queries to the dialog to avoid finding the background search input
        const inputs = within(dialog).getAllByRole('textbox');
        // inputs[0] should be Name because it's the first textbox in the dialog

        await user.type(inputs[0], 'New Ad Campaign');

        // Dates are pre-filled in component state, so validation should pass after adding name
        const createBtn = within(dialog).getByRole('button', { name: 'Create' });

        await waitFor(() => expect(createBtn).toBeEnabled());
        await user.click(createBtn);

        await waitFor(() => {
            expect(advertisementApi.createAdvertisement).toHaveBeenCalled();
        });
    });

    it('deletes an ad', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        (advertisementApi.deleteAdvertisement as any).mockResolvedValue({ code: 200 });
        const user = userEvent.setup();
        render(<AdManagement />);

        await waitFor(() => expect(screen.getByText('Ad 1')).toBeInTheDocument());

        const deleteButtons = screen.getAllByText('Delete');
        await user.click(deleteButtons[0]);

        expect(screen.getByText('Confirm Deletion')).toBeInTheDocument();

        const confirmBtn = screen.getByRole('button', { name: 'Delete' }); // The red button in dialog
        await user.click(confirmBtn);

        await waitFor(() => {
            expect(advertisementApi.deleteAdvertisement).toHaveBeenCalledWith('1');
        });
    });

    it('toggles ad status', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        (advertisementApi.updateAdvertisementStatus as any).mockResolvedValue({ code: 200 });
        const user = userEvent.setup();
        render(<AdManagement />);

        await waitFor(() => expect(screen.getByText('Ad 1')).toBeInTheDocument());

        const toggleBtn = screen.getByText('Deactivate'); // Ad 1 is Active
        await user.click(toggleBtn);

        await waitFor(() => {
            expect(advertisementApi.updateAdvertisementStatus).toHaveBeenCalledWith('1', 'Inactive');
        });
    });


    it('handles load error', async () => {
        (advertisementApi.getAllAdvertisements as any).mockRejectedValue(new Error('API Error'));
        render(<AdManagement />);

        await waitFor(() => {
            expect(screen.getByText('API Error')).toBeInTheDocument();
        });
    });

    it('opens edit dialog and updates ad', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        (advertisementApi.updateAdvertisement as any).mockResolvedValue({ code: 200 });
        const user = userEvent.setup();
        render(<AdManagement />);

        await waitFor(() => expect(screen.getByText('Ad 1')).toBeInTheDocument());

        const editButtons = screen.getAllByRole('button', { name: /Edit/i });
        await user.click(editButtons[0]);

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Edit Advertisement')).toBeInTheDocument();

        // Check if values are pre-filled
        const nameInput = within(dialog).getByDisplayValue('Ad 1');
        expect(nameInput).toBeInTheDocument();

        // Check date formatting (should be YYYY-MM-DD due to split('T')[0])
        // ad1.startDate is '2023-01-01T00:00:00.000Z' -> '2023-01-01'
        const startDateInput = within(dialog).getByDisplayValue('2023-01-01');
        expect(startDateInput).toBeInTheDocument();

        await user.type(nameInput, ' Updated');

        const saveBtn = within(dialog).getByRole('button', { name: 'Save Changes' });
        await user.click(saveBtn);

        await waitFor(() => {
            expect(advertisementApi.updateAdvertisement).toHaveBeenCalledWith('1', expect.objectContaining({
                name: 'Ad 1 Updated'
            }));
        });
    });

    it('handles update error', async () => {
        (advertisementApi.getAllAdvertisements as any).mockResolvedValue(mockPageData);
        (advertisementApi.updateAdvertisement as any).mockRejectedValue(new Error('Update Failed'));
        const user = userEvent.setup();
        render(<AdManagement />);

        await waitFor(() => expect(screen.getByText('Ad 1')).toBeInTheDocument());

        const editButtons = screen.getAllByRole('button', { name: /Edit/i });
        await user.click(editButtons[0]);

        const dialog = await screen.findByRole('dialog');
        const saveBtn = within(dialog).getByRole('button', { name: 'Save Changes' });
        await user.click(saveBtn);

        await waitFor(() => {
            expect(screen.getByText('Update Failed')).toBeInTheDocument();
        });
    });
});


