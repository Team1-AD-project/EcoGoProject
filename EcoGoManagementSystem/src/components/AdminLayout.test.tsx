import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import '@testing-library/jest-dom';

import { AdminLayout } from './AdminLayout';

vi.mock('@/components/Header', () => ({
  Header: () => <div>Mock Header</div>,
}));

vi.mock('@/components/Sidebar', () => ({
  Sidebar: ({
    selectedModule,
    onModuleSelect,
    onLogout,
  }: {
    selectedModule: string;
    onModuleSelect: (module: string) => void;
    onLogout: () => void;
  }) => (
    <div>
      <div>Mock Sidebar - {selectedModule}</div>
      <button onClick={() => onModuleSelect('user-management')}>Go User Management</button>
      <button onClick={onLogout}>Logout</button>
    </div>
  ),
}));

vi.mock('@/components/modules/UserManagement', () => ({
  UserManagement: () => <div>User Management Module</div>,
}));

vi.mock('@/components/modules/TripDataManagement', () => ({
  TripDataManagement: () => <div>Trip Data Management Module</div>,
}));

vi.mock('@/components/modules/PointsTransactionManagement', () => ({
  PointsTransactionManagement: () => <div>Points Transaction Management Module</div>,
}));

vi.mock('@/components/modules/VIPManagement', () => ({
  VIPManagement: () => <div>VIP Management Module</div>,
}));

vi.mock('@/components/modules/RewardStoreManagement', () => ({
  RewardStoreManagement: () => <div>Reward Store Management Module</div>,
}));

vi.mock('@/components/modules/CollectiblesManagement', () => ({
  CollectiblesManagement: () => <div>Collectibles Management Module</div>,
}));

vi.mock('@/components/modules/AnalyticsManagement', () => ({
  AnalyticsManagement: () => <div>Analytics Management Module</div>,
}));

vi.mock('@/components/modules/AdManagement', () => ({
  AdManagement: () => <div>Ad Management Module</div>,
}));

vi.mock('@/components/modules/LeaderboardManagement', () => ({
  LeaderboardManagement: () => <div>Leaderboard Management Module</div>,
}));

vi.mock('@/components/modules/ChatManagement', () => ({
  ChatManagement: () => <div>Chat Management Module</div>,
}));

vi.mock('@/components/modules/ActivityManagement', () => ({
  ActivityManagement: () => <div>Activity Management Module</div>,
}));

vi.mock('@/components/modules/ChallengeManagement', () => ({
  ChallengeManagement: () => <div>Challenge Management Module</div>,
}));

describe('AdminLayout', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  const renderWithRoute = (initialEntry: string = '/admin/dashboard') => {
    return render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/admin/:module" element={<AdminLayout />} />
          <Route path="/admin" element={<div>Admin Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );
  };

  it('renders Analytics (dashboard) module by default when module is dashboard', () => {
    renderWithRoute('/admin/dashboard');

    expect(screen.getByText('Mock Header')).toBeInTheDocument();
    expect(screen.getByText('Mock Sidebar - dashboard')).toBeInTheDocument();
    expect(screen.getByText('Analytics Management Module')).toBeInTheDocument();
  });

  it('renders correct module based on route param', () => {
    renderWithRoute('/admin/user-management');

    expect(screen.getByText('Mock Sidebar - user-management')).toBeInTheDocument();
    expect(screen.getByText('User Management Module')).toBeInTheDocument();
  });

  it('falls back to Analytics module when route param is unknown', () => {
    renderWithRoute('/admin/unknown-module');

    expect(screen.getByText('Analytics Management Module')).toBeInTheDocument();
  });

  it('navigates to selected module when Sidebar triggers onModuleSelect', async () => {
    const user = userEvent.setup();
    renderWithRoute('/admin/dashboard');

    expect(screen.getByText('Analytics Management Module')).toBeInTheDocument();

    const goUserButton = screen.getByRole('button', { name: 'Go User Management' });
    await user.click(goUserButton);

    await waitFor(() => {
      expect(screen.getByText('User Management Module')).toBeInTheDocument();
      expect(screen.getByText('Mock Sidebar - user-management')).toBeInTheDocument();
    });
  });

  it('clears admin data and navigates back to /admin on logout', async () => {
    const user = userEvent.setup();
    localStorage.setItem('adminToken', 'token');
    localStorage.setItem('adminInfo', '{"name":"Admin"}');

    renderWithRoute('/admin/dashboard');

    const logoutButton = screen.getByRole('button', { name: 'Logout' });
    await user.click(logoutButton);

    await waitFor(() => {
      expect(screen.getByText('Admin Login Page')).toBeInTheDocument();
    });

    expect(localStorage.getItem('adminToken')).toBeNull();
    expect(localStorage.getItem('adminInfo')).toBeNull();
  });
});

