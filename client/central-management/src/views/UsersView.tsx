import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import type { UserRole, AuthProvider } from '../types/enums';
import { UserPlus, Users, Trash2, Key, CheckCircle2, AlertCircle, X, Shield } from 'lucide-react';

interface UserResponse {
  id: string;
  username: string;
  fullName: string;
  role: UserRole;
  authProvider: AuthProvider;
  temporaryPassword: boolean;
  active: boolean;
}

export const UsersView: React.FC = () => {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [username, setUsername] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [documentNumber, setDocumentNumber] = useState('');
  const [role, setRole] = useState<UserRole>('PRESALE_AGENT');
  const [employeeIdNumber, setEmployeeIdNumber] = useState('');

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<UserResponse[]>('/users');
      setUsers(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al cargar la lista de personal');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await apiRequest<UserResponse>('/users', {
        method: 'POST',
        body: JSON.stringify({
          username,
          firstName,
          lastName,
          documentNumber,
          role,
          employeeIdNumber,
        }),
      });

      setIsModalOpen(false);
      resetForm();
      fetchUsers();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al registrar nuevo empleado');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteUser = async (id: string, name: string) => {
    if (!window.confirm(`¿Está seguro de dar de baja al usuario '${name}'?`)) {
      return;
    }

    try {
      await apiRequest<void>(`/users/${id}`, {
        method: 'DELETE',
      });
      fetchUsers();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error al eliminar usuario');
    }
  };

  const resetForm = () => {
    setUsername('');
    setFirstName('');
    setLastName('');
    setDocumentNumber('');
    setRole('PRESALE_AGENT');
    setEmployeeIdNumber('');
  };

  const getRoleBadge = (r: UserRole) => {
    switch (r) {
      case 'MANAGER':
        return <span className="badge badge-primary">Gerencia</span>;
      case 'ADMINISTRATIVE':
        return <span className="badge badge-info">Administrativo</span>;
      case 'PRESALE_AGENT':
        return <span className="badge badge-success">Preventista / Vendedor</span>;
      case 'DELIVERY_AGENT':
        return <span className="badge badge-warning">Repartidor / Logística</span>;
      default:
        return <span className="badge">{r}</span>;
    }
  };

  return (
    <div className="users-view">
      <div className="view-header">
        <div>
          <h2>Gestión de Personal y Usuarios</h2>
          <p className="text-muted">Control de accesos, roles y claves provisorias del equipo de Dulce Mayor.</p>
        </div>
        <button
          type="button"
          onClick={() => setIsModalOpen(true)}
          className="btn btn-primary flex items-center gap-2"
        >
          <UserPlus size={18} />
          Nuevo Personal
        </button>
      </div>

      {error && (
        <div className="login-error mb-4">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      <div className="panel">
        {loading ? (
          <div className="p-8 text-center text-muted">Cargando personal de la empresa...</div>
        ) : users.length === 0 ? (
          <div className="p-8 text-center text-muted">
            <Users size={48} className="mx-auto mb-2 opacity-50" />
            No hay empleados dados de alta en el sistema.
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Usuario / Legajo</th>
                <th>Nombre Completo</th>
                <th>Rol / Función</th>
                <th>Proveedor</th>
                <th>Estado Clave</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>
                    <div className="font-semibold">{u.username}</div>
                  </td>
                  <td>{u.fullName}</td>
                  <td>{getRoleBadge(u.role)}</td>
                  <td>
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-gray-100">
                      {u.authProvider}
                    </span>
                  </td>
                  <td>
                    {u.temporaryPassword ? (
                      <span className="badge badge-warning flex items-center gap-1">
                        <Key size={12} /> Temporal (dulcemayor)
                      </span>
                    ) : (
                      <span className="badge badge-success flex items-center gap-1">
                        <CheckCircle2 size={12} /> Clave Personalizada
                      </span>
                    )}
                  </td>
                  <td className="text-right">
                    <button
                      type="button"
                      onClick={() => handleDeleteUser(u.id, u.username)}
                      className="btn-icon text-danger"
                      title="Dar de baja usuario"
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal Nuevo Personal */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <div className="flex items-center gap-2">
                <UserPlus className="text-primary" size={22} />
                <h3 className="font-bold">Dar de Alta Nuevo Personal</h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsModalOpen(false);
                  resetForm();
                }}
                className="btn-icon"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateUser}>
              <div className="modal-body space-y-4">
                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Nombre:</span>
                    <input
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      placeholder="ej. Juan"
                      required
                    />
                  </label>
                  <label className="form-group">
                    <span className="form-label">Apellido:</span>
                    <input
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      placeholder="ej. Pérez"
                      required
                    />
                  </label>
                </div>

                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Usuario (para iniciar sesión):</span>
                    <input
                      type="text"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="ej. jperez"
                      required
                    />
                  </label>
                  <label className="form-group">
                    <span className="form-label">DNI / Documento:</span>
                    <input
                      type="text"
                      value={documentNumber}
                      onChange={(e) => setDocumentNumber(e.target.value)}
                      placeholder="ej. 35123456"
                      required
                    />
                  </label>
                </div>

                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Rol en Dulce Mayor:</span>
                    <select
                      value={role}
                      onChange={(e) => setRole(e.target.value as UserRole)}
                      required
                    >
                      <option value="PRESALE_AGENT">Preventista / Vendedor Móvil</option>
                      <option value="DELIVERY_AGENT">Repartidor / Logística</option>
                      <option value="ADMINISTRATIVE">Administrativo Central</option>
                      <option value="MANAGER">Gerente Comercial</option>
                    </select>
                  </label>
                  <label className="form-group">
                    <span className="form-label">N° de Legajo:</span>
                    <input
                      type="text"
                      value={employeeIdNumber}
                      onChange={(e) => setEmployeeIdNumber(e.target.value)}
                      placeholder="ej. PREV-015"
                    />
                  </label>
                </div>

                <div className="p-3 bg-warning-soft rounded-lg border border-warning-soft text-xs text-muted">
                  <div className="flex items-center gap-1 font-semibold text-warning mb-1">
                    <Shield size={14} /> Contraseña Provisoria Automática
                  </div>
                  El usuario se creará con la clave provisoria predeterminada <strong>dulcemayor</strong>. En su primer ingreso, el sistema le exigirá de forma obligatoria establecer una contraseña nueva y privada.
                </div>
              </div>

              <div className="modal-footer flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setIsModalOpen(false);
                    resetForm();
                  }}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-primary"
                >
                  {submitting ? 'Guardando...' : 'Registrar Empleado'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
