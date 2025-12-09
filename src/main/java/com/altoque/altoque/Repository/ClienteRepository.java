package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    boolean existsByDniCliente(String dniCliente);

    // Buscar cliente por DNI
    Optional<Cliente> findByDniCliente(String dniCliente);

    // Nuevos métodos para Persona Jurídica
    Optional<Cliente> findByRuc(String ruc);
    boolean existsByRuc(String ruc);

    // NUEVO: Búsqueda flexible para el autocompletado
    @Query("SELECT c FROM Cliente c WHERE " +
            "c.dniCliente LIKE %:query% OR " +
            "c.ruc LIKE %:query% OR " +
            "LOWER(c.nombreCliente) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.apellidoCliente) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Cliente> buscarClientes(@Param("query") String query);
}
