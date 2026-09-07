import { Navigate, Route, Routes } from 'react-router-dom';

import { PERMISOS } from './auth/permisos';
import { rutaInicioPorPermisos } from './auth/rolesInfo';
import { useAuth } from './auth/AuthContext';

import LandingPage from './app/auth/LandingPage';
import LoginPage from './app/auth/LoginPage';

import CambiarPasswordPage from './app/auth/CambiarPasswordPage';

import DashboardPage from './app/dashboard/DashboardPage';

import ReportesPage from './app/reportes/ReportesPage';

import VencimientosPage from './app/reportes/VencimientosPage';
import CorteDiaPage from './app/reportes/CorteDiaPage';

import CategoriasPage from './app/catalogo/CategoriasPage';

import ProductosPage from './app/catalogo/ProductosPage';

import ProveedoresPage from './app/catalogo/ProveedoresPage';

import InventarioPage from './app/inventario/InventarioPage';
import ComprasPage from './app/compras/ComprasPage';

import PosPage from './app/pos/PosPage';

import FacturasPage from './app/facturas/FacturasPage';

import UsuariosPage from './app/usuarios/UsuariosPage';

import PermisosPage from './app/usuarios/PermisosPage';

import ControlVentasPage from './app/control-ventas/ControlVentasPage';

import ConfiguracionPage from './app/configuracion/ConfiguracionPage';

import AuditoriaPage from './app/auditoria/AuditoriaPage';

import MantenimientoPage from './app/mantenimiento/MantenimientoPage';

import FinanzasPage from './app/finanzas/FinanzasPage';
import CreditosPage from './app/creditos/CreditosPage';
import MarcasPreciosPage from './app/marcas-precios/MarcasPreciosPage';

import Layout from './components/ui/Layout';

import { RequireAuth, RequireGuest, RequirePermiso } from './components/ui/ProtectedRoute';

function RutaInicio() {
  const { usuario } = useAuth();
  return <Navigate to={rutaInicioPorPermisos(usuario)} replace />;
}

export default function App() {

  return (

    <Routes>

      <Route element={<RequireGuest />}>

        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />

      </Route>



      <Route element={<RequireAuth />}>

        <Route path="/cambiar-password" element={<CambiarPasswordPage />} />

        <Route element={<Layout />}>

          <Route path="/" element={<RutaInicio />} />

          <Route element={<RequirePermiso permisos={[PERMISOS.DASHBOARD_VER]} />}>

            <Route path="/dashboard" element={<DashboardPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.REPORTES_VER]} />}>

            <Route path="/reportes" element={<ReportesPage />} />

            <Route path="/corte-dia" element={<CorteDiaPage />} />

            <Route path="/vencimientos" element={<VencimientosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.FINANZAS_VER]} />}>

            <Route path="/finanzas" element={<FinanzasPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.PRODUCTOS_VER, PERMISOS.PRODUCTOS_GESTIONAR]} />}>

            <Route path="/productos" element={<ProductosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.MARCAS_PRECIOS_GESTIONAR]} />}>

            <Route path="/marcas-precios" element={<MarcasPreciosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.PROVEEDORES_GESTIONAR]} />}>

            <Route path="/proveedores" element={<ProveedoresPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.CATEGORIAS_GESTIONAR]} />}>

            <Route path="/categorias" element={<CategoriasPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.INVENTARIO_VER]} />}>

            <Route path="/inventario" element={<InventarioPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.INVENTARIO_VER, PERMISOS.COMPRAS_GESTIONAR]} />}>

            <Route path="/compras" element={<ComprasPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.VENTAS_CREAR]} />}>

            <Route path="/pos" element={<PosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.FACTURAS_VER]} />}>

            <Route path="/facturas" element={<FacturasPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.FACTURAS_VER, PERMISOS.VENTAS_CREAR]} />}>
            <Route path="/creditos" element={<CreditosPage />} />
          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.USUARIOS_VER, PERMISOS.USUARIOS_GESTIONAR]} />}>

            <Route path="/usuarios" element={<UsuariosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.PERMISOS_GESTIONAR]} />}>

            <Route path="/usuarios/permisos" element={<PermisosPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.CONTROL_VENTAS_VER]} />}>

            <Route path="/control-ventas" element={<ControlVentasPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.CONFIG_GESTIONAR]} />}>

            <Route path="/configuracion" element={<ConfiguracionPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.AUDITORIA_VER]} />}>

            <Route path="/auditoria" element={<AuditoriaPage />} />

          </Route>

          <Route element={<RequirePermiso permisos={[PERMISOS.MANTENIMIENTO_GESTIONAR]} />}>

            <Route path="/mantenimiento" element={<MantenimientoPage />} />

          </Route>

        </Route>

      </Route>



      <Route path="*" element={<Navigate to="/" replace />} />

    </Routes>

  );

}

