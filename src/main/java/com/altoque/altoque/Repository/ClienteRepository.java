package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    boolean existsByDniCliente(String dniCliente);

    // Buscar cliente por DNI
    Optional<Cliente> findByDniCliente(String dniCliente);

    // Nuevos métodos para Persona Jurídica
    Optional<Cliente> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
}
