package com.menicucci.catalogo.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppConfigService {

    private static final String WHATSAPP_KEY = "whatsapp_number";
    private static final String DEFAULT_WHATSAPP_NUMBER = "5528988079115";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AppConfigService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getWhatsappNumber() {
        var params = new MapSqlParameterSource().addValue("chave", WHATSAPP_KEY);
        var values = jdbcTemplate.query(
                "SELECT valor FROM configuracoes WHERE chave = :chave",
                params,
                (rs, rowNum) -> rs.getString("valor"));

        return values.stream().findFirst().orElse(DEFAULT_WHATSAPP_NUMBER);
    }

    @Transactional
    public void salvarWhatsappNumber(String whatsappNumber) {
        String normalized = normalize(whatsappNumber);
        var params = new MapSqlParameterSource()
                .addValue("chave", WHATSAPP_KEY)
                .addValue("valor", normalized);

        jdbcTemplate.update(
                "INSERT INTO configuracoes (chave, valor) VALUES (:chave, :valor) " +
                        "ON CONFLICT(chave) DO UPDATE SET valor = excluded.valor",
                params);
    }

    private String normalize(String whatsappNumber) {
        return whatsappNumber == null ? DEFAULT_WHATSAPP_NUMBER : whatsappNumber.replaceAll("\\D", "");
    }
}
