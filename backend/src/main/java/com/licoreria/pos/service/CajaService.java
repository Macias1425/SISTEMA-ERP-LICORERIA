package com.licoreria.pos.service;

import com.licoreria.pos.dto.AperturaCajaDTO;
import com.licoreria.pos.dto.CierreCajaDTO;
import com.licoreria.pos.dto.EstadoCajaDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.Permiso;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TurnoCaja;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.TurnoCajaRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.repository.VentaRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;
    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Transactional
    public TurnoCajaDTO abrir(AperturaCajaDTO dto) {
        Usuario cajero = accesoService.exigirPermiso(Permiso.CAJA_OPERAR);
        turnoCajaRepository.findByUsuarioIdAndEstado(cajero.getId(), EstadoTurnoCaja.ABIERTO)
                .ifPresent(abierto -> {
                    throw new ReglaNegocioException("CAJA_YA_ABIERTA", "El usuario ya tiene un turno de caja abierto");
                });

        TurnoCaja turno = TurnoCaja.builder()
                .usuarioId(cajero.getId())
                .fechaApertura(LocalDateTime.now(clock))
                .montoInicial(dto.getMontoInicial().setScale(2, REDONDEO))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        TurnoCaja guardado = turnoCajaRepository.save(turno);
        auditoriaService.registrar(cajero, AccionAuditoria.APERTURA_CAJA, "TurnoCaja", guardado.getId(),
                null, "inicial=" + guardado.getMontoInicial(), "Apertura de caja");
        return toDto(guardado);
    }

    @Transactional
    public TurnoCajaDTO cerrar(Long turnoId, CierreCajaDTO dto) {
        if (dto.getMontoFisico() == null) {
            throw new ReglaNegocioException("ARQUEO_OBLIGATORIO", "No se puede cerrar caja sin el conteo físico del efectivo");
        }

        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado: " + turnoId));
        if (turno.getEstado() != EstadoTurnoCaja.ABIERTO) {
            throw new ReglaNegocioException("CAJA_YA_CERRADA", "El turno ya está cerrado");
        }

        Usuario quienCierra = accesoService.exigirPermiso(Permiso.CAJA_OPERAR);
        if (!turno.getUsuarioId().equals(quienCierra.getId()) && quienCierra.getRol() != Rol.ADMIN) {
            throw new ReglaNegocioException("CAJA_AJENA", "Solo el cajero del turno o un administrador puede cerrar la caja");
        }

        BigDecimal ventasEfectivo = ventaRepository.findByTurnoCajaIdAndEstado(turno.getId(), EstadoVenta.COMPLETADA)
                .stream()
                .filter(venta -> venta.getFormaPago() == FormaPago.EFECTIVO)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, REDONDEO);

        BigDecimal esperado = turno.getMontoInicial().add(ventasEfectivo).setScale(2, REDONDEO);
        BigDecimal fisico = dto.getMontoFisico().setScale(2, REDONDEO);
        BigDecimal diferencia = fisico.subtract(esperado).setScale(2, REDONDEO);

        turno.setFechaCierre(LocalDateTime.now(clock));
        turno.setVentasEfectivo(ventasEfectivo);
        turno.setMontoEsperado(esperado);
        turno.setMontoFisico(fisico);
        turno.setDiferencia(diferencia);
        turno.setObservacionArqueo(dto.getObservacionArqueo());
        turno.setEstado(EstadoTurnoCaja.CERRADO);

        TurnoCaja cerrado = turnoCajaRepository.save(turno);
        auditoriaService.registrar(quienCierra, AccionAuditoria.CIERRE_CAJA, "TurnoCaja", cerrado.getId(),
                "esperado=" + esperado, "fisico=" + fisico + ", diferencia=" + diferencia,
                resultadoArqueo(diferencia));
        return toDto(cerrado);
    }

    @Transactional(readOnly = true)
    public TurnoCaja exigirTurnoAbierto(Long usuarioId) {
        return turnoCajaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoTurnoCaja.ABIERTO)
                .orElseThrow(() -> new ReglaNegocioException(
                        "CAJA_CERRADA",
                        "No hay un turno de caja abierto. Debe abrir caja antes de vender"
                ));
    }

    @Transactional(readOnly = true)
    public EstadoCajaDTO estado() {
        Usuario operador = autorizacionService.operadorActual();
        return turnoCajaRepository.findByUsuarioIdAndEstado(operador.getId(), EstadoTurnoCaja.ABIERTO)
                .map(turno -> EstadoCajaDTO.builder().abierta(true).turno(toDto(turno)).build())
                .orElseGet(() -> EstadoCajaDTO.builder().abierta(false).turno(null).build());
    }

    @Transactional(readOnly = true)
    public TurnoCajaDTO abierta() {
        return abiertaDe(autorizacionService.operadorActual().getId());
    }

    @Transactional(readOnly = true)
    public TurnoCajaDTO abiertaDe(Long usuarioId) {
        return turnoCajaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoTurnoCaja.ABIERTO)
                .map(this::toDto)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay turno de caja abierto para el usuario " + usuarioId));
    }

    @Transactional(readOnly = true)
    public PaginaDTO<TurnoCajaDTO> historial(int pagina, int tamano) {
        return turnosDe(autorizacionService.operadorActual().getId(), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<TurnoCajaDTO> historial() {
        return historial(0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<TurnoCajaDTO> historialDe(Long usuarioId, int pagina, int tamano) {
        autorizacionService.exigirRol(Rol.ADMIN);
        return turnosDe(usuarioId, pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<TurnoCajaDTO> historialDe(Long usuarioId) {
        return historialDe(usuarioId, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public List<TurnoCajaDTO> turnosEnPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return turnoCajaRepository.findByFechaAperturaBetweenOrderByFechaAperturaDesc(inicio, fin).stream()
                .map(this::toDto)
                .toList();
    }

    private PaginaDTO<TurnoCajaDTO> turnosDe(Long usuarioId, int pagina, int tamano) {
        return PaginaDTO.de(turnoCajaRepository.findByUsuarioIdOrderByFechaAperturaDesc(
                usuarioId, PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    private List<TurnoCajaDTO> turnosDe(Long usuarioId) {
        return turnosDe(usuarioId, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    private TurnoCajaDTO toDto(TurnoCaja turno) {
        List<Venta> ventas = ventaRepository.findByTurnoCajaIdAndEstado(turno.getId(), EstadoVenta.COMPLETADA);
        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, REDONDEO);
        BigDecimal ventasEfectivo = turno.getVentasEfectivo() != null
                ? turno.getVentasEfectivo()
                : ventas.stream()
                .filter(venta -> venta.getFormaPago() == FormaPago.EFECTIVO)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, REDONDEO);
        BigDecimal esperado = turno.getMontoEsperado() != null
                ? turno.getMontoEsperado()
                : turno.getMontoInicial().add(ventasEfectivo).setScale(2, REDONDEO);

        return TurnoCajaDTO.builder()
                .id(turno.getId())
                .usuarioId(turno.getUsuarioId())
                .usuarioNombre(nombreUsuario(turno.getUsuarioId()))
                .fechaApertura(turno.getFechaApertura())
                .montoInicial(turno.getMontoInicial())
                .fechaCierre(turno.getFechaCierre())
                .ventasEfectivo(ventasEfectivo)
                .montoEsperado(esperado)
                .montoFisico(turno.getMontoFisico())
                .diferencia(turno.getDiferencia())
                .resultadoArqueo(resultadoArqueo(turno.getDiferencia()))
                .observacionArqueo(turno.getObservacionArqueo())
                .estado(turno.getEstado())
                .cantidadVentas(ventas.size())
                .totalVentas(totalVentas)
                .build();
    }

    private String nombreUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombreCompleto)
                .orElse(null);
    }

    static String resultadoArqueo(BigDecimal diferencia) {
        if (diferencia == null) {
            return "PENDIENTE";
        }
        int cmp = diferencia.compareTo(BigDecimal.ZERO);
        if (cmp < 0) {
            return "FALTANTE";
        }
        if (cmp > 0) {
            return "SOBRANTE";
        }
        return "CUADRADO";
    }
}
