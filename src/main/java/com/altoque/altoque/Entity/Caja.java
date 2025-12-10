package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "caja")
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCaja;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "saldo_inicial")
    private Double saldoInicial;

    @Column(name = "total_efectivo_sistema")
    private Double totalEfectivoSistema = 0.0;

    @Column(name = "total_digital_sistema")
    private Double totalDigitalSistema = 0.0;

    @Column(name = "total_ajuste_redondeo")
    private Double totalAjusteRedondeo = 0.0;

    @Column(name = "saldo_final_esperado")
    private Double saldoFinalEsperado = 0.0;

    @Column(name = "saldo_final_real")
    private Double saldoFinalReal;

    @Column(name = "estado")
    private String estado; // 'ABIERTA', 'CERRADA'

    @PrePersist
    protected void onCreate() {
        if (fechaApertura == null) fechaApertura = LocalDateTime.now();
        if (estado == null) estado = "ABIERTA";
    }
}