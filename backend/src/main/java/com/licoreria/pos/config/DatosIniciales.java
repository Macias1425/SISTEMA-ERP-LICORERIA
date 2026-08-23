package com.licoreria.pos.config;

import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.ClienteRepository;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final HorarioVentaLicorRepository horarioRepository;
    private final ClienteRepository clienteRepository;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nombreCompleto("Administrador")
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: admin / admin123");
        }
        if (usuarioRepository.findByUsername("cajero").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("cajero")
                    .password(passwordEncoder.encode("cajero123"))
                    .nombreCompleto("Cajero de turno")
                    .rol(Rol.CAJERO)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: cajero / cajero123");
        }
        if (usuarioRepository.findByUsername("almacenista").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("almacenista")
                    .password(passwordEncoder.encode("almacen123"))
                    .nombreCompleto("Encargado de almacén")
                    .rol(Rol.ALMACENISTA)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: almacenista / almacen123");
        }

        if (horarioRepository.count() == 0) {
            for (DayOfWeek dia : DayOfWeek.values()) {
                horarioRepository.save(HorarioVentaLicor.builder()
                        .diaSemana(dia)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(22, 0))
                        .activo(true)
                        .build());
            }
            log.info("Horario de venta de licor: 08:00 a 22:00 todos los días");
        }

        if (clienteRepository.count() == 0) {
            clienteRepository.save(Cliente.builder()
                    .nombre("Consumidor final")
                    .tipoCliente(TipoCliente.DETAL)
                    .activo(true)
                    .build());
            log.info("Cliente inicial creado: Consumidor final (DETAL)");
        }
    }
}
