(function () {
    const btn = document.getElementById('dm-google-login-btn');
    const errBox = document.getElementById('dm-google-error');
    if (!btn || !errBox) return;

    const domain = btn.dataset.auth0Domain || '';
    const clientId = btn.dataset.auth0ClientId || '';
    let clientInstance = null;

    function getClient() {
        if (!clientInstance) {
            const opts = {
                domain: domain,
                clientId: clientId,
                authorizationParams: {
                    redirect_uri: window.location.origin
                }
            };
            if (typeof window.auth0.Auth0Client === 'function') {
                clientInstance = Promise.resolve(new window.auth0.Auth0Client(opts));
            } else {
                clientInstance = window.auth0.createAuth0Client(opts);
            }
        }
        return clientInstance;
    }

    async function completeSocialSession(client) {
        const claims = await client.getIdTokenClaims();
        const user = await client.getUser();
        const sub = (claims && claims.sub) || (user && user.sub) || '';
        const email = (claims && claims.email) || (user && user.email) || (sub ? `${sub}@auth0.dulcemayor.com` : '');
        const name = (claims && claims.name) || (user && user.name) || (user && user.nickname) || 'Usuario Auth0';
        const picture = (claims && claims.picture) || (user && user.picture) || '';

        const res = await fetch('/auth/social', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, name, sub, provider: 'GOOGLE', picture })
        });
        const data = await res.json();
        if (!res.ok || !data.ok) {
            throw new Error(data.message || 'No se pudo iniciar sesión con Google.');
        }
        window.location.href = data.redirectUrl || '/my-orders';
    }

    function showError(msg, originalHtml) {
        errBox.textContent = msg;
        errBox.style.display = 'block';
        btn.disabled = false;
        if (originalHtml) btn.innerHTML = originalHtml;
    }

    // Pre-initialize Auth0 client and handle redirect callback if returning from Auth0
    if (domain && clientId && window.auth0) {
        const params = new URLSearchParams(window.location.search);
        if (params.has('code') && params.has('state')) {
            const originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.textContent = 'Finalizando ingreso con Google...';
            getClient()
                .then(async (client) => {
                    await client.handleRedirectCallback();
                    window.history.replaceState({}, document.title, window.location.pathname);
                    await completeSocialSession(client);
                })
                .catch((err) => {
                    console.error('Auth0 redirect callback error:', err);
                    showError((err && err.message) ? err.message : 'Error al completar el ingreso con Google.', originalHtml);
                });
        } else {
            getClient().catch((err) => console.warn('Auth0 pre-init warning:', err));
        }
    }

    btn.addEventListener('click', async () => {
        errBox.style.display = 'none';
        if (!domain || !clientId || !window.auth0) {
            showError('Google Sign-In no está configurado o no se pudo cargar el SDK de Auth0.');
            return;
        }

        // Open popup synchronously inside click handler so browsers never block it after an await
        const popup = window.open('', 'auth0:authorize:popup', 'width=500,height=650,scrollbars=yes,resizable=yes');
        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.textContent = 'Conectando con Google...';

        try {
            const client = await getClient();
            if (!popup || popup.closed) {
                // Fallback to full-page redirect if browser blocked the popup
                await client.loginWithRedirect({
                    authorizationParams: {
                        connection: 'google-oauth2',
                        redirect_uri: window.location.origin + window.location.pathname
                    }
                });
                return;
            }

            await client.loginWithPopup(
                {
                    authorizationParams: {
                        connection: 'google-oauth2',
                        redirect_uri: window.location.origin
                    }
                },
                { popup, timeoutInSeconds: 60 }
            );
            await completeSocialSession(client);
        } catch (err) {
            if (popup && !popup.closed) {
                try { popup.close(); } catch (_) { /* ignore */ }
            }
            console.error('Auth0 login error:', err);
            showError((err && err.message) ? err.message : 'Error al iniciar sesión con Google.', originalHtml);
        }
    });
})();
