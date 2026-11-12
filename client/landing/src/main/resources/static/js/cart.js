(function () {
    const CART_KEY = 'dulce_mayor_cart_v1';

    function safeJsonParse(raw, fallback) {
        try {
            return raw ? JSON.parse(raw) : fallback;
        } catch (e) {
            return fallback;
        }
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function formatMoney(value) {
        const num = Number(value || 0);
        return '$' + num.toLocaleString('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    }

    function formatUnitLabel(qty) {
        return Number(qty) === 1 ? '1 unidad' : `${Number(qty)} unidades`;
    }

    function getItems() {
        return safeJsonParse(localStorage.getItem(CART_KEY), []);
    }

    function saveItems(items) {
        localStorage.setItem(CART_KEY, JSON.stringify(items));
        renderCartBadge();
        renderDrawer();
        window.dispatchEvent(new CustomEvent('dulce-cart-updated', { detail: items }));
    }

    function clear() {
        localStorage.removeItem(CART_KEY);
        renderCartBadge();
        renderDrawer();
        window.dispatchEvent(new CustomEvent('dulce-cart-updated', { detail: [] }));
    }

    function addItem(product, qtyToAdd) {
        const qty = Math.max(1, Number(qtyToAdd || 1));
        const maxStock = Number(product.stock || 0);
        if (maxStock <= 0) {
            showToast('Este producto no tiene stock disponible.');
            return;
        }
        const items = getItems();
        const existing = items.find(i => i.id === product.id);
        if (existing) {
            const nextQty = existing.quantity + qty;
            if (nextQty > maxStock) {
                existing.quantity = maxStock;
                showToast(`Stock máximo alcanzado (${formatUnitLabel(maxStock)}).`);
            } else {
                existing.quantity = nextQty;
                showToast(`Se agregó ${product.name} al carrito.`);
            }
            existing.stock = maxStock;
            existing.price = Number(product.price || existing.price);
        } else {
            items.push({
                id: product.id,
                code: product.code || '',
                name: product.name || 'Producto',
                price: Number(product.price || 0),
                stock: maxStock,
                packaging: product.packaging || '',
                quantity: Math.min(qty, maxStock)
            });
            showToast(`Se agregó ${product.name} al carrito.`);
        }
        saveItems(items);
    }

    function updateQuantity(productId, delta) {
        const items = getItems();
        const idx = items.findIndex(i => i.id === productId);
        if (idx === -1) return;
        const item = items[idx];
        const nextQty = item.quantity + delta;
        if (nextQty <= 0) {
            items.splice(idx, 1);
        } else if (nextQty > item.stock) {
            item.quantity = item.stock;
            showToast(`Stock máximo disponible: ${formatUnitLabel(item.stock)}.`);
        } else {
            item.quantity = nextQty;
        }
        saveItems(items);
    }

    function removeItem(productId) {
        const items = getItems().filter(i => i.id !== productId);
        saveItems(items);
    }

    function showToast(msg) {
        let toast = document.getElementById('dm-cart-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'dm-cart-toast';
            toast.className = 'dm-toast';
            document.body.appendChild(toast);
        }
        toast.textContent = msg;
        toast.classList.add('visible');
        clearTimeout(toast._timer);
        toast._timer = setTimeout(() => toast.classList.remove('visible'), 2500);
    }

    function ensureDrawerDom() {
        if (document.getElementById('dm-cart-drawer')) return;
        const backdrop = document.createElement('div');
        backdrop.id = 'dm-cart-backdrop';
        backdrop.className = 'dm-drawer-backdrop';
        backdrop.addEventListener('click', closeDrawer);

        const drawer = document.createElement('aside');
        drawer.id = 'dm-cart-drawer';
        drawer.className = 'dm-cart-drawer';
        drawer.innerHTML = `
            <div class="dm-drawer-header">
                <h3>Tu Pedido Mayorista</h3>
                <button type="button" id="dm-close-drawer" class="dm-icon-btn" aria-label="Cerrar">&times;</button>
            </div>
            <div id="dm-drawer-items" class="dm-drawer-items"></div>
            <div class="dm-drawer-footer">
                <div class="dm-drawer-total-row">
                    <span>Subtotal estimado</span>
                    <strong id="dm-drawer-subtotal">$0</strong>
                </div>
                <div class="dm-drawer-actions">
                    <a href="/checkout" id="dm-go-checkout" class="btn btn-primary" style="width:100%; text-align:center;">Finalizar Compra →</a>
                    <button type="button" id="dm-clear-cart-btn" style="background:none; border:none; color:var(--muted); font-size:0.78rem; text-decoration:underline; cursor:pointer; width:100%; text-align:center; padding:0.4rem 0;">Vaciar carrito</button>
                </div>
            </div>
        `;
        document.body.appendChild(backdrop);
        document.body.appendChild(drawer);
        document.getElementById('dm-close-drawer').addEventListener('click', closeDrawer);
        document.getElementById('dm-clear-cart-btn').addEventListener('click', () => {
            clear();
            showToast('Carrito vaciado.');
        });
    }

    function openDrawer() {
        ensureDrawerDom();
        renderDrawer();
        document.getElementById('dm-cart-backdrop').classList.add('open');
        document.getElementById('dm-cart-drawer').classList.add('open');
    }

    function closeDrawer() {
        const backdrop = document.getElementById('dm-cart-backdrop');
        const drawer = document.getElementById('dm-cart-drawer');
        if (backdrop) backdrop.classList.remove('open');
        if (drawer) drawer.classList.remove('open');
    }

    function renderCartBadge() {
        const count = getItems().reduce((acc, item) => acc + Number(item.quantity || 0), 0);
        document.querySelectorAll('#dm-cart-count, .dm-cart-count').forEach(el => {
            el.textContent = String(count);
        });
    }

    function renderDrawer() {
        const container = document.getElementById('dm-drawer-items');
        const subtotalEl = document.getElementById('dm-drawer-subtotal');
        const goBtn = document.getElementById('dm-go-checkout');
        if (!container || !subtotalEl) return;

        const items = getItems();
        if (items.length === 0) {
            container.innerHTML = `
                <div class="dm-drawer-empty">
                    <p>Tu carrito mayorista está vacío.</p>
                    <p style="font-size:0.85rem; color:#7A6254;">Agregá productos desde el catálogo para iniciar tu compra.</p>
                </div>`;
            subtotalEl.textContent = '$0';
            if (goBtn) {
                goBtn.style.opacity = '0.5';
                goBtn.style.pointerEvents = 'none';
            }
            return;
        }

        if (goBtn) {
            goBtn.style.opacity = '1';
            goBtn.style.pointerEvents = 'auto';
        }

        let subtotal = 0;
        container.innerHTML = '';
        items.forEach(item => {
            const lineTotal = Number(item.price) * Number(item.quantity);
            subtotal += lineTotal;

            const row = document.createElement('div');
            row.className = 'dm-drawer-item';
            row.innerHTML = `
                <div class="dm-drawer-item-info">
                    <div class="dm-drawer-item-title">${escapeHtml(item.name)}</div>
                    <div class="dm-drawer-item-meta">${formatMoney(item.price)} c/u • Stock: ${formatUnitLabel(item.stock)}</div>
                </div>
                <div class="dm-drawer-item-controls">
                    <button type="button" class="dm-qty-btn" data-action="dec">−</button>
                    <span class="dm-qty-val">${item.quantity}</span>
                    <button type="button" class="dm-qty-btn" data-action="inc">+</button>
                    <button type="button" class="dm-remove-btn" title="Quitar">&times;</button>
                </div>
                <div class="dm-drawer-item-sub">${formatMoney(lineTotal)}</div>
            `;
            row.querySelector('[data-action="dec"]').addEventListener('click', () => updateQuantity(item.id, -1));
            row.querySelector('[data-action="inc"]').addEventListener('click', () => updateQuantity(item.id, 1));
            row.querySelector('.dm-remove-btn').addEventListener('click', () => removeItem(item.id));
            container.appendChild(row);
        });
        subtotalEl.textContent = formatMoney(subtotal);
    }

    function bindProductButtons() {
        document.querySelectorAll('.dm-add-btn').forEach(btn => {
            if (btn.dataset.bound === '1') return;
            btn.dataset.bound = '1';
            btn.addEventListener('click', () => {
                const card = btn.closest('.product-card');
                const qtyInput = card ? card.querySelector('.dm-card-qty-input') : null;
                const qty = qtyInput ? Math.max(1, parseInt(qtyInput.value || '1', 10)) : 1;
                addItem({
                    id: btn.dataset.id,
                    code: btn.dataset.code,
                    name: btn.dataset.name,
                    price: parseFloat(btn.dataset.price || '0'),
                    stock: parseInt(btn.dataset.stock || '0', 10),
                    packaging: btn.dataset.packaging || ''
                }, qty);
            });
        });

        document.querySelectorAll('#dm-open-cart-btn, .dm-open-cart-trigger').forEach(btn => {
            if (btn.dataset.bound === '1') return;
            btn.dataset.bound = '1';
            btn.addEventListener('click', openDrawer);
        });
    }

    window.DulceCart = {
        getItems,
        addItem,
        updateQuantity,
        removeItem,
        clear,
        openDrawer,
        closeDrawer,
        formatMoney,
        formatUnitLabel,
        escapeHtml
    };

    document.addEventListener('DOMContentLoaded', () => {
        ensureDrawerDom();
        renderCartBadge();
        renderDrawer();
        bindProductButtons();
    });
})();

