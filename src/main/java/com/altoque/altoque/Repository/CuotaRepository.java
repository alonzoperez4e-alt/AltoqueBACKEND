package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Integer> {
    // Buscar todas las cuotas de un préstamo ordenadas por vencimiento
    List<Cuota> findByPrestamo_IdPrestamoOrderByNumeroCuotaAsc(Integer idPrestamo);

    // SOLUCIÓN FINAL: Usamos @Query para evitar la ambigüedad en la generación automática de nombres.
    // "c.prestamo.id" asume que en tu entidad Prestamo el campo @Id se llama "id".
    @Query("SELECT c FROM Cuota c WHERE c.prestamo.idPrestamo = :prestamoId")
    List<Cuota> buscarPorPrestamoId(@Param("prestamoId") Integer prestamoId);
}