package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Caja;
import com.altoque.altoque.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Integer> {
    // Buscar si el usuario tiene una caja abierta
    Optional<Caja> findByUsuario_IdUsuarioAndEstado(Integer idUsuario, String estado);
    // Buscar la caja actualmente abierta (sin fecha de cierre) para un usuario
    Optional<Caja> findByUsuarioAndFechaCierreIsNull(Usuario usuario);
    // Listar movimientos (usaremos una query personalizada o JPA en el servicio)
    List<Caja> findByUsuario_IdUsuarioOrderByFechaAperturaDesc(Integer idUsuario);
}