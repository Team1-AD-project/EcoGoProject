import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import { ChatManagement } from './ChatManagement';

describe('ChatManagement Component', () => {

    beforeEach(() => {
        vi.restoreAllMocks();
        vi.spyOn(window, 'alert').mockImplementation(() => { });
        vi.spyOn(window, 'confirm').mockImplementation(() => true);
    });

    it('renders models list', () => {
        render(<ChatManagement />);
        expect(screen.getByText('Llama 2')).toBeInTheDocument();
        expect(screen.getByText('Mistral 7B')).toBeInTheDocument();
        // Check connected status
        expect(screen.getByText('Connected')).toBeInTheDocument();
    });

    it('adds a new model', async () => {
        const user = userEvent.setup();
        render(<ChatManagement />);

        await user.click(screen.getByText('Add New Model'));

        expect(screen.getByText('Add New AI Model')).toBeInTheDocument();

        // Fill form
        await user.type(screen.getByPlaceholderText('e.g., GPT-4, Llama 2, Mistral 7B'), 'My Custom Model');
        await user.type(screen.getByPlaceholderText('e.g., gpt-4, llama2, mistral'), 'custom-model');
        await user.type(screen.getByPlaceholderText('Brief description of this model\'s capabilities and use cases'), 'A custom model description');

        // Click Add Model button (footer)
        // There are 2 "Add Model" texts? Title and Button.
        // Button has icon Plus.
        const addBtn = screen.getByRole('button', { name: /Add Model/i });
        await user.click(addBtn);

        expect(screen.getByText('My Custom Model')).toBeInTheDocument();
    });

    it('edits a model', async () => {
        const user = userEvent.setup();
        render(<ChatManagement />);

        // Use the first "Edit" button on a model card
        const editButtons = screen.getAllByRole('button', { name: 'Edit Model' });
        await user.click(editButtons[0]);

        expect(screen.getByText('Edit Model Configuration')).toBeInTheDocument();

        const nameInput = screen.getByDisplayValue('Llama 2');
        await user.clear(nameInput);
        await user.type(nameInput, 'Llama 2 Updated');

        await user.click(screen.getByText('Save Changes'));

        expect(screen.getByText('Llama 2 Updated')).toBeInTheDocument();
    });

    it('tests a model', async () => {
        const user = userEvent.setup();
        render(<ChatManagement />);

        const testBtns = screen.getAllByRole('button', { name: 'Test Model' });
        await user.click(testBtns[0]);

        expect(screen.getByText('Test AI Model')).toBeInTheDocument();

        await user.type(screen.getByPlaceholderText('Enter your test prompt here...'), 'Hello');
        await user.click(screen.getByRole('button', { name: 'Run Test' }));

        expect(screen.getByText(/This is a simulated response/i)).toBeInTheDocument();
    });

    it('deletes a model', async () => {
        const user = userEvent.setup();
        globalThis.confirm = vi.fn(() => true);
        render(<ChatManagement />);

        const deleteBtns = screen.getAllByRole('button', { name: 'Delete Model' });
        await user.click(deleteBtns[0]);

        expect(globalThis.confirm).toHaveBeenCalled();
        expect(screen.queryByText('Llama 2')).not.toBeInTheDocument();
    });
});
