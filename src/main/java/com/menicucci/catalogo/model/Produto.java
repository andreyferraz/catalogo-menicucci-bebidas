package com.menicucci.catalogo.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("produtos")
public class Produto implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("nome")
    private String nome;

    @Column("descricao")
    private String descricao;

    @Column("categoria")
    private String categoria;

    @Column("preco")
    private BigDecimal preco;

    @Transient
    private boolean isNew = false;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

}
