import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types/enums';
import { LogIn, Truck, ClipboardList } from 'lucide-react';
import { Auth0SocialButtons } from '../components/Auth0SocialButtons';

export const LoginView: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('preventa_mza');
  const [password, setPassword] = useState('dulcemayor');
  const [selectedRole, setSelectedRole] = useState<UserRole>(UserRole.PRESALE_AGENT);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (username.trim() && password) login(username.trim(), selectedRole);
  };

  return (
    <div className="login-screen">
      <div className="login-card">
        {/* Brand Header */}
        <div className="login-brand">
          <img src="/logo.svg" alt="Dulce Mayor" className="login-logo" />
          <h1 className="login-title">Dulce Mayor</h1>
          <span className="login-tag">Operaciones de Campo • Mendoza</span>
        </div>

        {/* Role Segment Selector */}
        <div className="role-selector">
          <button
            type="button"
            className={`role-tab ${selectedRole === UserRole.PRESALE_AGENT ? 'active' : ''}`}
            onClick={() => {
              setSelectedRole(UserRole.PRESALE_AGENT);
              setUsername('preventa_mza');
            }}
          >
            <ClipboardList size={16} />
            <span>Preventista</span>
          </button>
          <button
            type="button"
            className={`role-tab ${selectedRole === UserRole.DELIVERY_AGENT ? 'active' : ''}`}
            onClick={() => {
              setSelectedRole(UserRole.DELIVERY_AGENT);
              setUsername('reparto_mza');
            }}
          >
            <Truck size={16} />
            <span>Distribuidor</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label className="form-label">Usuario o Correo</label>
            <input
              type="text"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="form-input"
              placeholder="ej: preventa_mza"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="form-input"
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" className="btn-primary btn-block">
            <LogIn size={16} />
            <span>Ingresar a la Jornada</span>
          </button>
        </form>

        <div className="login-divider">
          <span>o ingresar con Google</span>
        </div>

        <Auth0SocialButtons selectedRole={selectedRole} />

        <div className="login-footer-info">
          <span>Sistema Mayorista de Distribución • Mendoza</span>
        </div>
      </div>
    </div>
  );
};
