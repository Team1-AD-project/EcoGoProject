import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';

import { ProtectedRoute } from './ProtectedRoute';

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  const renderWithRoutes = () => {
    return render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/admin/dashboard" element={<div>Protected Dashboard</div>} />
          </Route>
          <Route path="/admin" element={<div>Admin Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );
  };

  it('redirects to /admin when no adminToken is present', () => {
    renderWithRoutes();

    expect(screen.getByText('Admin Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Dashboard')).not.toBeInTheDocument();
  });

  it('renders protected content when adminToken exists', () => {
    localStorage.setItem('adminToken', 'test-token');

    renderWithRoutes();

    expect(screen.getByText('Protected Dashboard')).toBeInTheDocument();
    expect(screen.queryByText('Admin Login Page')).not.toBeInTheDocument();
  });
});

