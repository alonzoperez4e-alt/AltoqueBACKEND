package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cuota")
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuota")
    private Integer idCuota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prestamo", nullable = false)
    private Prestamo prestamo;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "monto_programado", nullable = false)
    private Double montoProgramado;

    @Column(name = "monto_pagado")
    private Double montoPagado = 0.0;

    @Column(name = "interes_mora")
    private Double interesMora = 0.0;

    @Column(name = "estado", length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE, PAGADO, PARCIAL, VENCIDO

    public Cuota() {}

    // Constructor útil para la generación
    public Cuota(Prestamo prestamo, Integer numeroCuota, LocalDate fechaVencimiento, Double montoProgramado) {
        this.prestamo = prestamo;
        this.numeroCuota = numeroCuota;
        this.fechaVencimiento = fechaVencimiento;
        this.montoProgramado = montoProgramado;
        this.estado = "PENDIENTE";
    }

    // Getters y Setters
    public Integer getIdCuota() { return idCuota; }
    public void setIdCuota(Integer idCuota) { this.idCuota = idCuota; }
    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }
    public Integer getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Integer numeroCuota) { this.numeroCuota = numeroCuota; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Double getMontoProgramado() { return montoProgramado; }
    public void setMontoProgramado(Double montoProgramado) { this.montoProgramado = montoProgramado; }
    public Double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }
    public Double getInteresMora() { return interesMora; }
    public void setInteresMora(Double interesMora) { this.interesMora = interesMora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // Método helper para saldo pendiente real (útil para lógica de negocio)
    public Double getSaldoPendiente() {
        return (montoProgramado + interesMora) - montoPagado;
    }
}