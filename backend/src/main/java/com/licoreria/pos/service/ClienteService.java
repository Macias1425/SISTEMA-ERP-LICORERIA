package com.licoreria.pos.service;

import com.licoreria.pos.dto.ClienteDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {
        return clienteRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public ClienteDTO crear(ClienteDTO dto) {
        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .rtn(dto.getRtn())
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
        cliente.setRtn(dto.getRtn());
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
                .rtn(cliente.getRtn())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .tipoCliente(cliente.getTipoCliente())
                .activo(cliente.getActivo())
                .build();
    }
}
