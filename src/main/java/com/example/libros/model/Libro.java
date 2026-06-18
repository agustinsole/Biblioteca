package com.example.libros.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "libros")
public class Libro {

    @Id
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "nom_libro")
    private String nomLibro;

    @Column(name = "ano")
    private Integer ano;

    @NotBlank
    @Column(name = "portada")
    private String portada;

    @NotBlank
    @Column(name = "nom_archivo")
    private String nomArchivo;

    @Column(name = "pelicula")
    private String pelicula;

    @Column(name = "comentario")
    private String comentario;

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie;

    @Column(name = "indice_serie")
    private Integer indiceSerie;

    @OneToMany(mappedBy = "libro")
    private List<AutorLibro> autorLibros = new ArrayList<>();

    @OneToMany(mappedBy = "libro")
    private List<LibroPremio> libroPremios = new ArrayList<>();

    public Libro() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomLibro() {
        return nomLibro;
    }

    public void setNomLibro(String nomLibro) {
        this.nomLibro = nomLibro;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getPortada() {
        return portada;
    }

    public void setPortada(String portada) {
        this.portada = portada;
    }

    public String getNomArchivo() {
        return nomArchivo;
    }

    public void setNomArchivo(String nomArchivo) {
        this.nomArchivo = nomArchivo;
    }

    public String getPelicula() {
        return pelicula;
    }

    public void setPelicula(String pelicula) {
        this.pelicula = pelicula;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Serie getSerie() {
        return serie;
    }

    public void setSerie(Serie serie) {
        this.serie = serie;
    }

    public Integer getIndiceSerie() {
        return indiceSerie;
    }

    public void setIndiceSerie(Integer indiceSerie) {
        this.indiceSerie = indiceSerie;
    }

    public List<AutorLibro> getAutorLibros() {
        return autorLibros;
    }

    public void setAutorLibros(List<AutorLibro> autorLibros) {
        this.autorLibros = autorLibros;
    }

    public List<LibroPremio> getLibroPremios() {
        return libroPremios;
    }

    public void setLibroPremios(List<LibroPremio> libroPremios) {
        this.libroPremios = libroPremios;
    }
}
