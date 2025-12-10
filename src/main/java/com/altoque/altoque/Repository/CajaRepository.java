package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Integer> {
    // Buscar si el usuario tiene una caja abierta
    Optional<Caja> findByUsuario_IdUsuarioAndEstado(Integer idUsuario, String estado);

    // Listar movimientos (usaremos una query personalizada o JPA en el servicio)
    List<Caja> findByUsuario_IdUsuarioOrderByFechaAperturaDesc(Integer idUsuario);
}