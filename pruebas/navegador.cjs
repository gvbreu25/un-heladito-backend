const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const { mkdir } = require('node:fs/promises');

const web = 'http://localhost:5173';
const api = 'http://localhost:8080';
const target = process.env.QA_CAPTURE_DIR;
const password = process.env.QA_CLIENT_PASSWORD;

async function capture(page, name) {
  await page.screenshot({ path: target + '/' + name + '.png', animations: 'disabled', timeout: 15000 });
}

async function login(page, address, pass) {
  await page.goto(web + '/login');
  await page.locator('#email').fill(address);
  await page.locator('#password').fill(pass);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  await page.waitForURL(web + '/gestion');
}

async function run(browser) {
  const shopper = await browser.newContext({ viewport: { width: 1280, height: 850 }, locale: 'es-CL' });
  const page = await shopper.newPage();
  await page.goto(web + '/productos');
  await page.getByRole('link', { name: 'Ver Chocolate Belga 70%', exact: true }).waitFor();
  assert.equal(await page.getByRole('button', { name: 'Agregar +' }).count(), 9);
  await capture(page, '01-catalogo');

  await page.goto(web + '/registro');
  await page.getByRole('heading', { name: 'Crear cuenta' }).waitFor();
  await capture(page, '02-registro');
  await page.locator('#name').fill('Cliente de prueba');
  await page.locator('#email').fill('cliente.qa@unheladito.test');
  await page.locator('#password').fill(password);
  await page.locator('#confirm').fill(password);
  await page.getByRole('button', { name: 'Crear mi cuenta' }).click();
  await page.waitForURL(web + '/mi-cuenta');
  await page.reload();
  await page.getByRole('heading', { name: /Hola, Cliente/ }).waitFor();
  await page.goto(web + '/admin/usuarios');
  await page.waitForURL(web + '/');

  await page.goto(web + '/productos/HT001');
  await page.getByRole('heading', { name: 'Chocolate Belga 70%' }).waitFor();
  await capture(page, '03-detalle');
  await page.getByRole('button', { name: 'Agregar al carrito' }).click();
  await page.getByRole('link', { name: /Carrito 1/ }).click();
  await page.getByRole('button', { name: /Continuar compra/ }).click();
  await page.waitForURL(web + '/checkout');
  await page.locator('#street').fill('Calle de prueba 100');
  await page.locator('#region').selectOption('Región Metropolitana');
  await page.locator('#commune').fill('Melipilla');
  await page.getByRole('button', { name: 'Confirmar pedido' }).click();
  await page.getByRole('heading', { name: 'Pedido registrado' }).waitFor();
  await capture(page, '04-pedido');

  const sales = await browser.newContext({ viewport: { width: 1280, height: 850 }, locale: 'es-CL' });
  const seller = await sales.newPage();
  await login(seller, 'ventas@unheladito.test', process.env.DEMO_VENDOR_PASSWORD);
  await seller.getByRole('heading', { name: 'Consulta de ventas' }).waitFor();
  await seller.getByRole('link', { name: 'Ver detalle' }).first().waitFor();
  assert.equal(await seller.getByRole('button', { name: 'Nuevo producto' }).count(), 0);
  await capture(seller, '05-vendedor');
  await seller.getByRole('navigation', { name: 'Panel de gestión' }).getByRole('link', { name: 'Pedidos' }).click();
  await seller.getByRole('link', { name: /Ver pedido #/ }).first().click();
  await seller.getByRole('heading', { name: /Pedido #/ }).waitFor();
  await capture(seller, '06-pedido-vendedor');
  await seller.getByRole('button', { name: 'Salir' }).click();
  await seller.waitForURL(web + '/login');

  const admin = await browser.newContext({ viewport: { width: 1280, height: 850 }, locale: 'es-CL' });
  const manager = await admin.newPage();
  await login(manager, 'admin@unheladito.test', process.env.DEMO_ADMIN_PASSWORD);
  await manager.getByRole('button', { name: 'Nuevo producto' }).click();
  await manager.getByRole('heading', { name: 'Crear producto' }).waitFor();
  await capture(manager, '07-administrador');

  const swagger = await admin.newPage();
  await swagger.goto(api + '/swagger-ui.html');
  await swagger.getByRole('heading', { name: /Un Heladito API/ }).waitFor();
  await swagger.getByText('Consultar un producto por código', { exact: true }).waitFor();
  await capture(swagger, '08-swagger');
  console.log('PASS: catálogo, registro, sesión tras recarga, restricción de cliente, pedido, vendedor, administrador y Swagger');
}

(async () => {
  await mkdir(target, { recursive: true });
  const browser = await chromium.launch();
  try { await run(browser); } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
