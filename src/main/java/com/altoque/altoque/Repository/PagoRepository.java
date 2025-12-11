package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    // Aquí podrás agregar métodos personalizados si necesitas buscar pagos por fecha o usuario
    List<Pago> findByCaja_IdCajaOrderByFechaPagoDesc(Integer idCaja);
    // Método para validar idempotencia (evitar duplicados) de pagos online
    Optional<Pago> findByOrdenExterna(String ordenExterna);
}