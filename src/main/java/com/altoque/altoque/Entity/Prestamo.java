package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "prestamos")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prestamo")
    private Integer idPrestamo;

    @OneToOne
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "monto", nullable = false)
    private Double monto;

    @Column(name = "tasa_interes_anual", nullable = false)
    private Double tasaInteresAnual;

    @Column(name = "numero_cuotas", nullable = false)
    private Integer numeroCuotas;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDate fechaPrestamo;

    @Column(name = "estado", length = 20)
    private String estado = "activo";

    @Column(name = "declaracion_impresa")
    private Boolean declaracionImpresa = false;

    @Column(name = "declaracion_pep_impresa")
    private Boolean declaracionPepImpresa = false;

    // ----- CONSTRUCTORES -----
    public Prestamo() {
    }

    // ----- GETTERS Y SETTERS -----
    public Integer getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Integer idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getTasaInteresAnual() {
        return tasaInteresAnual;
    }

    public void setTasaInteresAnual(Double tasaInteresAnual) {
        this.tasaInteresAnual = tasaInteresAnual;
    }

    public Integer getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(Integer numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getDeclaracionImpresa() {
        return declaracionImpresa;
    }

    public void setDeclaracionImpresa(Boolean declaracionImpresa) {
        this.declaracionImpresa = declaracionImpresa;
    }

    public Boolean getDeclaracionPepImpresa() {
        return declaracionPepImpresa;
    }

    public void setDeclaracionPepImpresa(Boolean declaracionPepImpresa) {
        this.declaracionPepImpresa = declaracionPepImpresa;
    }
}