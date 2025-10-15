package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    // =====> MÉTODOS CORREGIDOS

    // ELIMINA O COMENTA ESTA LÍNEA, ES LA CAUSA DEL ERROR #3
    // Optional<Prestamo> findByClienteIdClienteAndEstado(Integer idCliente, String estado);

    // Este método busca si existe un préstamo con un estado específico.
    boolean existsByCliente_IdClienteAndEstado(Integer idCliente, String estado);

    // Este es el método correcto para buscar una LISTA de préstamos por cliente y estado.
    List<Prestamo> findByCliente_IdClienteAndEstado(Integer idCliente, String estado);

    // Este método es para buscar todos los préstamos de un cliente.
    List<Prestamo> findByCliente_IdCliente(Integer idCliente);

    // Esta consulta personalizada está bien.
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Prestamo p WHERE p.cliente.dniCliente = :dni AND p.estado = 'activo'")
    boolean existePrestamoActivoPorDni(@Param("dni") String dni);
}