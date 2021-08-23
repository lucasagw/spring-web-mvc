package curso.springboot.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import curso.springboot.model.Pessoa;
import curso.springboot.model.Telefone;
import curso.springboot.repository.PessoaRepository;
import curso.springboot.repository.ProfissaoRepository;
import curso.springboot.repository.TelefoneRepository;

@Controller
public class PessoaController {

	@Autowired
	private PessoaRepository pessoaRepository;

	@Autowired
	private TelefoneRepository telefoneRepository;

	@Autowired
	private ProfissaoRepository profissaoRepository;

	@Autowired
	private ReportUtil reportUtil;

	@RequestMapping(value = "**/cadastropessoa", method = RequestMethod.GET)
	public ModelAndView inicio() {
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		andView.addObject("pessoa", new Pessoa()); // instanciando um objeto vazio de pessoa
													// para primeiro acesso ao form cadastro
		Iterable<Pessoa> pessoa = pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome")));
		andView.addObject("pessoas", pessoa);
		andView.addObject("profissoes", profissaoRepository.findAll());
		return andView;
	}

	@GetMapping("/pessoaspag")
	public ModelAndView carregaPessoaPorPagina(@PageableDefault(size = 5) Pageable pageable, ModelAndView model,
			@RequestParam("nomePesquisa") String nome) {

		Page<Pessoa> pagePessoa = pessoaRepository.findPessoaByNamePage(nome, pageable);

		model.addObject("pessoas", pagePessoa);
		model.addObject("nomePesquisa", nome);
		model.addObject("pessoa", new Pessoa());
		model.setViewName("cadastro/cadastropessoa");

		return model;

	}

	// ** faz ignorar qualquer coisa que tenha
	@RequestMapping(value = "**/salvarpessoa", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	public ModelAndView salvar(@Valid Pessoa pessoa, BindingResult bindingResult, final MultipartFile file)
			throws IOException {
//        System.out.println(file.getContentType());
//        System.out.println(file.getName());
//        System.out.println(file.getOriginalFilename());

		// se existir, carrega a lista de telefones que pertence a pessoa
		pessoa.setTelefones(telefoneRepository.getTelefoneByPessoa(pessoa.getId()));

		if (bindingResult.hasErrors()) {
			ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
			Iterable<Pessoa> pessoaIt = pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome")));
			andView.addObject("pessoas", pessoaIt);
			andView.addObject("pessoa", pessoa);
			andView.addObject("profissoes", profissaoRepository.findAll()); // carrega o combo Profissao

			List<String> msg = new ArrayList<>();
			for (ObjectError objectError : bindingResult.getAllErrors()) {
				msg.add(objectError.getDefaultMessage()); // vem das anotações dos atributos do model
			}

			andView.addObject("msg", msg);

			return andView;
		}

		if (file.getSize() > 0) { // Cadastrando um currículo

			pessoa.setCurriculo(file.getBytes());
			pessoa.setContentType(file.getContentType());
			pessoa.setOriginalFileName(file.getOriginalFilename());
		} else {

			if (pessoa.getId() != null && pessoa.getId() > 0) { // editando
				// para evitar que um arquivo uma vez cadastrado, tenha seu contéudo perdido
				pessoa.setCurriculo(pessoaRepository.findById(pessoa.getId()).get().getCurriculo());
				pessoa.setContentType(pessoaRepository.findById(pessoa.getId()).get().getContentType());
				pessoa.setOriginalFileName(pessoaRepository.findById(pessoa.getId()).get().getOriginalFileName());

			}
		}

		pessoaRepository.save(pessoa);
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		Iterable<Pessoa> pessoaIt = pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome")));
		andView.addObject("pessoas", pessoaIt);
		andView.addObject("profissoes", profissaoRepository.findAll()); // carrega o combo Profissao
		andView.addObject("pessoa", new Pessoa()); // instancia um objeto vazio
													// porque depois de salvar, ele volta para o form novamente.
		return andView;
	}

	@RequestMapping(value = "/listapessoa", method = RequestMethod.GET)
	public ModelAndView pessoas() {
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		Iterable<Pessoa> pessoa = pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome")));
		andView.addObject("pessoas", pessoa);

		andView.addObject("pessoa", new Pessoa()); // instancia um objeto vazio
		// porque depois de listar, ele volta para o form novamente.

		return andView;

	}

	@GetMapping("**/baixarcurriculo/{idpessoa}") // nova forma
	public void downloadCurriculo(@PathVariable("idpessoa") Long idpessoa, HttpServletResponse response)
			throws IOException {

		// Consultar o objeto pessoa no banco de dados
		Pessoa pessoa = pessoaRepository.findById(idpessoa).get();

		if (pessoa.getCurriculo() != null) {
			// Setar o tamanho da resposta
			response.setContentLength(pessoa.getCurriculo().length);

			// Tipo do arquivo para download. Pode ser um tipo generico:
			// application/octet-stream
			response.setContentType(pessoa.getContentType());

			// Define o cabeçalho da resposta - info default
			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", pessoa.getOriginalFileName());
			response.setHeader(headerKey, headerValue);

			// Finaliza a resposta passando o arquivo para o navegador
			response.getOutputStream().write(pessoa.getCurriculo());

		}

	}

	@GetMapping("/editarpessoa/{idpessoa}") // nova forma
	public ModelAndView editar(@PathVariable("idpessoa") Long idpessoa) {
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);
		andView.addObject("pessoa", pessoa.get());
		andView.addObject("profissoes", profissaoRepository.findAll()); // carrega o combo Profissao
		return andView;

	}

