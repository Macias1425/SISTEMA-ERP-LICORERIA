package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.ListaPrecioDTO;
import com.licoreria.pos.exception.RecursoNoEncontradoException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.model.ListaPrecio;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ListaPrecioRepository;
import com.licoreria.pos.repository.ProductoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListaPrecioService {

    private final ListaPrecioRepository listaPrecioRepository;
    private final ProductoRepository productoRepository;
    private final PosProperties posProperties;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<ListaPrecioDTO> listarPorProducto(Long productoId) {
        return listaPrecioRepository.findByProductoId(productoId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ListaPrecioDTO guardar(ListaPrecioDTO dto) {
        productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId()));

        ListaPrecio entidad = listaPrecioRepository
                .findByProductoIdAndTipoCliente(dto.getProductoId(), dto.getTipoCliente())
                .orElse(ListaPrecio.builder()
                        .productoId(dto.getProductoId())
                        .tipoCliente(dto.getTipoCliente())
                        .build());
        entidad.setPrecioUmm(dto.getPrecioUmm());
        entidad.setVolumenMinimoUmm(dto.getVolumenMinimoUmm() == null ? 0 : dto.getVolumenMinimoUmm());
        ListaPrecio guardada = listaPrecioRepository.save(entidad);
        auditoriaService.registrar((Long) null, null, AccionAuditoria.CAMBIO_PRECIO, "ListaPrecio",
                guardada.getId(), null,
                dto.getTipoCliente() + "=" + dto.getPrecioUmm(),
                "Actualización de lista de precios");
        return toDto(guardada);
    }

    public TipoCliente resolverTipoAplicado(TipoCliente solicitado, int cantidadUmmTicket) {
        if (solicitado == null || solicitado == TipoCliente.DETAL) {
            return TipoCliente.DETAL;
        }
        int minimo = posProperties.getMayorista().getVolumenMinimoUmm();
        if (cantidadUmmTicket < minimo) {
            return TipoCliente.DETAL;
        }
        return solicitado;
    }

    public PrecioResuelto resolverPrecio(Producto producto, TipoCliente tipoAplicado, int cantidadUmmLinea) {
        BigDecimal precioDetal = listaPrecioRepository
                .findByProductoIdAndTipoCliente(producto.getId(), TipoCliente.DETAL)
                .map(ListaPrecio::getPrecioUmm)
                .orElse(producto.getPrecioVenta());

        if (tipoAplicado == TipoCliente.DETAL) {
            return new PrecioResuelto(precioDetal, TipoCliente.DETAL);
        }

        return listaPrecioRepository.findByProductoIdAndTipoCliente(producto.getId(), tipoAplicado)
                .map(item -> {
                    int minimoLinea = item.getVolumenMinimoUmm() == null ? 0 : item.getVolumenMinimoUmm();
                    if (minimoLinea > 0 && cantidadUmmLinea < minimoLinea) {
                        return new PrecioResuelto(precioDetal, TipoCliente.DETAL);
                    }
                    return new PrecioResuelto(item.getPrecioUmm(), tipoAplicado);
                })
                .orElse(new PrecioResuelto(precioDetal, TipoCliente.DETAL));
    }

    private ListaPrecioDTO toDto(ListaPrecio entidad) {
        return ListaPrecioDTO.builder()
                .id(entidad.getId())
                .productoId(entidad.getProductoId())
                .tipoCliente(entidad.getTipoCliente())
                .precioUmm(entidad.getPrecioUmm())
                .volumenMinimoUmm(entidad.getVolumenMinimoUmm())
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    public static class PrecioResuelto {
        private final BigDecimal precioUmm;
        private final TipoCliente tipoAplicadoLinea;
    }
}
