import '@testing-library/jest-dom';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Clear RTL elements after each test
afterEach(() => {
  cleanup();
});
