package com.licoreria.pos.util;

import com.licoreria.pos.dto.PaginaDTO;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PaginacionUtil {

    public static final int TAMANO_DEFAULT = 20;
    public static final int TAMANO_MAX = 200;

    private PaginacionUtil() {
    }

    public static Pageable pageable(int pagina, int tamano) {
        return pageable(pagina, tamano, null);
    }

    public static Pageable pageable(int pagina, int tamano, Sort sort) {
        int size = tamano <= 0 ? TAMANO_DEFAULT : Math.min(tamano, TAMANO_MAX);
        int page = Math.max(0, pagina);
        return sort == null ? PageRequest.of(page, size) : PageRequest.of(page, size, sort);
    }

    public static <T> PaginaDTO<T> deLista(List<T> items, int pagina, int tamano) {
        Pageable pageable = pageable(pagina, tamano);
        List<T> fuente = items == null ? List.of() : items;
        int inicio = (int) Math.min(pageable.getOffset(), fuente.size());
        int fin = Math.min(inicio + pageable.getPageSize(), fuente.size());
        return PaginaDTO.de(new PageImpl<>(fuente.subList(inicio, fin), pageable, fuente.size()));
    }
}
