package com.licoreria.pos.repository;

import com.licoreria.pos.model.HorarioVentaLicor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface HorarioVentaLicorRepository extends JpaRepository<HorarioVentaLicor, Long> {

    Optional<HorarioVentaLicor> findByDiaSemanaAndActivoTrue(DayOfWeek diaSemana);

    Optional<HorarioVentaLicor> findByDiaSemana(DayOfWeek diaSemana);
}
