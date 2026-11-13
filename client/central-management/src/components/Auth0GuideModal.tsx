import React from 'react';
import { X, ExternalLink, Check, Copy, Key, Globe, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export const Auth0GuideModal: React.FC<Props> = ({ isOpen, onClose }) => {
  const { loginSocial } = useAuth();
  const [copied, setCopied] = React.useState<string | null>(null);

  if (!isOpen) return null;

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopied(label);
    setTimeout(() => setCopied(null), 2000);
  };

  const handleSimulateGoogle = async () => {
    await loginSocial({
      email: 'operador.mendoza@dulcemayor.com',
      name: 'Carlos Mendoza',
      sub: 'google-oauth2|109283746501928374',
      provider: 'GOOGLE',
      picture: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100',
    });
    onClose();
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-dialog guide-modal">
        <div className="modal-header">
          <div className="flex items-center gap-2">
            <Key className="text-primary" size={24} />
            <div>
              <h3 className="font-bold text-lg">Guía de Configuración Auth0 (Google)</h3>
              <p className="text-xs text-muted">Pasos para conectar Auth0 con Central Management</p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn-icon">
            <X size={20} />
          </button>
        </div>

        <div className="modal-body space-y-6">
          {/* Step 1 */}
          <div className="guide-step">
            <div className="step-badge">1</div>
            <div className="step-content">
              <h4 className="font-semibold text-sm">Crear Cuenta y Aplicación en Auth0</h4>
              <p className="text-xs text-muted mt-1">
                Ingresa en <a href="https://manage.auth0.com" target="_blank" rel="noreferrer" className="text-primary inline-flex items-center gap-1 font-medium">Auth0 Dashboard <ExternalLink size={12} /></a> y crea una aplicación de tipo <strong>Single Page Web Applications</strong> llamada <em>Dulce Mayor</em>.
              </p>
            </div>
          </div>

          {/* Step 2 */}
          <div className="guide-step">
            <div className="step-badge">2</div>
            <div className="step-content">
              <h4 className="font-semibold text-sm">Configurar URLs en Auth0 Settings</h4>
              <p className="text-xs text-muted mt-1">
                En la pestaña <strong>Settings</strong> de tu aplicación en Auth0, agrega la URL de Vite:
              </p>
              <div className="url-copy-box mt-2">
                <code>http://localhost:5173</code>
                <button
                  type="button"
                  onClick={() => copyToClipboard('http://localhost:5173', 'url')}
                  className="btn-copy"
                >
                  {copied === 'url' ? <Check size={14} className="text-success" /> : <Copy size={14} />}
                </button>
              </div>
              <p className="text-xs text-muted mt-1 italic">
                (Pega esta URL en: <strong>Allowed Callback URLs</strong>, <strong>Allowed Logout URLs</strong> y <strong>Allowed Web Origins</strong>).
              </p>
            </div>
          </div>

          {/* Step 3 */}
          <div className="guide-step">
            <div className="step-badge">3</div>
            <div className="step-content">
              <h4 className="font-semibold text-sm">Habilitar Proveedor Social Google</h4>
              <ul className="text-xs text-muted mt-1 space-y-1 list-disc pl-4">
                <li>
                  <strong>Google:</strong> En el menú Auth0 <em>Authentication → Social</em>, activa la conexión de Google. Para desarrollo ya incluye claves de prueba (<em>Auth0 Developer Keys</em>) sin configurar nada más.
                </li>
              </ul>
            </div>
          </div>

          {/* Step 4 */}
          <div className="guide-step">
            <div className="step-badge">4</div>
            <div className="step-content">
              <h4 className="font-semibold text-sm">Cargar variables en <code>client/central-management/.env</code></h4>
              <pre className="env-code-box mt-2">
                {`VITE_AUTH0_DOMAIN=tu-tenant.us.auth0.com\nVITE_AUTH0_CLIENT_ID=tu_client_id_aqui\nVITE_API_URL=http://localhost:8080/api`}
              </pre>
            </div>
          </div>

          {/* Quick Simulation Section */}
          <div className="p-4 bg-primary-soft rounded-lg border border-primary-soft">
            <div className="flex items-center gap-2 font-semibold text-primary text-sm mb-2">
              <ShieldCheck size={18} />
              <span>¿Quieres probar la integración backend ahora mismo?</span>
            </div>
            <p className="text-xs text-muted mb-3">
              Puedes simular la respuesta que Auth0 envía tras autenticarte en Google. Esto registrará y validará inmediatamente al usuario en la base de datos Spring Boot:
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleSimulateGoogle}
                className="btn btn-sm btn-outline flex-1 flex items-center justify-center gap-1"
              >
                <Globe size={14} /> Probar con Google
              </button>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button type="button" onClick={onClose} className="btn btn-primary w-full">
            Entendido, cerrar guía
          </button>
        </div>
      </div>
    </div>
  );
};
