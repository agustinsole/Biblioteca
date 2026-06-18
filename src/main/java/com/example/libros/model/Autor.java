package com.example.libros.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "autores")
public class Autor {

    @Id
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "nom_autor")
    private String nomAutor;

    @Column(name = "apellido1")
    private String apellido1;

    @Column(name = "apellido2")
    private String apellido2;

    @Column(name = "f_nac")
    private java.sql.Date fechaNacimiento;

    @Column(name = "f_def")
    private java.sql.Date fechaDefuncion;

    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;

    @Column(name = "foto")
    private String foto;

    @Column(name = "localidad")
    private String localidad;

    @Column(name = "biografia")
    private String biografia;

    @OneToMany(mappedBy = "autor")
    private List<AutorLibro> autorLibros = new ArrayList<>();

    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutorPremio> autorPremios = new ArrayList<>();

    public Autor() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomAutor() {
        return nomAutor;
    }

    public void setNomAutor(String nomAutor) {
        this.nomAutor = nomAutor;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public java.sql.Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(java.sql.Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public java.sql.Date getFechaDefuncion() {
        return fechaDefuncion;
    }

    public void setFechaDefuncion(java.sql.Date fechaDefuncion) {
        this.fechaDefuncion = fechaDefuncion;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public List<AutorLibro> getAutorLibros() {
        return autorLibros;
    }

    public void setAutorLibros(List<AutorLibro> autorLibros) {
        this.autorLibros = autorLibros;
    }

    public List<AutorPremio> getAutorPremios() {
        return autorPremios;
    }

    public void setAutorPremios(List<AutorPremio> autorPremios) {
        this.autorPremios = autorPremios;
    }

    // Devuelve los libros asociados ordenados por campo 'ano' ascendente
    public List<Libro> getLibrosOrdenadosPorAnoAsc() {
        return this.autorLibros.stream()
                .map(AutorLibro::getLibro)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(l -> l.getAno() == null ? Integer.MAX_VALUE : l.getAno()))
                .collect(Collectors.toList());
    }

    // Devuelve los AutorPremio ordenados por 'anio' ascendente
    public List<AutorPremio> getPremiosOrdenadosPorAnioAsc() {
        return this.autorPremios.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ap -> ap.getAnio() == null ? Integer.MAX_VALUE : ap.getAnio()))
                .collect(Collectors.toList());
    }
}
