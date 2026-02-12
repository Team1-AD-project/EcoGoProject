import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import '@testing-library/jest-dom';
import { BadgeManagement } from './BadgeManagement';

describe('BadgeManagement Component', () => {
    beforeEach(() => {
        // Mock Pointer Capture methods for Radix Select to avoid jsdom errors
        window.HTMLElement.prototype.hasPointerCapture = vi.fn(() => false);
        window.HTMLElement.prototype.setPointerCapture = vi.fn();
        window.HTMLElement.prototype.releasePointerCapture = vi.fn();
        window.HTMLElement.prototype.scrollIntoView = vi.fn();
    });

    it('renders badges list', () => {
        render(<BadgeManagement />);
        expect(screen.getByText('环保新人')).toBeInTheDocument();
    });

    it('filters badges by method', async () => {
        const user = userEvent.setup();
        render(<BadgeManagement />);

        // Initially shows all
        expect(screen.getByText('环保新人')).toBeInTheDocument(); // free

        // Filter by 'purchase' (积分购买)
        // The select trigger text is "全部方式" (All methods) by default.
        // We need to find the select.
        // There are 2 selects: Method and Rarity.
        // Method label is "获取方式".
        // We can use getByLabelText or just look for the trigger via surrounding text.

        // Using simple approach: Find the select by role combobox associated with label "获取方式" would be ideal but ShadCN Select doesn't always map label correctly for testing-library.
        // Since there are multiple selects, we can access them by index or text.

        // Let's try finding the trigger that says "全部方式"
        const triggers = screen.getAllByRole('combobox');
        const methodTrigger = triggers[0]; // First one is method

        await user.click(methodTrigger);

        // Option: 积分购买 - scope to select content to avoid matching badges
        const content = document.querySelector('[data-slot="select-content"]') as HTMLElement | null;
        expect(content).toBeTruthy();
        await user.click(within(content as HTMLElement).getByText('积分购买'));

        // "环保新人" is free, should disappear. "星耀徽章" is purchase, should remain.
        expect(screen.queryByText('环保新人')).not.toBeInTheDocument();
        expect(screen.getByText('星耀徽章')).toBeInTheDocument();
    });

    it('edits a badge', async () => {
        const user = userEvent.setup();
        render(<BadgeManagement />);

        const editButtons = screen.getAllByText('编辑设置');
        await user.click(editButtons[0]); // Edit first badge (环保新人)

        expect(screen.getByText('编辑徽章设置')).toBeInTheDocument();

        // Change name
        const nameInput = screen.getByDisplayValue('环保新人');
        await user.clear(nameInput);
        await user.type(nameInput, 'New Badge Name');

        // Save
        await user.click(screen.getByText('保存修改'));

        // Verify
        expect(screen.getByText('New Badge Name')).toBeInTheDocument();
    });
});
