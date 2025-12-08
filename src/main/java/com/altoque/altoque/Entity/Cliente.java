package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    // --- Identificación ---
    @Column(name = "tipo_cliente", nullable = false, length = 20)
    private String tipoCliente = "NATURAL"; // "NATURAL" o "JURIDICA"

    // --- Campos Persona Natural ---
    @Column(name = "dni_cliente", unique = true, length = 8)
    private String dniCliente;

    @Column(name = "nombre_cliente", length = 50)
    private String nombreCliente;

    @Column(name = "apellido_cliente", length = 255)
    private String apellidoCliente;

    @Column(name = "fecha_nacimiento")
    private LocalDateTime fechaNacimiento;

    @Column(name = "es_pep")
    private Boolean esPep;

    // --- Campos Persona Jurídica ---
    @Column(name = "ruc", unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 255)
    private String razonSocial;

    @Column(name = "direccion_fiscal", length = 255)
    private String direccionFiscal;

    @Column(name = "fecha_constitucion")
    private LocalDateTime fechaConstitucion;

    @Column(name = "representante_legal_dni", length = 8)
    private String representanteLegalDni;

    @Column(name = "representante_legal_nombre", length = 255)
    private String representanteLegalNombre;

    // --- Campos Comunes de Contacto ---
    @Column(name = "correo_cliente", length = 150)
    private String correoCliente;

    @Column(name = "telefono_cliente", length = 9)
    private String telefonoCliente;

    @Column(name = "direccion_cliente")
    private String direccionCliente;

    public Cliente() {}

    // Getters y Setters
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public String getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(String tipoCliente) { this.tipoCliente = tipoCliente; }
    public String getDniCliente() { return dniCliente; }
    public void setDniCliente(String dniCliente) { this.dniCliente = dniCliente; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getApellidoCliente() { return apellidoCliente; }
    public void setApellidoCliente(String apellidoCliente) { this.apellidoCliente = apellidoCliente; }
    public LocalDateTime getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDateTime fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public Boolean getEsPep() { return esPep; }
    public void setEsPep(Boolean esPep) { this.esPep = esPep; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public void setDireccionFiscal(String direccionFiscal) { this.direccionFiscal = direccionFiscal; }
    public LocalDateTime getFechaConstitucion() { return fechaConstitucion; }
    public void setFechaConstitucion(LocalDateTime fechaConstitucion) { this.fechaConstitucion = fechaConstitucion; }
    public String getRepresentanteLegalDni() { return representanteLegalDni; }
    public void setRepresentanteLegalDni(String representanteLegalDni) { this.representanteLegalDni = representanteLegalDni; }
    public String getRepresentanteLegalNombre() { return representanteLegalNombre; }
    public void setRepresentanteLegalNombre(String representanteLegalNombre) { this.representanteLegalNombre = representanteLegalNombre; }
    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }
    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }
    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String direccionCliente) { this.direccionCliente = direccionCliente; }
}