import { render, screen } from '@testing-library/react';
import { describe, test, expect } from 'vitest';
import App from './App';

describe('App Component Sanity Check', () => {
  test('renders AI Project Manager title text', () => {
    render(<App />);
    const titleElements = screen.getAllByText(/AI Project Manager/i);
    expect(titleElements.length).toBeGreaterThan(0);
  });
});
