package com.menicucci.catalogo.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.menicucci.catalogo.model.Usuario;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, UUID> {

}
