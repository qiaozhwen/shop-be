#!/usr/bin/env node

import process from 'node:process';
import { randomBytes } from 'node:crypto';

const baseUrl = process.env.ACCEPTANCE_BASE_URL ?? 'http://127.0.0.1:8080';
const phone = process.env.ACCEPTANCE_ADMIN_PHONE ?? '13900000000';
const password = process.env.ACCEPTANCE_ADMIN_PASSWORD ?? 'AcceptancePass1';
const mode = process.argv[2] ?? 'full';
const marker = process.env.ACCEPTANCE_MARKER ?? `ACCEPT-${Date.now()}`;
let cleanupToken;
let temporaryStaffId;
let temporaryStaffRefreshToken;

function check(condition, message) {
  if (!condition) throw new Error(message);
}

async function request(path, { method = 'GET', token, body, expectedStatus = 200 } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'content-type': 'application/json' }),
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      throw new Error(`${method} ${path} returned non-JSON: ${text.slice(0, 200)}`);
    }
  }
  check(response.status === expectedStatus,
    `${method} ${path} expected HTTP ${expectedStatus}, got ${response.status}: ${text}`);
  return payload;
}

async function api(path, options = {}) {
  const payload = await request(path, options);
  check(payload?.code === 200, `${options.method ?? 'GET'} ${path} failed: ${JSON.stringify(payload)}`);
  return payload.data;
}

async function login() {
  const result = await request('/api/admin/auth/login', {
    method: 'POST',
    body: { phone, password, deviceInfo: 'customer-delivery-acceptance' },
  });
  check(result?.accessToken, 'login did not return an access token');
  check(result?.refreshToken, 'login did not return a refresh token');
  return result;
}

async function assertAllLists(token) {
  const paths = [
    '/api/stores?pageSize=200',
    '/api/poultry-categories?pageSize=200',
    '/api/inventory?pageSize=200',
    '/api/sales-orders?pageSize=200',
    '/api/processing-tasks?pageSize=200',
    '/api/suppliers?pageSize=200',
    '/api/purchases?pageSize=200',
    '/api/losses?pageSize=200',
    '/api/members?pageSize=200',
    '/api/staff?pageSize=200',
    '/api/pricing?pageSize=200',
  ];
  for (const path of paths) {
    const data = await api(path, { token });
    check(Array.isArray(data?.list), `${path} did not return a page list`);
    check(Number.isInteger(data?.total), `${path} did not return a page total`);
  }
  const dashboard = await api('/api/dashboard/summary', { token });
  check(Array.isArray(dashboard.salesTrend) && dashboard.salesTrend.length === 7,
    'dashboard did not return a seven-day trend');
}

