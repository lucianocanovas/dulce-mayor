import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import type { UserRole, AuthProvider } from '../types/enums';
import {
  UserPlus,
  Trash2,
  Key,
  CheckCircle2,
  AlertCircle,
  X,
  Shield,
  Eye,
  Edit2,
  AlertTriangle,
  User,
} from 'lucide-react';

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

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [viewUser, setViewUser] = useState<UserResponse | null>(null);
  const [editUser, setEditUser] = useState<UserResponse | null>(null);
  const [deleteUser, setDeleteUser] = useState<UserResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [username, setUsername] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [documentNumber, setDocumentNumber] = useState('');
  const [role, setRole] = useState<UserRole>('PRESALE_AGENT');
  const [employeeIdNumber, setEmployeeIdNumber] = useState('');
  const [password, setPassword] = useState('');

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<UserResponse[]>('/users');
      // Separar clientes (RETAILER) del personal interno
      setUsers(data.filter((u) => u.role !== 'RETAILER'));
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

  const openCreateModal = () => {
    resetForm();
    setIsModalOpen(true);
  };

  const openEditModal = (u: UserResponse) => {
    setEditUser(u);
    const parts = (u.fullName || '').split(' ');
    setUsername(u.username);
    setFirstName(parts[0] || '');
    setLastName(parts.slice(1).join(' ') || '');
    setDocumentNumber('');
    setRole(u.role);
    setEmployeeIdNumber('');
    setPassword('');
  };

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

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editUser) return;
    setSubmitting(true);
    setError(null);

    try {
      await apiRequest<UserResponse>(`/users/${editUser.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          username: editUser.username,
          firstName,
          lastName,
          documentNumber: documentNumber || undefined,
          role,
          password: password.trim() ? password.trim() : undefined,
        }),
      });

      setEditUser(null);
      resetForm();
      fetchUsers();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al actualizar datos del usuario');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteUser = async () => {
    if (!deleteUser) return;
    setSubmitting(true);
    try {
      await apiRequest<void>(`/users/${deleteUser.id}`, {
        method: 'DELETE',
      });
      setDeleteUser(null);
      fetchUsers();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error al dar de baja al usuario');
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setUsername('');
    setFirstName('');
    setLastName('');
    setDocumentNumber('');
    setRole('PRESALE_AGENT');
    setEmployeeIdNumber('');
    setPassword('');
  };

  const roleLabels: Record<string, string> = {
    MANAGER: 'Gerencia y Administración',
    ADMINISTRATIVE: 'Administrativo de Depósito',
    PRESALE_AGENT: 'Preventista de Calle',
    DELIVERY_AGENT: 'Repartidor de Logística',
  };

  return (
    <div className="users-view">
      <div className="view-header">
        <div>
          <h2>Administración de Personal y Usuarios</h2>
          <p className="text-muted">Gestión de cuentas internas, roles, preventistas y choferes de reparto.</p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            onClick={openCreateModal}
            className="btn btn-primary"
          >
            <UserPlus size={16} /> Nuevo Personal
          </button>
          <button onClick={fetchUsers} className="btn btn-secondary">
            Actualizar
          </button>
        </div>
      </div>

      {error && (
        <div style={{ background: '#fee2e2', border: '1px solid #ef4444', color: '#b91c1c', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="view-loading">
          <p>Cargando personal...</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Nombre y Apellido</th>
                  <th>Rol Asignado</th>
                  <th>Método Auth</th>
                  <th>Clave Provisoria</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Shield size={15} className="text-muted" />
                        <span className="font-mono font-bold">{u.username}</span>
                      </div>
                    </td>
                    <td className="font-semibold">{u.fullName}</td>
                    <td>
                      <span className={`badge ${u.role === 'MANAGER' ? 'badge-primary' : 'badge-neutral'}`}>
                        {roleLabels[u.role] || u.role}
                      </span>
                    </td>
                    <td>
                      <span className="badge badge-neutral">{u.authProvider}</span>
                    </td>
                    <td>
                      {u.temporaryPassword ? (
                        <span className="status-indicator warning">
                          <Key size={13} /> Sí ('dulcemayor')
                        </span>
                      ) : (
                        <span className="text-muted text-xs">Definida por usuario</span>
                      )}
                    </td>
                    <td>
                      {u.active ? (
                        <span className="status-indicator good">
                          <CheckCircle2 size={13} /> Activo
                        </span>
                      ) : (
                        <span className="status-indicator warning">
                          Inactivo
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="action-btn-group">
                        <button
                          onClick={() => setViewUser(u)}
                          className="action-icon-btn view"
                          title="Ver ficha de personal"
                        >
                          <Eye size={14} />
                          <span>Ver</span>
                        </button>
                        <button
                          onClick={() => openEditModal(u)}
                          className="action-icon-btn edit"
                          title="Editar rol y datos"
                        >
                          <Edit2 size={14} />
                          <span>Editar</span>
                        </button>
                        {u.username !== 'admin' && (
                          <button
                            onClick={() => setDeleteUser(u)}
                            className="action-icon-btn delete"
                            title="Dar de baja usuario"
                          >
                            <Trash2 size={14} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal: VER FICHA */}
      {viewUser && (
        <div className="modal-backdrop" onClick={() => setViewUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <User size={20} className="text-primary" />
                <h3 style={{ margin: 0 }}>Ficha del Empleado</h3>
              </div>
              <button onClick={() => setViewUser(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '8px', marginBottom: '1.25rem' }}>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
                {viewUser.fullName}
              </div>
              <span className="font-mono text-sm text-muted">@{viewUser.username}</span>
            </div>

            <div className="details-grid">
              <div className="detail-item">
                <span className="detail-label">Rol y Área</span>
                <span className="detail-value font-semibold">
                  {roleLabels[viewUser.role] || viewUser.role}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Proveedor de Autenticación</span>
                <span className="detail-value">{viewUser.authProvider}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Clave Provisoria</span>
                <span className="detail-value">
                  {viewUser.temporaryPassword ? 'Pendiente de cambio' : 'Clave confirmada'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Estado de la Cuenta</span>
                <span className="detail-value font-semibold text-success">
                  {viewUser.active ? 'Habilitado' : 'Baja'}
                </span>
              </div>
            </div>

            <div className="modal-actions">
              <button
                onClick={() => {
                  const u = viewUser;
                  setViewUser(null);
                  openEditModal(u);
                }}
                className="btn btn-primary"
              >
                <Edit2 size={16} /> Editar Usuario
              </button>
              <button onClick={() => setViewUser(null)} className="btn btn-secondary">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: CREAR USUARIO */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Alta de Personal</h3>
              <button onClick={() => setIsModalOpen(false)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateUser} className="modal-form">
              <label>
                Nombre de Usuario (Login) *
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value.toLowerCase())}
                  placeholder="ej. jtorres"
                  required
                />
              </label>

              <div className="grid-cols-2 gap-4">
                <label>
                  Nombre *
                  <input
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                </label>

                <label>
                  Apellido *
                  <input
                    type="text"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                  />
                </label>
              </div>

              <div className="grid-cols-2 gap-4">
                <label>
                  DNI *
                  <input
                    type="text"
                    value={documentNumber}
                    onChange={(e) => setDocumentNumber(e.target.value)}
                    placeholder="34567890"
                    required
                  />
                </label>

                <label>
                  Legajo / Nro Empleado
                  <input
                    type="text"
                    value={employeeIdNumber}
                    onChange={(e) => setEmployeeIdNumber(e.target.value)}
                    placeholder="EMP-012"
                  />
                </label>
              </div>

              <label>
                Rol en el Sistema *
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value as UserRole)}
                  style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                >
                  <option value="PRESALE_AGENT">Preventista de Calle (Order Management)</option>
                  <option value="DELIVERY_AGENT">Repartidor de Logística (Rutas y Entregas)</option>
                  <option value="ADMINISTRATIVE">Administrativo de Depósito</option>
                  <option value="MANAGER">Gerencia (Acceso Total)</option>
                </select>
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Registrar Empleado'}
                </button>
                <button type="button" onClick={() => setIsModalOpen(false)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: EDITAR USUARIO */}
      {editUser && (
        <div className="modal-backdrop" onClick={() => setEditUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Editar Usuario (@{editUser.username})</h3>
              <button onClick={() => setEditUser(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdateUser} className="modal-form">
              <div className="grid-cols-2 gap-4">
                <label>
                  Nombre *
                  <input
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                </label>

                <label>
                  Apellido *
                  <input
                    type="text"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                  />
                </label>
              </div>

              <label>
                Rol en la Empresa *
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value as UserRole)}
                  style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                >
                  <option value="PRESALE_AGENT">Preventista de Calle (Order Management)</option>
                  <option value="DELIVERY_AGENT">Repartidor de Logística (Rutas y Entregas)</option>
                  <option value="ADMINISTRATIVE">Administrativo de Depósito</option>
                  <option value="MANAGER">Gerencia (Acceso Total)</option>
                </select>
              </label>

              <label>
                Restablecer Contraseña (opcional - dejar vacío para conservar la actual):
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nueva contraseña..."
                />
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Guardar Cambios'}
                </button>
                <button type="button" onClick={() => setEditUser(null)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: ELIMINAR USUARIO */}
      {deleteUser && (
        <div className="modal-backdrop" onClick={() => setDeleteUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem', color: 'var(--danger)' }}>
              <AlertTriangle size={28} />
              <h3 style={{ margin: 0, color: 'var(--text-main)' }}>¿Dar de baja a {deleteUser.fullName}?</h3>
            </div>
            <p style={{ marginBottom: '1rem', color: 'var(--text-muted)' }}>
              El usuario <strong>@{deleteUser.username}</strong> perderá el acceso a las aplicaciones de gestión y preventa de Dulce Mayor.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                onClick={handleDeleteUser}
                disabled={submitting}
                className="btn btn-danger-outline"
                style={{ background: 'var(--danger)', color: '#fff', border: 'none' }}
              >
                {submitting ? 'Dando de baja...' : 'Sí, Dar de Baja'}
              </button>
              <button
                type="button"
                onClick={() => setDeleteUser(null)}
                className="btn btn-secondary"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
