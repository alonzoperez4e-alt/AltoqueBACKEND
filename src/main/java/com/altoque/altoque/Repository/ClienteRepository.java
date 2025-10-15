package com.altoque.altoque.Repository;

import com.altoque.altoque.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findClienteByNombreCliente(String nombreCliente);
    boolean existsByNombreCliente(String nombreCliente);

    Optional<Cliente> findClientesByDniCliente(String dniCliente);

    boolean existsByDniCliente(String dniCliente);

    // Buscar cliente por DNI
    Optional<Cliente> findByDniCliente(String dniCliente);

}