async function verifyMarker(token, reportRestart = false) {
  const categories = await api(`/api/poultry-categories?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const members = await api(`/api/members?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const orders = await api(`/api/sales-orders?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const stores = await api(`/api/stores?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const inventory = await api(`/api/inventory?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const suppliers = await api(`/api/suppliers?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const purchases = await api(`/api/purchases?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const losses = await api(`/api/losses?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const tasks = await api(`/api/processing-tasks?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  const prices = await api(`/api/pricing?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  check(categories.total === 1, `category marker ${marker} was not persisted`);
  check(members.total === 1, `member marker ${marker} was not persisted`);
  check(orders.total === 1, `sales marker ${marker} was not persisted`);
  check(stores.total === 1, `store marker ${marker} was not persisted`);
  check(inventory.total >= 2, `inventory markers ${marker} were not persisted`);
  check(suppliers.total === 1, `supplier marker ${marker} was not persisted`);
  check(purchases.total === 1 && purchases.list[0].status === 'RECEIVED', `received purchase ${marker} was not persisted`);
  check(losses.total === 1, `loss marker ${marker} was not persisted`);
  check(tasks.total === 1 && tasks.list[0].workerName === marker, `processing marker ${marker} was not persisted`);
  check(prices.total === 1 && prices.list[0].price === 31.5, `pricing marker ${marker} was not persisted`);
  if (reportRestart) console.log(`RESTART_PERSISTENCE_OK marker=${marker}`);
}

async function fullAcceptance(token, refreshToken) {
  await assertAllLists(token);

  const suffix = String(Date.now()).slice(-8);
  const store = await request('/api/stores', {
    method: 'POST',
    token,
    expectedStatus: 201,
    body: {
      code: `S-${marker}`,
      name: `验收门店-${marker}`,
      address: '客户交付验收地址',
      phone: `138${suffix}`,
      ownerName: '验收负责人',
      status: 'OPEN',
      openTime: '08:00:00',
      closeTime: '20:00:00',
      remark: marker,
    },
  });
  check(store?.id, 'store creation did not return an id');
  const updatedStore = await request(`/api/stores/${store.id}`, {
    method: 'PUT',
    token,
    body: { ...store, remark: `${marker}-UPDATED` },
  });
  check(updatedStore?.remark === `${marker}-UPDATED`, 'store update was not applied');

  const category = await api('/api/poultry-categories', {
    method: 'POST', token,
    body: {
      code: marker,
      name: marker,
      species: '鸡',
      unit: 'PIECE',
      basePrice: 30,
      processingFee: 5,
      avgWeight: 2,
      enabled: true,
    },
  });
  const inventory = await api('/api/inventory', {
    method: 'POST', token,
    body: {
      storeId: 1,
      storeName: '总店·城北店',
      categoryId: category.id,
      categoryName: marker,
      batchNo: `${marker}-SALE`,
      quantity: 10,
      avgWeight: 2,
      health: 'HEALTHY',
    },
  });
  const adjusted = await api(`/api/inventory/${inventory.id}/adjust`, {
    method: 'POST', token, body: { delta: 1, reason: '交付验收盘点' },
  });
  check(adjusted.quantity === 11, 'inventory adjustment returned the wrong quantity');

  const supplier = await api('/api/suppliers', {
    method: 'POST', token,
    body: { name: marker, contact: '验收联系人', phone: `137${suffix}`, category: '鸡' },
  });
  check(supplier.level === 'C' && supplier.enabled === true,
    'supplier defaults were not normalized for frontend rendering');
  const purchase = await api('/api/purchases', {
    method: 'POST', token,
    body: {
      supplierId: supplier.id,
      supplierName: marker,
      storeId: 1,
      storeName: '总店·城北店',
      categoryName: marker,
      quantity: 4,
      totalWeight: 8,
      unitPrice: 20,
      batchNo: `${marker}-PO`,
    },
  });
  const received = await api(`/api/purchases/${purchase.id}/receive`, { method: 'POST', token });
  check(received.status === 'RECEIVED', 'purchase was not received');
  const purchasedStock = await api(`/api/inventory?keyword=${encodeURIComponent(`${marker}-PO`)}&pageSize=20`, { token });
  check(purchasedStock.total === 1, 'receiving a purchase did not create inventory');

  const loss = await api('/api/losses', {
    method: 'POST', token,
    body: {
      inventoryId: inventory.id,
      storeId: 1,
      storeName: '总店·城北店',
      categoryName: marker,
      batchNo: `${marker}-SALE`,
      quantity: 1,
      reason: 'OTHER',
      handler: marker,
    },
  });
  check(loss.id, 'loss creation did not return an id');

  const member = await api('/api/members', {
    method: 'POST', token,
    body: { name: marker, phone: `136${suffix}`, level: 'REGULAR', remark: marker },
  });
  const updatedMember = await api(`/api/members/${member.id}`, {
    method: 'PUT', token, body: { level: 'PLATINUM' },
  });
  check(updatedMember.level === 'PLATINUM', 'member update was not applied');

  const staffPhone = `135${suffix}`;
  const staffPassword = `A-${randomBytes(18).toString('base64url')}`;
  const staff = await api('/api/staff', {
    method: 'POST', token,
    body: {
      name: marker,
      phone: staffPhone,
      role: 'CASHIER',
      storeId: 1,
      password: staffPassword,
      hireDate: '2026-07-14',
      remark: marker,
      enabled: true,
    },
  });
  const staffLogin = await request('/api/admin/auth/login', {
    method: 'POST',
    body: { phone: staffPhone, password: staffPassword, deviceInfo: 'staff-module-acceptance' },
  });
  check(staffLogin?.accessToken, 'staff created through management could not log in');
  temporaryStaffId = staff.id;
  temporaryStaffRefreshToken = staffLogin.refreshToken;
  const updatedStaff = await api(`/api/staff/${staff.id}`, {
    method: 'PUT', token, body: { role: 'BUTCHER', remark: `${marker}-UPDATED` },
  });
  check(updatedStaff.role === 'BUTCHER', 'staff update was not applied');

  const pricingPage = await api(`/api/pricing?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  check(pricingPage.list.length === 1, 'new category did not create an editable pricing record');
  const pricing = await api(`/api/pricing/${pricingPage.list[0].id}`, {
    method: 'PUT', token, body: { price: 31.5, processingFee: 5.5 },
  });
  check(pricing.price === 31.5, 'pricing update was not applied');

  const order = await api('/api/sales-orders', {
    method: 'POST', token,
    body: {
      storeId: 1,
      customerPhone: marker,
      memberId: member.id,
      payMethod: 'CASH',
      remark: marker,
      items: [{
        categoryId: category.id,
        categoryName: marker,
        quantity: 2,
        weight: 4,
        unitPrice: 30,
        processMethod: 'EVISCERATE',
        processFee: 5,
        subtotal: 130,
      }],
    },
  });
  check(order.status === 'PAID', 'sales order was not paid');
  check(order.totalAmount === 74, 'sales order did not use the authoritative server price');
  const completedOrder = await api(`/api/sales-orders/${order.id}/status`, {
    method: 'PUT', token, body: { status: 'COMPLETED' },
  });
  check(completedOrder.status === 'COMPLETED', 'sales status update was not applied');

  const tasks = await api(`/api/processing-tasks?keyword=${encodeURIComponent(marker)}&pageSize=20`, { token });
  check(tasks.total === 1, 'processed sale did not create a processing task');
  const task = tasks.list[0];
  const assigned = await api(`/api/processing-tasks/${task.id}/assign`, {
    method: 'POST', token, body: { workerId: staff.id, workerName: marker },
  });
  check(assigned.workerId === staff.id, 'processing assignment was not applied');
  const advanced = await api(`/api/processing-tasks/${task.id}/advance`, { method: 'POST', token });
  check(advanced.status === 'SLAUGHTERING', 'processing task did not advance');

  const refreshed = await request('/api/auth/refresh', {
    method: 'POST', body: { refreshToken },
  });
  check(refreshed?.accessToken && refreshed?.refreshToken, 'refresh token rotation failed');
  const me = await request('/api/admin/me', { token: refreshed.accessToken });
  check(me?.phone === phone, 'refreshed access token did not resolve the administrator');

  await assertAllLists(refreshed.accessToken);
  await verifyMarker(refreshed.accessToken);
  console.log(`HTTP_ACCEPTANCE_OK marker=${marker}`);
}

async function main() {
  const health = await request('/health');
  check(health?.status === 'UP', 'health endpoint did not report UP');
  await request('/api/stores', { expectedStatus: 401 });
  const session = await login();
  cleanupToken = session.accessToken;
  if (mode === 'verify-marker') {
    await assertAllLists(session.accessToken);
    await verifyMarker(session.accessToken, true);
    return;
  }
  check(mode === 'full', `unknown mode: ${mode}`);
  await fullAcceptance(session.accessToken, session.refreshToken);
}

async function cleanup() {
  if (temporaryStaffRefreshToken) {
    await request('/api/admin/auth/logout', {
      method: 'POST', body: { refreshToken: temporaryStaffRefreshToken },
    }).catch(() => undefined);
  }
  if (temporaryStaffId && cleanupToken) {
    await request(`/api/staff/${temporaryStaffId}`, {
      method: 'DELETE', token: cleanupToken,
    }).catch(() => undefined);
  }
}

main()
  .catch((error) => {
    console.error(`HTTP_ACCEPTANCE_FAILED: ${error.stack ?? error.message}`);
    process.exitCode = 1;
  })
  .finally(cleanup);
