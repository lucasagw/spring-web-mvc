package curso.springboot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
@EnableWebSecurity
public class WebConfigSecurity extends WebSecurityConfigurerAdapter {

	@Autowired
	ImplementacaoUserDetailsService implUserDetailsService;

	@Override // Configura as solicitações de acesso por Http
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable() // Desativa as configurações padrão de memória do spring.
				.authorizeRequests() // Permitir restringir acessos
				.antMatchers(HttpMethod.GET, "/").permitAll() // Qualquer usuário acessa a página incial
				//Começa a montar a pilha de restrições
				.antMatchers(HttpMethod.GET, "/cadastropessoa").hasAnyRole("ADMIN").anyRequest().authenticated() 
				.and().formLogin().permitAll() // permite qualquer usuário
				.loginPage("/login") // se isso nao for colocado, será usado o form login default
				.defaultSuccessUrl("/cadastropessoa")
				.failureUrl("/login?error=true")
				.and()
				.logout()// Mapeia URL de Logout e invalida usuário autenticado
				.logoutSuccessUrl("/login") 
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout")); // invalida a seção
	}

	@Override // Cria autenticação do usuário com banco de dados ou em memória
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(implUserDetailsService).passwordEncoder(new BCryptPasswordEncoder());

//		auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder())
//		.withUser("Lucas")
//		.password("$2a$10$8R9Gi8rQW9PXRaqXInuOLej1F8c7EDgpGpPzGuxY8k9CL1ibjuHGW") //123
//		.roles("ADMIN");

	}

	@Override // Ignora URL especificas
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/static/materialize/**");
	}

}
