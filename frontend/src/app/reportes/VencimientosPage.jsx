import { Navigate } from 'react-router-dom';

export default function VencimientosPage() {
  return <Navigate to="/reportes?tab=vencimientos" replace />;
}
