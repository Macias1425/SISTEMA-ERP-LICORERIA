package com.licoreria.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaDTO<T> {

    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;

    public static <T> PaginaDTO<T> de(Page<T> page) {
        return PaginaDTO.<T>builder()
                .contenido(page.getContent())
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }

    public static <E, T> PaginaDTO<T> de(Page<E> page, List<T> contenido) {
        return PaginaDTO.<T>builder()
                .contenido(contenido)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
