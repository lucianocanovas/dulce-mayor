(function () {
    const HISTORY_KEY = 'dulce_mayor_ai_chat_v1';

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function formatMoney(value) {
        return '$' + Number(value || 0).toLocaleString('es-AR', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        });
    }

    function formatReplyText(text) {
        return escapeHtml(text)
            .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.+?)\*/g, '<em>$1</em>')
            .replace(/\n/g, '<br/>');
    }

    document.addEventListener('DOMContentLoaded', () => {
        const root = document.getElementById('dm-ai-chat-root');
        const toggleBtn = document.getElementById('dm-chat-toggle');
        const closeBtn = document.getElementById('dm-chat-close');
        const chatWindow = document.getElementById('dm-chat-window');
        const messagesEl = document.getElementById('dm-chat-messages');
        const form = document.getElementById('dm-chat-form');
        const input = document.getElementById('dm-chat-input');
        const sendBtn = document.getElementById('dm-chat-send');
        const chips = document.querySelectorAll('#dm-chat-suggestions .dm-chip');

        if (!root || !toggleBtn || !chatWindow || !messagesEl || !form || !input) return;

        const isAuthenticated = root.dataset.authenticated === 'true';
        const customerName = root.dataset.customerName || '';

        let history = [];
        try {
            const saved = sessionStorage.getItem(HISTORY_KEY);
            if (saved) history = JSON.parse(saved);
        } catch (e) {
            history = [];
        }

        function saveHistory() {
            try {
                sessionStorage.setItem(HISTORY_KEY, JSON.stringify(history.slice(-16)));
            } catch (e) { /* ignore */ }
        }

        function scrollToBottom() {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }

        function appendBubble(role, text, quoteData) {
            const wrapper = document.createElement('div');
            wrapper.className = 'dm-msg ' + (role === 'user' ? 'dm-msg-user' : 'dm-msg-bot');

            const bubble = document.createElement('div');
            bubble.className = 'dm-msg-bubble';
            bubble.innerHTML = role === 'user' ? escapeHtml(text) : formatReplyText(text);
            wrapper.appendChild(bubble);

            if (quoteData && Array.isArray(quoteData.recommendedItems) && quoteData.recommendedItems.length > 0) {
                const card = document.createElement('div');
                card.className = 'dm-quote-card';

                let rowsHtml = quoteData.recommendedItems.map(item => `
                    <div class="dm-quote-row">
                        <div>
                            <div class="dm-quote-name">${escapeHtml(item.name)}</div>
                            <div class="dm-quote-meta">${item.quantity}x ${escapeHtml(item.packaging || 'Pack')} • ${formatMoney(item.unitPrice)} c/u</div>
                        </div>
                        <div class="dm-quote-sub">${formatMoney(item.subtotal)}</div>
                    </div>
                `).join('');

                const usdLine = quoteData.estimatedTotalUsd
                    ? `<div class="dm-quote-usd">≈ USD ${Number(quoteData.estimatedTotalUsd).toFixed(2)} (TC Ref: $${Number(quoteData.usdRate || 0).toFixed(0)})</div>`
                    : '';

                card.innerHTML = `
                    <div class="dm-quote-header">Presupuesto Mayorista Estimado</div>
                    <div class="dm-quote-items">${rowsHtml}</div>
                    <div class="dm-quote-total">
                        <span>Total Estimado:</span>
                        <strong>${formatMoney(quoteData.estimatedTotal)} ARS</strong>
                    </div>
                    ${usdLine}
                    <div class="dm-quote-actions">
                        <button type="button" class="dm-quote-add-btn">🛒 Agregar Todo al Carrito</button>
                        <a href="/checkout" class="dm-quote-checkout-link">Ir al Checkout</a>
                        ${!isAuthenticated ? '<a href="/register" class="dm-quote-register-link">Crear Cuenta</a>' : ''}
                    </div>
                    <div class="dm-quote-disclaimer">${escapeHtml(quoteData.disclaimer || '')}</div>
                `;

                const addAllBtn = card.querySelector('.dm-quote-add-btn');
                if (addAllBtn) {
                    addAllBtn.addEventListener('click', () => {
                        if (!window.DulceCart) return;
                        quoteData.recommendedItems.forEach(item => {
                            window.DulceCart.addItem({
                                id: item.productId,
                                code: item.code,
                                name: item.name,
                                price: item.unitPrice,
                                stock: item.currentStock,
                                packaging: item.packaging
                            }, item.quantity);
                        });
                        addAllBtn.textContent = '✓ Agregado al Carrito';
                        addAllBtn.disabled = true;
                    });
                }

                wrapper.appendChild(card);
            }

            messagesEl.appendChild(wrapper);
            scrollToBottom();
        }

        function renderInitialMessages() {
            messagesEl.innerHTML = '';
            if (history.length === 0) {
                const welcome = isAuthenticated
                    ? `¡Hola ${customerName}! Soy tu asistente mayorista de Dulce Mayor. Puedo armarte presupuestos con stock en tiempo real o consultar el estado de tus pedidos y dirección de entrega.`
                    : '¡Hola! Soy el asistente de Dulce Mayor. Decime qué golosinas buscás o cuánto querés invertir (ej: *"Surtido por $50.000"*) y te armo un presupuesto mayorista al instante.';
                appendBubble('assistant', welcome, null);
            } else {
                history.forEach(entry => {
                    appendBubble(entry.role, entry.content, entry.quoteData || null);
                });
            }
        }

        function setOpen(open) {
            chatWindow.classList.toggle('open', open);
            chatWindow.setAttribute('aria-hidden', open ? 'false' : 'true');
            if (open) {
                input.focus();
                scrollToBottom();
            }
        }

        toggleBtn.addEventListener('click', () => {
            setOpen(!chatWindow.classList.contains('open'));
        });

        if (closeBtn) {
            closeBtn.addEventListener('click', () => setOpen(false));
        }

        async function sendMessage(text) {
            const trimmed = text.trim();
            if (!trimmed) return;

            appendBubble('user', trimmed, null);
            history.push({ role: 'user', content: trimmed });
            saveHistory();

            input.value = '';
            input.disabled = true;
            sendBtn.disabled = true;

            const typingEl = document.createElement('div');
            typingEl.className = 'dm-msg dm-msg-bot';
            typingEl.innerHTML = '<div class="dm-msg-bubble dm-typing">Consultando catálogo mayorista...</div>';
            messagesEl.appendChild(typingEl);
            scrollToBottom();

            try {
                const payloadHistory = history
                    .slice(-8)
                    .map(h => ({ role: h.role, content: h.content }));

                const res = await fetch('/api/landing/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: trimmed, history: payloadHistory })
                });
                const json = await res.json();
                typingEl.remove();

                if (!res.ok || !json.success || !json.data) {
                    const errText = json.message || 'No pude procesar tu consulta en este momento.';
                    appendBubble('assistant', errText, null);
                } else {
                    const data = json.data;
                    appendBubble('assistant', data.reply, data);
                    history.push({ role: 'assistant', content: data.reply, quoteData: data });
                    saveHistory();
                }
            } catch (err) {
                typingEl.remove();
                appendBubble('assistant', 'Ocurrió un error de conexión con el asistente. Por favor intentá nuevamente.', null);
            } finally {
                input.disabled = false;
                sendBtn.disabled = false;
                input.focus();
            }
        }

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            sendMessage(input.value);
        });

        chips.forEach(chip => {
            chip.addEventListener('click', () => {
                const prompt = chip.getAttribute('data-prompt') || chip.textContent;
                sendMessage(prompt);
            });
        });

        renderInitialMessages();
    });
})();
