import { expect, test } from '@playwright/test';

test('a team member can ingest a metric, trigger an alert, and analyze an incident', async ({ page }) => {
  const email = process.env.DEMO_EMAIL ?? 'demo@devpulse.local';
  const password = process.env.DEMO_PASSWORD ?? 'DemoPass123!';
  const suffix = `${Date.now()}`;
  const metricName = `e2e_cpu_${suffix}`;
  const ruleName = `E2E high CPU ${suffix}`;
  const incidentTitle = `E2E incident ${suffix}`;
  const pageErrors: string[] = [];

  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') pageErrors.push(message.text());
  });

  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  const loginResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/auth/login',
  );
  await page.getByRole('button', { name: 'Login' }).click();
  const loginResponse = await loginResponsePromise;
  expect(loginResponse.status()).toBe(200);

  await expect(page).toHaveURL(/\/dashboard$/, { message: `Browser errors: ${pageErrors.join(' | ')}` });
  await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
  await expect(page.locator('strong').filter({ hasText: 'DevPulse Demo' })).toBeVisible();

  const session = await page.evaluate(() => {
    const raw = localStorage.getItem('devpulse.session');
    const selectedProject = Number(localStorage.getItem('devpulse.selectedProject'));
    return {
      token: raw ? (JSON.parse(raw) as { token?: string }).token : undefined,
      projectId: selectedProject,
    };
  });
  expect(session.token).toBeTruthy();
  expect(session.projectId).toBeGreaterThan(0);

  await page.goto('/alerts');
  await expect(page.getByRole('heading', { name: 'Alerts', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'New rule' }).click();
  await page.getByLabel('Name').fill(ruleName);
  await page.getByLabel('Metric').fill(metricName);
  await page.getByLabel('Threshold').fill('80');
  await page.getByLabel('Severity').selectOption('HIGH');
  await page.getByRole('button', { name: 'Create rule' }).click();
  await expect(page.getByText(ruleName, { exact: true })).toBeVisible();

  await page.goto('/metrics');
  await expect(page.getByRole('heading', { name: 'Metrics', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ingest metric' }).click();
  await page.getByLabel('Metric name').fill(metricName);
  await page.getByLabel('Value', { exact: true }).fill('95');
  await page.getByLabel('Service (optional)').fill('checkout-api');
  await page.getByLabel('Unit (optional)').fill('%');
  await page.getByRole('button', { name: 'Ingest', exact: true }).click();
  await expect(page.getByRole('row').filter({ hasText: metricName })).toBeVisible();

  await page.goto('/alerts');
  await expect(page.getByText(ruleName, { exact: true })).toBeVisible();
  const triggeredAlertRow = page.getByRole('row').filter({ hasText: ruleName }).filter({ hasText: 'current value: 95' });
  await expect(triggeredAlertRow).toContainText('OPEN');

  await page.goto('/incidents');
  await expect(page.getByRole('heading', { name: 'Incidents', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Report incident' }).click();
  await page.getByLabel('Title').fill(incidentTitle);
  await page.getByLabel('Description').fill('Checkout requests are failing because the database connection pool is exhausted.');
  await page.getByRole('button', { name: 'Create incident' }).click();

  const incidentRow = page.getByRole('row').filter({ hasText: incidentTitle });
  await expect(incidentRow).toBeVisible();
  const analysisResponse = page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/incidents\/\d+\/analyze$/.test(new URL(response.url()).pathname),
  );
  await incidentRow.getByRole('button', { name: 'Analyze' }).click();
  const response = await analysisResponse;
  if (!response.ok()) {
    throw new Error(`Incident analysis failed with HTTP ${response.status()}: ${await response.text()}`);
  }
  const analyzedIncident = await response.json() as { rootCause?: string; recommendations?: string };
  expect(analyzedIncident.rootCause?.trim()).toBeTruthy();
  await expect(incidentRow).toContainText(analyzedIncident.rootCause!);
});
