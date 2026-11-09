import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert, Lock, CheckCircle2, AlertCircle } from 'lucide-react';

export const ChangePasswordModal: React.FC = () => {
  const { user, changePassword } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('dulcemayor');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user || !user.temporaryPassword) {
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 6) {
      setError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    if (newPassword === 'dulcemayor') {
      setError('La nueva contraseña no puede ser igual a la provisoria por defecto.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Las contraseñas no coinciden.');
      return;
    }

    setLoading(true);
    try {
      await changePassword(newPassword, currentPassword);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al actualizar la contraseña');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-dialog">
        <div className="modal-header bg-warning-soft">
          <div className="flex items-center gap-2 text-warning">
            <ShieldAlert size={24} />
            <h3 className="font-bold">Actualización Obligatoria de Contraseña</h3>
          </div>
        </div>

        <div className="modal-body">
          <p className="text-muted mb-4">
            Hola <strong>{user.fullName || user.username}</strong>. Su cuenta actualmente tiene asignada la clave provisoria del sistema. Para proteger la información de Dulce Mayor, configure una nueva clave personal.
          </p>

          {error && (
            <div className="login-error mb-4">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="form-group">
              <span className="form-label">Contraseña Provisoria Actual:</span>
              <div className="input-with-icon">
                <Lock size={16} />
                <input
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="dulcemayor"
                  required
                />
              </div>
            </label>

            <label className="form-group">
              <span className="form-label">Nueva Contraseña:</span>
              <div className="input-with-icon">
                <Lock size={16} />
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Mínimo 6 caracteres"
                  required
                />
              </div>
            </label>

            <label className="form-group">
              <span className="form-label">Confirmar Nueva Contraseña:</span>
              <div className="input-with-icon">
                <Lock size={16} />
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repita la nueva contraseña"
                  required
                />
              </div>
            </label>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-block mt-4 flex items-center justify-center gap-2"
            >
              <CheckCircle2 size={18} />
              {loading ? 'Guardando nueva clave...' : 'Actualizar y Entrar al Sistema'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
