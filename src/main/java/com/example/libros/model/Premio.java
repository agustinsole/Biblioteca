package com.example.libros.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "premios")
public class Premio {

    @Id
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "nom_premio")
    private String nomPremio;

    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;

    @NotBlank
    @Column(name = "tipo")
    private String tipo;

    @OneToMany(mappedBy = "premio")
    private List<AutorPremio> autorPremios = new ArrayList<>();

    @OneToMany(mappedBy = "premio")
    private List<LibroPremio> libroPremios = new ArrayList<>();

    public Premio() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomPremio() {
        return nomPremio;
    }

    public void setNomPremio(String nomPremio) {
        this.nomPremio = nomPremio;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<AutorPremio> getAutorPremios() {
        return autorPremios;
    }

    public void setAutorPremios(List<AutorPremio> autorPremios) {
        this.autorPremios = autorPremios;
    }

    public List<LibroPremio> getLibroPremios() {
        return libroPremios;
    }

    public void setLibroPremios(List<LibroPremio> libroPremios) {
        this.libroPremios = libroPremios;
    }
}
