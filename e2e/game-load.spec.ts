import { test, expect } from '@playwright/test';

// Basic smoke test that the Indigo game boots and renders a canvas.
test('game loads and renders canvas', async ({ page }) => {
  await page.goto('/');

  // Container should exist.
  const container = page.locator('#indigo-container');
  await expect(container).toBeVisible();

  // Wait for a canvas element to be attached inside the container.
  const canvas = container.locator('canvas');
  await expect(canvas).toHaveCount(1, { timeout: 15000 });
  await expect(canvas).toBeVisible();

  // Validate canvas has a non-zero size after rendering.
  const bbox = await canvas.boundingBox();
  expect(bbox).not.toBeNull();
  expect(bbox!.width).toBeGreaterThan(10);
  expect(bbox!.height).toBeGreaterThan(10);

  // Optionally check that the window title was set.
  await expect(page).toHaveTitle(/Indigo/i);

  // Take a screenshot artifact for debugging.
  await page.screenshot({ path: 'game-load.png', fullPage: false });
});

