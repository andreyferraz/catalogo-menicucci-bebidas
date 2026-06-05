package com.menicucci.catalogo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.menicucci.catalogo.service.AppConfigService;
import com.menicucci.catalogo.service.ProdutoService;

@Controller
public class PublicPageController {

    private final ProdutoService produtoService;
    private final AppConfigService appConfigService;

    public PublicPageController(ProdutoService produtoService, AppConfigService appConfigService) {
        this.produtoService = produtoService;
        this.appConfigService = appConfigService;
    }

    @GetMapping({"/", "/index", "/index.html", "/home"})
    public String home(Model model) {
        model.addAttribute("products", produtoService.listarOrdenados());
        model.addAttribute("whatsappNumber", appConfigService.getWhatsappNumber());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

}
