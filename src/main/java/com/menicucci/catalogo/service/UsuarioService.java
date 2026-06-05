package com.menicucci.catalogo.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menicucci.catalogo.model.Usuario;
import com.menicucci.catalogo.repository.UsuarioRepository;
import com.menicucci.catalogo.utils.ValidationUtils;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final FileUploadService fileUploadService;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    public UsuarioService(UsuarioRepository usuarioRepository, FileUploadService fileUploadService,
            PasswordEncoder passwordEncoder, NamedParameterJdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.fileUploadService = fileUploadService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Usuario> buscarPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);

        var params = new MapSqlParameterSource().addValue(USERNAME_FIELD, username);
        List<Usuario> usuarios = jdbcTemplate.query(
                "SELECT id, username, password, role, foto_url FROM usuarios WHERE username = :username",
                params,
                this::mapUsuario);
        return usuarios.stream().findFirst();
    }

    @Transactional
    public Usuario criarUsuario(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, PASSWORD_FIELD);

        if (buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe" + username);
        }

        String hash = passwordEncoder.encode(password);

        Usuario novo = new Usuario();
        novo.setId(UUID.randomUUID());
        novo.setUsername(username);
        novo.setPassword(hash);
        novo.setNew(true);
        return salvarNovoUsuario(novo);

    }

    public boolean autenticar(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, PASSWORD_FIELD);

        Optional<Usuario> opt = buscarPorUsername(username);
        if (opt.isEmpty()) {
            return false;
        }

        String stored = opt.get().getPassword();
        return passwordEncoder.matches(password, stored);
    }

    @Transactional
    public Usuario atualizarSenha(UUID id, String novaSenha) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        ValidationUtils.validarCampoStringObrigatorio(novaSenha, "novaSenha");

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        String hash = passwordEncoder.encode(novaSenha);

        var params = new MapSqlParameterSource()
                .addValue("id", id.toString())
                .addValue(PASSWORD_FIELD, hash);

        jdbcTemplate.update(
                "UPDATE usuarios SET password = :password WHERE id = :id",
                params);

        // reload and return using explicit query to avoid possible repository mapping
        // issues
        var out = jdbcTemplate.query(
                "SELECT id, username, password, role, foto_url FROM usuarios WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id.toString()),
                this::mapUsuario);

        return out.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado apos update."));
    }

    @Transactional
    public void deletar(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        var opt = usuarioRepository.findById(id);
        if (opt.isPresent()) {
            var u = opt.get();
        }
        usuarioRepository.deleteById(id);
    }

    public Usuario salvarNovoUsuario(Usuario novo) {
        ValidationUtils.validarCampoObrigatorio(novo, "usuario");
        ValidationUtils.validarCampoObrigatorio(novo.getId(), "id");
        ValidationUtils.validarCampoStringObrigatorio(novo.getUsername(), USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(novo.getPassword(), PASSWORD_FIELD);

        UUID id = Objects.requireNonNull(novo.getId(), "id");
        String username = novo.getUsername();
        String password = novo.getPassword();

        if (buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe" + username);
        }

        var params = new MapSqlParameterSource()
                .addValue("id", id.toString())
                .addValue(USERNAME_FIELD, username)
                .addValue(PASSWORD_FIELD, password);
        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, password) VALUES (:id, :username, :password)",
                params);

        novo.setNew(false);
        return novo;
    }

    private Usuario mapUsuario(ResultSet rs, int rowNum) throws SQLException {
        Usuario usuario = new Usuario();
        String id = rs.getString("id");
        if (id != null) {
            usuario.setId(UUID.fromString(id));
        }
        usuario.setUsername(rs.getString(USERNAME_FIELD));
        usuario.setPassword(rs.getString(PASSWORD_FIELD));

        usuario.setNew(false);
        return usuario;
    }

}
