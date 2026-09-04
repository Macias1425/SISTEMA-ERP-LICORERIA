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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

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
        return toDto(clienteRepository.save(cliente));
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
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
                .build();
    }
}
