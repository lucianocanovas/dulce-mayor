import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const outDir = path.resolve('docs/review_screenshots');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

async function run() {
  console.log('Starting Playwright visual inspection...');
  const browser = await chromium.launch({ headless: true });

  const styleReport = {
    landing: {},
    centralManagement: {},
    orderManagement: {},
    inconsistencies: [],
  };

  // Helper to extract styles
  async function extractKeyStyles(page) {
    return await page.evaluate(() => {
      const body = document.body;
      const primaryBtn = document.querySelector('.btn-primary, .btn, button');
      const heading = document.querySelector('h1, h2, h3');
      const getStyles = (el) => {
        if (!el) return null;
        const comp = window.getComputedStyle(el);
        return {
          fontFamily: comp.fontFamily,
          color: comp.color,
          backgroundColor: comp.backgroundColor,
          borderRadius: comp.borderRadius,
        };
      };

      return {
        body: getStyles(body),
        primaryBtn: getStyles(primaryBtn),
        heading: getStyles(heading),
      };
    });
  }

  // 1. LANDING (PORT 8081)
  console.log('Inspecting 1. Landing (http://localhost:8081)...');
  const pageLanding = await browser.newPage({ viewport: { width: 1280, height: 800 } });
  
  await pageLanding.goto('http://localhost:8081/');
  await pageLanding.waitForLoadState('networkidle');
  await pageLanding.screenshot({ path: path.join(outDir, '01_landing_home.png') });
  styleReport.landing.home = await extractKeyStyles(pageLanding);

  await pageLanding.goto('http://localhost:8081/catalog');
  await pageLanding.waitForLoadState('networkidle');
  await pageLanding.screenshot({ path: path.join(outDir, '02_landing_catalog.png') });
  styleReport.landing.catalog = await extractKeyStyles(pageLanding);

  await pageLanding.goto('http://localhost:8081/contact');
  await pageLanding.waitForLoadState('networkidle');
  await pageLanding.screenshot({ path: path.join(outDir, '03_landing_contact.png') });

  await pageLanding.close();

  // 2. CENTRAL MANAGEMENT (PORT 5173)
  console.log('Inspecting 2. Central Management (http://localhost:5173)...');
  const pageCentral = await browser.newPage({ viewport: { width: 1366, height: 850 } });
  await pageCentral.goto('http://localhost:5173/');
  await pageCentral.waitForLoadState('networkidle');
  await pageCentral.screenshot({ path: path.join(outDir, '04_central_login.png') });

  // Perform login
  try {
    const userInput = await pageCentral.$('input[type="text"], input[placeholder*="usuario"]');
    const passInput = await pageCentral.$('input[type="password"]');
    const submitBtn = await pageCentral.$('button[type="submit"]');

    if (userInput && passInput && submitBtn) {
      await userInput.fill('admin');
      await passInput.fill('dulcemayor');
      await submitBtn.click();
      await pageCentral.waitForTimeout(1000);
    }
  } catch (e) {
    console.warn('Login form bypass or already logged in:', e.message);
  }

  await pageCentral.screenshot({ path: path.join(outDir, '05_central_dashboard.png') });
  styleReport.centralManagement.dashboard = await extractKeyStyles(pageCentral);

  // Navigate to Products tab
  try {
    const navButtons = await pageCentral.$$('.sidebar-nav button, nav button, button');
    for (const btn of navButtons) {
      const text = await btn.innerText();
      if (text.includes('Productos') || text.includes('Inventario') || text.includes('Depósito')) {
        await btn.click();
        await pageCentral.waitForTimeout(600);
        break;
      }
    }
    await pageCentral.screenshot({ path: path.join(outDir, '06_central_products.png') });
    styleReport.centralManagement.products = await extractKeyStyles(pageCentral);

    // Open view details modal if available
    const viewBtn = await pageCentral.$('.action-icon-btn.view');
    if (viewBtn) {
      await viewBtn.click();
      await pageCentral.waitForTimeout(500);
      await pageCentral.screenshot({ path: path.join(outDir, '07_central_product_modal.png') });
      const closeBtn = await pageCentral.$('.modal-close-btn, .modal-actions button');
      if (closeBtn) await closeBtn.click();
      await pageCentral.waitForTimeout(300);
    }
  } catch (e) {
    console.warn('Products view nav issue:', e.message);
  }

  // Navigate to Orders tab
  try {
    const navButtons = await pageCentral.$$('.sidebar-nav button, nav button, button');
    for (const btn of navButtons) {
      const text = await btn.innerText();
      if (text.includes('Pedidos') || text.includes('Facturación')) {
        await btn.click();
        await pageCentral.waitForTimeout(600);
        break;
      }
    }
    await pageCentral.screenshot({ path: path.join(outDir, '08_central_orders.png') });

    // Open order details modal
    const viewOrderBtn = await pageCentral.$('.action-icon-btn.view');
    if (viewOrderBtn) {
      await viewOrderBtn.click();
      await pageCentral.waitForTimeout(500);
      await pageCentral.screenshot({ path: path.join(outDir, '09_central_order_modal.png') });
      const closeBtn = await pageCentral.$('.modal-close-btn');
      if (closeBtn) await closeBtn.click();
      await pageCentral.waitForTimeout(300);
    }
  } catch (e) {
    console.warn('Orders view nav issue:', e.message);
  }

  // Navigate to Promotions tab
  try {
    const navButtons = await pageCentral.$$('.sidebar-nav button, nav button, button');
    for (const btn of navButtons) {
      const text = await btn.innerText();
      if (text.includes('Promociones') || text.includes('Descuentos')) {
        await btn.click();
        await pageCentral.waitForTimeout(600);
        break;
      }
    }
    await pageCentral.screenshot({ path: path.join(outDir, '10_central_promotions.png') });
  } catch (e) {
    console.warn('Promotions view nav issue:', e.message);
  }

  // Navigate to Users tab
  try {
    const navButtons = await pageCentral.$$('.sidebar-nav button, nav button, button');
    for (const btn of navButtons) {
      const text = await btn.innerText();
      if (text.includes('Personal') || text.includes('Usuarios')) {
        await btn.click();
        await pageCentral.waitForTimeout(600);
        break;
      }
    }
    await pageCentral.screenshot({ path: path.join(outDir, '11_central_users.png') });
  } catch (e) {
    console.warn('Users view nav issue:', e.message);
  }

  await pageCentral.close();

  // 3. ORDER MANAGEMENT PWA (PORT 5174)
  console.log('Inspecting 3. Order Management (http://localhost:5174)...');
  // Mobile viewport for PWA
  const pageOrder = await browser.newPage({ viewport: { width: 390, height: 844 }, isMobile: true });
  await pageOrder.goto('http://localhost:5174/');
  await pageOrder.waitForLoadState('networkidle');
  await pageOrder.screenshot({ path: path.join(outDir, '12_order_login.png') });

  // Perform login
  try {
    const userInput = await pageOrder.$('input[type="text"]');
    const passInput = await pageOrder.$('input[type="password"]');
    const submitBtn = await pageOrder.$('button[type="submit"]');

    if (userInput && passInput && submitBtn) {
      await userInput.fill('preventor');
      await passInput.fill('dulcemayor');
      await submitBtn.click();
      await pageOrder.waitForTimeout(1000);
    }
  } catch (e) {
    console.warn('Order login form bypass:', e.message);
  }

  // 13: Routes View
  await pageOrder.screenshot({ path: path.join(outDir, '13_order_routes.png') });
  styleReport.orderManagement.routes = await extractKeyStyles(pageOrder);

  // Switch to Catalog / Pedidos tab
  try {
    const bottomNav = await pageOrder.$$('.bottom-nav-item, nav button, .tab-btn');
    for (const btn of bottomNav) {
      const text = await btn.innerText();
      if (text.includes('Catálogo') || text.includes('Pedido') || text.includes('Venta')) {
        await btn.click();
        await pageOrder.waitForTimeout(600);
        break;
      }
    }
    await pageOrder.screenshot({ path: path.join(outDir, '14_order_catalog.png') });
    styleReport.orderManagement.catalog = await extractKeyStyles(pageOrder);
  } catch (e) {
    console.warn('Order catalog nav issue:', e.message);
  }

  // Switch to Comercios / Retailers tab
  try {
    const bottomNav = await pageOrder.$$('.bottom-nav-item, nav button, .tab-btn');
    for (const btn of bottomNav) {
      const text = await btn.innerText();
      if (text.includes('Comercios') || text.includes('Clientes') || text.includes('Kioscos')) {
        await btn.click();
        await pageOrder.waitForTimeout(600);
        break;
      }
    }
    await pageOrder.screenshot({ path: path.join(outDir, '15_order_retailers.png') });
  } catch (e) {
    console.warn('Order retailers nav issue:', e.message);
  }

  await pageOrder.close();
  await browser.close();

  fs.writeFileSync(path.join(outDir, 'style_report.json'), JSON.stringify(styleReport, null, 2));
  console.log('Playwright visual inspection complete! Screenshots and style report saved.');
}

run().catch((err) => {
  console.error('Playwright inspection error:', err);
  process.exit(1);
});
