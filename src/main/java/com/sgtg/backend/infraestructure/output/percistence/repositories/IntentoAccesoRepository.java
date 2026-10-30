package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.infraestructure.output.percistence.entities.IntentoAccesoEntity;

public interface IntentoAccesoRepository extends JpaRepository<IntentoAccesoEntity, String> {

    @Modifying
    @Query("DELETE FROM IntentoAccesoEntity i WHERE i.ultimoFallo < :antesDe"
            + " AND (i.bloqueadoHasta IS NULL OR i.bloqueadoHasta < :antesDe)")
    int depurar(@Param("antesDe") LocalDateTime antesDe);
}
