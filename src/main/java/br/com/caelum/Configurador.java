package br.com.caelum;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import br.com.caelum.dao.CategoriaDao;
import br.com.caelum.dao.LojaDao;
import br.com.caelum.dao.ProdutoDao;
import br.com.caelum.model.Categoria;
import br.com.caelum.model.Loja;
import br.com.caelum.model.Produto;

@Configuration
@EnableWebMvc
@ComponentScan("br.com.caelum")
@EnableTransactionManagement
public class Configurador extends WebMvcConfigurerAdapter {

	@Bean
	@Scope("request")
	public List<Produto> produtos(ProdutoDao produtoDao) {
		List<Produto> produtos = produtoDao.getProdutos();

		return produtos;
	}

	@Bean
	public List<Categoria> categorias(CategoriaDao categoriaDao) {
		List<Categoria> categorias = categoriaDao.getCategorias();

		return categorias;
	}

	@Bean
	public List<Loja> lojas(LojaDao lojaDao) {
		List<Loja> lojas = lojaDao.getLojas();

		return lojas;
	}

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

		messageSource.setBasename("/WEB-INF/messages");
		messageSource.setCacheSeconds(1);
		messageSource.setDefaultEncoding("ISO-8859-1");

		return messageSource;

	}

	@Bean
	public ViewResolver getViewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();

		viewResolver.setPrefix("/WEB-INF/views/");
		viewResolver.setSuffix(".jsp");

		viewResolver.setExposeContextBeansAsAttributes(true);

		return viewResolver;
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(new Converter<String, Categoria>() {

			@Override
			public Categoria convert(String categoriaId) {
				Categoria categoria = new Categoria();
				categoria.setId(Integer.valueOf(categoriaId));

				return categoria;
			}

		});
	}

	@Bean
	public OpenEntityManagerInViewInterceptor getOpenEntityManagerInViewInterceptor() {
		return new OpenEntityManagerInViewInterceptor();
	}

	/*
	 * RESOLVENDO RELACIONAMENTOS COM LAZY NO SPRING
	 * 
	 * Os interceptors permitem executar um código antes (e depois) de chamarem o
	 * método Controller. Para registrar o OpenEntityManagerInViewInterceptor na
	 * aplicação para sobrescrever o método addInterceptors que ganhamos por herança
	 * de WebMvcConfigurerAdapter.
	 * 
	 * Esse método permite adicionar interceptadores ao contexto do SpringMVC.
	 * 
	 * Mas, o que passar como argumento? Precisamos passar uma instância do
	 * interceptor que será registrada. Porém, o container de IoC do Spring precisa
	 * conhecer essa instância, sendo assim, é necessário criar um método que
	 * retorne uma instância do interceptor e anotá-lo com @Bean (método acima).
	 * 
	 * Com essa configuração, o EntitityManager será aberto no início da requisição
	 * e fechará no final.
	 * 
	 * Ex.: ao editar um produto, fazemos um find() no mesmo para apresentar suas
	 * informações na tela de edição. Na tela de edição, precisamos iterar a lista
	 * de categorias. Contudo, a lista de categorias ainda não foi carregada. Se o
	 * EntityManager estiver fechado, receberemos uma exceção do tipo Lazy, pois o
	 * Hibernate vai tentar fazer a busca das categorias mas o EM já foi fechado.
	 * 
	 * Essa configuração que fizemos então resolveria isso: abre o EntityManager no
	 * início da requisição e o fecha quando a página de fato for apresentada ao
	 * usuário (final da requisição).
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addWebRequestInterceptor(getOpenEntityManagerInViewInterceptor());
	}

}
