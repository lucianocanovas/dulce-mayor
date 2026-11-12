(function () {
    const btn = document.getElementById('dm-google-login-btn');
    const errBox = document.getElementById('dm-google-error');
    if (!btn || !errBox) return;

    const domain = btn.dataset.auth0Domain || '';
    const clientId = btn.dataset.auth0ClientId || '';

    btn.addEventListener('click', async () => {
        errBox.style.display = 'none';
        if (!domain || !clientId || !window.auth0) {
            errBox.textContent = 'Google Sign-In no está configurado o no se pudo cargar el SDK de Auth0.';
            errBox.style.display = 'block';
            return;
        }
        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.textContent = 'Conectando con Google...';
        try {
            const client = await window.auth0.createAuth0Client({
                domain: domain,
                clientId: clientId,
                authorizationParams: {
                    redirect_uri: window.location.origin + window.location.pathname
                }
            });
            await client.loginWithPopup({
                authorizationParams: {
                    connection: 'google-oauth2'
                }
            });
            const claims = await client.getIdTokenClaims();
            const user = await client.getUser();
            const email = (claims && claims.email) || (user && user.email) || '';
            const sub = (claims && claims.sub) || (user && user.sub) || '';
            const name = (claims && claims.name) || (user && user.name) || '';
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
        } catch (err) {
            errBox.textContent = (err && err.message) ? err.message : 'Error al iniciar sesión con Google.';
            errBox.style.display = 'block';
            btn.disabled = false;
            btn.innerHTML = originalHtml;
        }
    });
})();
