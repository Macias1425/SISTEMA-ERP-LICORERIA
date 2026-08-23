import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './app/auth/LoginPage';
import CambiarPasswordPage from './app/auth/CambiarPasswordPage';
import DashboardPage from './app/dashboard/DashboardPage';
import InventarioPage from './app/inventario/InventarioPage';
import PosPage from './app/pos/PosPage';
import Layout from './components/ui/Layout';
import { RequireAuth, RequireGuest, RequireRole } from './components/ui/ProtectedRoute';

export default function App() {
  return (
    <Routes>
      <Route element={<RequireGuest />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route path="/cambiar-password" element={<CambiarPasswordPage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route element={<RequireRole roles={['ALMACENISTA', 'ADMIN']} />}>
            <Route path="/inventario" element={<InventarioPage />} />
          </Route>
          <Route element={<RequireRole roles={['CAJERO', 'ADMIN']} />}>
            <Route path="/pos" element={<PosPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
