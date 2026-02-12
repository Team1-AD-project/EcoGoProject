import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import '@testing-library/jest-dom';

import { LoginPage } from './LoginPage';
import * as authService from '@/services/auth';
import { toast } from 'sonner';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/services/auth', () => ({
  loginAdmin: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('LoginPage', () => {
  const loginAdminMock = authService.loginAdmin as unknown as ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockReset();
    localStorage.clear();
  });

  it('renders login form fields', () => {
    render(<LoginPage />);

    expect(screen.getByLabelText(/user id/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByText(/demo credentials/i)).toBeInTheDocument();
  });

  it('successfully logs in and navigates to dashboard', async () => {
    (loginAdminMock as any).mockResolvedValue({
      code: 200,
      data: {
        token: 'test-token',
        user_info: {
          nickname: 'Test Admin',
        },
      },
      message: 'Success',
    });

    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/user id/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'admin123');

    const submitButton = screen.getByRole('button', { name: /sign in/i });
    await user.click(submitButton);

    expect(loginAdminMock).toHaveBeenCalledWith({
      userid: 'admin',
      password: 'admin123',
    });

    await waitFor(() => {
      expect(localStorage.getItem('adminToken')).toBe('test-token');
      expect(localStorage.getItem('adminInfo')).toContain('Test Admin');
      expect(mockNavigate).toHaveBeenCalledWith('/admin/dashboard');
      expect(toast.success).toHaveBeenCalledWith(
        expect.stringContaining('Test Admin'),
      );
    });

    // Button should be re-enabled after request completes
    expect(submitButton).not.toBeDisabled();
  });

  it('shows error toast when login fails with non-200 code', async () => {
    (loginAdminMock as any).mockResolvedValue({
      code: 400,
      message: 'Invalid credentials',
    });

    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/user id/i), 'wrong');
    await user.type(screen.getByLabelText(/password/i), 'wrong');

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Invalid credentials');
    });

    expect(localStorage.getItem('adminToken')).toBeNull();
    expect(mockNavigate).not.toHaveBeenCalledWith('/admin/dashboard');
  });

  it('shows error toast when loginAdmin throws', async () => {
    (loginAdminMock as any).mockRejectedValue(new Error('Network error'));

    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/user id/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'admin123');

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Network error');
    });

    expect(localStorage.getItem('adminToken')).toBeNull();
  });

  it('navigates back to home when clicking "Back to Home"', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    const backButton = screen.getByRole('button', { name: /back to home/i });
    await user.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('disables submit button while loading', async () => {
    // Make login promise that resolves later so we can inspect loading state
    let resolvePromise: (value: unknown) => void;
    const loginPromise = new Promise((resolve) => {
      resolvePromise = resolve;
    });
    (loginAdminMock as any).mockReturnValue(loginPromise);

    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/user id/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'admin123');

    const submitButton = screen.getByRole('button', { name: /sign in/i });
    await user.click(submitButton);

    expect(submitButton).toBeDisabled();

    // Finish the request
    resolvePromise!({
      code: 200,
      data: { token: 't', user_info: {} },
    });

    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
    });
  });
});

