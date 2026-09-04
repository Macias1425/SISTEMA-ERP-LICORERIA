package com.licoreria.pos.dto;

import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.Permiso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String username;
    private String nombreCompleto;
    private Rol rol;
    private Boolean activo;
    private Boolean debeCambiarPassword;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime passwordActualizadaEn;
    private Boolean esSesionActual;
    private Boolean puedeDesactivar;
    private Boolean puedeCambiarRol;
    private Boolean turnoCajaAbierto;
    private Boolean horarioAccesoHabilitado;
    @Builder.Default
    private List<HorarioAccesoDTO> horarios = new ArrayList<>();
    private Boolean accesoPermitidoAhora;
    @Builder.Default
    private List<Permiso> permisosRol = new ArrayList<>();
    @Builder.Default
    private List<Permiso> permisosAdicionales = new ArrayList<>();
    @Builder.Default
    private List<Permiso> permisosEfectivos = new ArrayList<>();
}
