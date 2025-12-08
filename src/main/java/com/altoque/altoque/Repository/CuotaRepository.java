package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Integer> {
    // Buscar todas las cuotas de un préstamo ordenadas por vencimiento
    List<Cuota> findByPrestamo_IdPrestamoOrderByNumeroCuotaAsc(Integer idPrestamo);
}