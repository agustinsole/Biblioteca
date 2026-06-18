package com.example.libros.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paises")
public class Pais {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "nom_pais")
    private String nomPais;

    @NotBlank
    @Column(name = "bandera")
    private String bandera;

    @OneToMany(mappedBy = "pais")
    private List<Autor> autores = new ArrayList<>();

    @OneToMany(mappedBy = "pais")
    private List<Premio> premios = new ArrayList<>();

    public Pais() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomPais() {
        return nomPais;
    }

    public void setNomPais(String nomPais) {
        this.nomPais = nomPais;
    }

    public String getBandera() {
        return bandera;
    }

    public void setBandera(String bandera) {
        this.bandera = bandera;
    }

    public List<Autor> getAutores() {
        return autores;
    }

    public void setAutores(List<Autor> autores) {
        this.autores = autores;
    }

    public List<Premio> getPremios() {
        return premios;
    }

    public void setPremios(List<Premio> premios) {
        this.premios = premios;
    }
}
