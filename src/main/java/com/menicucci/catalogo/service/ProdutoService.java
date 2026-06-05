package com.menicucci.catalogo.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.menicucci.catalogo.model.Produto;
import com.menicucci.catalogo.repository.ProdutoRepository;
import com.menicucci.catalogo.utils.ValidationUtils;

@Service
public class ProdutoService {

    private static final String CAMPO_ID = "id";
    private static final String CAMPO_NOME = "nome";
    private static final String CAMPO_DESCRICAO = "descricao";
    private static final String CAMPO_PRECO = "preco";
    private static final String CAMPO_CATEGORIA = "categoria";
    private static final String CAMPO_IMAGEM_URL = "imagem_url";
    private static final String MENSAGEM_ID_NAO_NULO = "Produto id nao pode ser nulo";

    private final ProdutoRepository produtoRepository;
    private final FileUploadService fileUploadService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProdutoService(ProdutoRepository produtoRepository, FileUploadService fileUploadService,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.produtoRepository = produtoRepository;
        this.fileUploadService = fileUploadService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Optional<Produto> buscarPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID);
        return produtoRepository.findById(id);
    }

    public List<Produto> listarTodos() {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .toList();
    }

    public List<Produto> listarOrdenados() {
        return listarTodos().stream()
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Produto criarProduto(String nome, String descricao, BigDecimal preco, String categoria,
            MultipartFile imagem) {
        ValidationUtils.validarCampoStringObrigatorio(nome, CAMPO_NOME);
        ValidationUtils.validarCampoStringObrigatorio(categoria, CAMPO_CATEGORIA);

        Produto novo = new Produto();
        novo.setId(UUID.randomUUID());
        novo.setNome(nome);
        novo.setDescricao(descricao);
        novo.setPreco(preco);
        novo.setCategoria(categoria);
        novo.setNew(true);

        if (imagem != null && !imagem.isEmpty()) {
            novo.setImagemUrl(fileUploadService.salvarImagem(imagem));
        }

        namedParameterJdbcTemplate.update(
                "INSERT INTO produtos (id, nome, descricao, categoria, preco, imagem_url) " +
                        "VALUES (:id, :nome, :descricao, :categoria, :preco, :imagemUrl)",
                new MapSqlParameterSource()
                        .addValue(CAMPO_ID, Objects.requireNonNull(novo.getId(), MENSAGEM_ID_NAO_NULO).toString())
                        .addValue(CAMPO_NOME, novo.getNome())
                        .addValue(CAMPO_DESCRICAO, novo.getDescricao())
                        .addValue(CAMPO_CATEGORIA, novo.getCategoria())
                        .addValue(CAMPO_PRECO, novo.getPreco())
                        .addValue(CAMPO_IMAGEM_URL, novo.getImagemUrl()));

        return novo;
    }

    public Produto atualizar(UUID id, String nome, String descricao, String categoria, BigDecimal preco,
            MultipartFile imagem) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID);

        Produto existente = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));

        if (nome != null) {
            existente.setNome(nome);
        }
        if (descricao != null) {
            existente.setDescricao(descricao);
        }
        if (categoria != null) {
            existente.setCategoria(categoria);
        }
        if (preco != null) {
            existente.setPreco(preco);
        }

        if (imagem != null && !imagem.isEmpty()) {
            if (existente.getImagemUrl() != null && !existente.getImagemUrl().isEmpty()) {
                try {
                    fileUploadService.removerImagem(existente.getImagemUrl());
                } catch (Exception e) {
                    /* ignore */
                }
            }
            existente.setImagemUrl(fileUploadService.salvarImagem(imagem));
        }

        namedParameterJdbcTemplate.update(
                "UPDATE produtos SET nome = :nome, descricao = :descricao, categoria = :categoria, preco = :preco, " +
                        "imagem_url = :imagemUrl WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue(CAMPO_ID, Objects.requireNonNull(existente.getId(), MENSAGEM_ID_NAO_NULO).toString())
                        .addValue(CAMPO_NOME, existente.getNome())
                        .addValue(CAMPO_DESCRICAO, existente.getDescricao())
                        .addValue(CAMPO_CATEGORIA, existente.getCategoria())
                        .addValue(CAMPO_PRECO, existente.getPreco())
                        .addValue(CAMPO_IMAGEM_URL, existente.getImagemUrl()));

        return existente;
    }

    @Transactional
    public Produto salvarProduto(Produto produto, MultipartFile imagem) {
        ValidationUtils.validarCampoObrigatorio(produto, "produto");
        ValidationUtils.validarCampoStringObrigatorio(produto.getNome(), CAMPO_NOME);
        ValidationUtils.validarCampoStringObrigatorio(produto.getCategoria(), CAMPO_CATEGORIA);
        ValidationUtils.validarCampoObrigatorio(produto.getPreco(), CAMPO_PRECO);

        if (produto.getId() == null) {
            return criarProduto(produto.getNome(), produto.getDescricao(), produto.getPreco(), produto.getCategoria(),
                    imagem);
        }

        return atualizar(produto.getId(), produto.getNome(), produto.getDescricao(), produto.getCategoria(),
                produto.getPreco(), imagem);
    }

    @Transactional
    public void deletar(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID);
        var opt = produtoRepository.findById(id);
        if (opt.isPresent()) {
            var produto = opt.get();
            if (produto.getImagemUrl() != null && !produto.getImagemUrl().isEmpty()) {
                try {
                    fileUploadService.removerImagem(produto.getImagemUrl());
                } catch (Exception e) {
                    /* ignore */
                }
            }
        }
        namedParameterJdbcTemplate.update(
                "DELETE FROM produtos WHERE id = :id",
                new MapSqlParameterSource().addValue(CAMPO_ID, id.toString()));
    }
}
