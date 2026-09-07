package com.licoreria.pos.service;

import com.licoreria.pos.dto.ClienteDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ClienteRepository;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public PaginaDTO<ClienteDTO> listar(String busqueda, Boolean activo, int pagina, int tamano) {
        String termino = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        return PaginaDTO.de(clienteRepository.buscarPaginado(
                termino, activo, PaginacionUtil.pageable(pagina, tamano)
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {
        return listar(null, true, 0, PaginacionUtil.TAMANO_MAX).getContenido();
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> deudores() {
        return clienteRepository.findAll().stream()
                .filter(c -> c.getSaldoCredito() != null && c.getSaldoCredito().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getSaldoCredito().compareTo(a.getSaldoCredito()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public ClienteDTO crear(ClienteDTO dto) {
        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .ruc(dto.getRuc())
                .telefono(dto.getTelefono())
                .direccion(dto.getDireccion())
                .tipoCliente(dto.getTipoCliente() == null ? TipoCliente.DETAL : dto.getTipoCliente())
                .activo(dto.getActivo() == null || dto.getActivo())
                .limiteCredito(dinero(dto.getLimiteCredito()))
                .saldoCredito(BigDecimal.ZERO.setScale(2, REDONDEO))
                .build();
        return toDto(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteDTO actualizar(Long id, ClienteDTO dto) {
        Cliente cliente = buscar(id);
        cliente.setNombre(dto.getNombre());
        cliente.setRuc(dto.getRuc());
        cliente.setTelefono(dto.getTelefono());
        cliente.setDireccion(dto.getDireccion());
        if (dto.getTipoCliente() != null) {
            cliente.setTipoCliente(dto.getTipoCliente());
        }
        if (dto.getActivo() != null) {
            cliente.setActivo(dto.getActivo());
        }
        if (dto.getLimiteCredito() != null) {
            BigDecimal limite = dinero(dto.getLimiteCredito());
            if (cliente.getSaldoCredito() != null && cliente.getSaldoCredito().compareTo(limite) > 0) {
                throw new com.licoreria.pos.exception.ReglaNegocioException(
                        "LIMITE_CREDITO",
                        "El límite no puede ser menor al saldo pendiente (" + cliente.getSaldoCredito() + ")"
                );
            }
            cliente.setLimiteCredito(limite);
        }
        return toDto(clienteRepository.save(cliente));
    }

    @Transactional
    public void cargarCredito(Long clienteId, BigDecimal monto) {
        Cliente cliente = buscar(clienteId);
        BigDecimal cargo = dinero(monto);
        BigDecimal limite = dinero(cliente.getLimiteCredito());
        BigDecimal saldo = dinero(cliente.getSaldoCredito());
        BigDecimal nuevo = saldo.add(cargo);
        if (limite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.licoreria.pos.exception.ReglaNegocioException(
                    "SIN_CREDITO",
                    "El cliente no tiene límite de crédito configurado"
            );
        }
        if (nuevo.compareTo(limite) > 0) {
            throw new com.licoreria.pos.exception.ReglaNegocioException(
                    "CREDITO_EXCEDIDO",
                    "Crédito insuficiente. Disponible: C$ " + limite.subtract(saldo).toPlainString()
            );
        }
        cliente.setSaldoCredito(nuevo);
        clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteDTO abonar(Long clienteId, BigDecimal monto) {
        Cliente cliente = buscar(clienteId);
        BigDecimal abono = dinero(monto);
        if (abono.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.licoreria.pos.exception.ReglaNegocioException(
                    "ABONO_INVALIDO",
                    "El abono debe ser mayor a cero"
            );
        }
        BigDecimal saldo = dinero(cliente.getSaldoCredito());
        if (abono.compareTo(saldo) > 0) {
            throw new com.licoreria.pos.exception.ReglaNegocioException(
                    "ABONO_EXCEDE_SALDO",
                    "El abono no puede superar el saldo (C$ " + saldo.toPlainString() + ")"
            );
        }
        cliente.setSaldoCredito(saldo.subtract(abono));
        return toDto(clienteRepository.save(cliente));
    }

    @Transactional
    public void liberarCredito(Long clienteId, BigDecimal monto) {
        if (clienteId == null || monto == null) {
            return;
        }
        Cliente cliente = buscar(clienteId);
        BigDecimal liberar = dinero(monto);
        BigDecimal saldo = dinero(cliente.getSaldoCredito());
        BigDecimal nuevo = saldo.subtract(liberar);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0) {
            nuevo = BigDecimal.ZERO.setScale(2, REDONDEO);
        }
        cliente.setSaldoCredito(nuevo);
        clienteRepository.save(cliente);
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private static BigDecimal dinero(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, REDONDEO);
        }
        return valor.setScale(2, REDONDEO);
    }

    private ClienteDTO toDto(Cliente cliente) {
        return ClienteDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .ruc(cliente.getRuc())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .tipoCliente(cliente.getTipoCliente())
                .activo(cliente.getActivo())
                .limiteCredito(dinero(cliente.getLimiteCredito()))
                .saldoCredito(dinero(cliente.getSaldoCredito()))
                .build();
    }
}
