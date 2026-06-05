package com.menicucci.catalogo.controller;

import java.util.UUID;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.menicucci.catalogo.model.Produto;
import com.menicucci.catalogo.model.Usuario;
import com.menicucci.catalogo.service.AppConfigService;
import com.menicucci.catalogo.service.ProdutoService;
import com.menicucci.catalogo.service.UsuarioService;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

	private static final String ADMIN_REDIRECT = "redirect:/admin";
	private static final String SUCCESS_MESSAGE = "successMessage";

	private final ProdutoService produtoService;
	private final UsuarioService usuarioService;
	private final AppConfigService appConfigService;

	public AdminPageController(ProdutoService produtoService, UsuarioService usuarioService,
			AppConfigService appConfigService) {
		this.produtoService = produtoService;
		this.usuarioService = usuarioService;
		this.appConfigService = appConfigService;
	}

	@GetMapping
	public String admin(Model model) {
		model.addAttribute("produtos", produtoService.listarOrdenados());
		model.addAttribute("produtoForm", new Produto());
		model.addAttribute("whatsappNumber", appConfigService.getWhatsappNumber());
		return "admin";
	}

	@PostMapping("/senha")
	public String alterarSenha(Principal principal, @RequestParam String novaSenha,
			RedirectAttributes redirectAttributes) {
		String username = principal.getName();
		UUID id = usuarioService.buscarPorUsername(username)
				.map(Usuario::getId)
				.orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));

		usuarioService.atualizarSenha(id, novaSenha);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Senha alterada com sucesso.");
		return ADMIN_REDIRECT;
	}

	@PostMapping("/produto/salvar")
	public String salvarProduto(@ModelAttribute Produto produto,
			@RequestParam(value = "imagem", required = false) MultipartFile imagem,
			RedirectAttributes redirectAttributes) {
		boolean creating = produto.getId() == null;
		produtoService.salvarProduto(produto, imagem);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
				creating ? "Produto criado com sucesso." : "Produto atualizado com sucesso.");
		return ADMIN_REDIRECT;
	}

	@PostMapping("/produto/excluir")
	public String excluirProduto(@RequestParam UUID id, RedirectAttributes redirectAttributes) {
		produtoService.deletar(id);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Produto excluído com sucesso.");
		return ADMIN_REDIRECT;
	}

	@PostMapping("/whatsapp")
	public String atualizarWhatsapp(@RequestParam String whatsappNumber,
			RedirectAttributes redirectAttributes) {
		appConfigService.salvarWhatsappNumber(whatsappNumber);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Número do WhatsApp atualizado com sucesso.");
		return ADMIN_REDIRECT;
	}
}
