package com.example.libros.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Size(max = 30)
    @Column(name = "logotipo", length = 30)
    private String logotipo;

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

    public String getLogotipo() {
        return logotipo;
    }

    public void setLogotipo(String logotipo) {
        this.logotipo = logotipo;
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
