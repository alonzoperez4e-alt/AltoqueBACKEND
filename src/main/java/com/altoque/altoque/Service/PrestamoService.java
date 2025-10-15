package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.PrestamoDto;
import com.altoque.altoque.Entity.Prestamo;
import java.util.List;
import java.util.Optional;

public interface PrestamoService {
    List<Prestamo> listarPrestamos();
    Optional<Prestamo> obtenerPorId(Integer id);
    Prestamo registrarPrestamo(PrestamoDto prestamoDto);
    Prestamo actualizarPrestamo(Prestamo prestamo);
    void eliminarPrestamo(Integer id);

    // =====> NUEVOS MÉTODOS
    List<Prestamo> buscarPorClienteId(Integer idCliente);
    List<Prestamo> buscarPorClienteDni(String dniCliente);
    List<Prestamo> buscarPorClienteDniYEstado(String dniCliente, String estado);
}