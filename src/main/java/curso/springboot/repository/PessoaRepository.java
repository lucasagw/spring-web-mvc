package curso.springboot.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import curso.springboot.model.Pessoa;

@Repository
@Transactional
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

	// vai pesquisar no bd só as pessoas que tiverem parte do nome que for digitado.
	@Query("select p from Pessoa p where p.nome like %?1%")
	List<Pessoa> findByNome(String nome);

	@Query("select p from Pessoa p where p.nome like %?1% and p.sexo = ?2")
	List<Pessoa> findByNomeAndSexo(String nome, String sexo);

	@Query("select p from Pessoa p where p.sexo = ?1")
	List<Pessoa> findBySexo(String sexo);

	default Page<Pessoa> findPessoaByNamePage(String nome, Pageable pageable) {

		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);

		// Estamos configurando a pesquisa para consultar por partes do nome no banco de
		// dados, igual a like com SQL.
		ExampleMatcher exampleMatcher = ExampleMatcher.matchingAny().withMatcher("nome",
				ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());

		// Une o objeto com o valor e a configuração para consultar
		Example<Pessoa> example = Example.of(pessoa, exampleMatcher);

		Page<Pessoa> pessoas = findAll(example, pageable);

		return pessoas;

	}
	
	default Page<Pessoa> findPessoaByNomeAndSexoPage(String nome, String sexo, Pageable pageable) {

		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);
		pessoa.setSexo(sexo);

		// Estamos configurando a pesquisa para consultar por partes do nome no banco de
		// dados, igual a like com SQL.
		ExampleMatcher exampleMatcher = ExampleMatcher.matchingAny()
				.withMatcher("nome", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
				.withMatcher("sexo", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());
		
		// Une o objeto com o valor e a configuração para consultar
		Example<Pessoa> example = Example.of(pessoa, exampleMatcher);

		Page<Pessoa> pessoas = findAll(example, pageable);

		return pessoas;

	}

}