	@GetMapping("/removerpessoa/{idpessoa}")
	public ModelAndView excluir(@PathVariable("idpessoa") Long idpessoa) {
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");
		pessoaRepository.deleteById(idpessoa);

		andView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));

		andView.addObject("pessoa", new Pessoa()); // instancia um objeto vazio
		// porque depois de excluir, ele volta para o form novamente.
		return andView;

	}

	@PostMapping("**/pesquisarpessoa")
	public ModelAndView pesquisar(@RequestParam("nomePesquisa") String nome,
			@RequestParam("sexoPesquisa") String sexoPesquisa,
			@PageableDefault(size = 5, sort = { "nome" }) Pageable pageable) {
		ModelAndView andView = new ModelAndView("cadastro/cadastropessoa");

		Page<Pessoa> pessoas = null;

		if (sexoPesquisa != null && !sexoPesquisa.isEmpty() && nome != null && !nome.isEmpty()) {

			pessoas = pessoaRepository.findPessoaByNomeAndSexoPage(nome, sexoPesquisa, pageable);

		} else {
			pessoas = pessoaRepository.findPessoaByNamePage(nome, pageable);
		}

//	} else if (sexoPesquisa != null && !sexoPesquisa.isEmpty()) {
//			// Busca por sexo exato
//			//pessoas = pessoaRepository.findBySexo(sexoPesquisa);
//
//		} else {// Busca todos
////			List<Pessoa> pessoasBanco = pessoaRepository.findAll();
/// 		for (Pessoa pessoa : pessoasBanco) {
////				pessoas.add(pessoa);
////			}
//		}

		andView.addObject("pessoas", pessoas);
		andView.addObject("nomePesquisa", nome);
		andView.addObject("pessoa", new Pessoa()); // instancia um objeto vazio
		// porque depois de pesquisar, ele volta para o form novamente.
		return andView;

	}

	@GetMapping("**/pesquisarpessoa")
	public void imprimePDF(@RequestParam("nomePesquisa") String nome, @RequestParam("sexoPesquisa") String sexoPesquisa,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		List<Pessoa> pessoas = new ArrayList<>();

		if (sexoPesquisa != null && !sexoPesquisa.isEmpty() && nome != null && !nome.isEmpty()) {
			// Busca por parte do nome e sexo exato
			pessoas = pessoaRepository.findByNomeAndSexo(nome, sexoPesquisa);

		} else if (nome != null && !nome.isEmpty()) {
			// Busca por parte do nome
			pessoas = pessoaRepository.findByNome(nome);

		} else if (sexoPesquisa != null && !sexoPesquisa.isEmpty()) {
			// Busca por sexo exato
			pessoas = pessoaRepository.findBySexo(sexoPesquisa);
		}

		else {// Busca todos
			Iterable<Pessoa> iterator = pessoaRepository.findAll();
			for (Pessoa pessoa : iterator) {
				pessoas.add(pessoa);
			}
		}

		// Chama o serviço que faz a geração do relatório
		byte[] pdf = reportUtil.gerarRelatorio(pessoas, "Pessoa", request.getServletContext());

		// Tamanho da resposta para o navegador
		response.setContentLength(pdf.length);

		// Definir na resposta o tipo de arquivo, para o navegador saber o que ele deve
		// fazer
		response.setContentType("application/octet-stream"); // arquivos, pdf, mídia, vídeo

		// Definir o cabeçalho da resposta
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", "relatorio.pdf");
		response.setHeader(headerKey, headerValue);

		// Finaliza a resposta para o navegador
		response.getOutputStream().write(pdf);

	}

	@GetMapping("/telefones/{idpessoa}")
	public ModelAndView telefones(@PathVariable("idpessoa") Long idpessoa) {
		ModelAndView andView = new ModelAndView("cadastro/telefones");
		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);
		andView.addObject("pessoa", pessoa.get());
		andView.addObject("telefones", telefoneRepository.getTelefoneByPessoa(idpessoa));
		return andView;

	}

	@PostMapping("**/addfonePessoa/{pessoaid}")
	public ModelAndView addfonePessoa(@PathVariable("pessoaid") Long pessoaid, Telefone telefone) {
		Pessoa pessoa = pessoaRepository.findById(pessoaid).get();

		if (telefone != null && telefone.getNumero().isEmpty() || telefone.getTipo().isEmpty()) {

			ModelAndView andView = new ModelAndView("cadastro/telefones");
			andView.addObject("pessoa", pessoa);
			andView.addObject("telefones", telefoneRepository.getTelefoneByPessoa(pessoaid));

			List<String> msg = new ArrayList<>();
			if (telefone.getNumero().isEmpty()) {
				msg.add("Número deve ser informado");
			}

			if (telefone.getTipo().isEmpty()) {
				msg.add("Tipo deve ser informado");
			}
			andView.addObject("msg", msg);

			return andView;

		}

		ModelAndView andView = new ModelAndView("cadastro/telefones");

		telefone.setPessoa(pessoa); // amarra telefone a uma pessoa
		telefoneRepository.save(telefone);

		andView.addObject("pessoa", pessoa);
		andView.addObject("telefones", telefoneRepository.getTelefoneByPessoa(pessoaid));

		return andView;

	}

	@GetMapping("/removertelefone/{idtelefone}")
	public ModelAndView excluirTelefone(@PathVariable("idtelefone") Long idtelefone) {
		ModelAndView andView = new ModelAndView("cadastro/telefones");

		Pessoa pessoa = telefoneRepository.findById(idtelefone).get().getPessoa();

		telefoneRepository.deleteById(idtelefone);
		andView.addObject("telefones", telefoneRepository.getTelefoneByPessoa(pessoa.getId()));

		andView.addObject("pessoa", pessoa);

		return andView;

	}

}
