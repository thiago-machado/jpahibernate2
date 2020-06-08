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
	 * Os interceptors permitem executar um c�digo antes (e depois) de chamarem o
	 * m�todo Controller. Para registrar o OpenEntityManagerInViewInterceptor na
	 * aplica��o para sobrescrever o m�todo addInterceptors que ganhamos por heran�a
	 * de WebMvcConfigurerAdapter.
	 * 
	 * Esse m�todo permite adicionar interceptadores ao contexto do SpringMVC.
	 * 
	 * Mas, o que passar como argumento? Precisamos passar uma inst�ncia do
	 * interceptor que ser� registrada. Por�m, o container de IoC do Spring precisa
	 * conhecer essa inst�ncia, sendo assim, � necess�rio criar um m�todo que
	 * retorne uma inst�ncia do interceptor e anot�-lo com @Bean (m�todo acima).
	 * 
	 * Com essa configura��o, o EntitityManager ser� aberto no in�cio da requisi��o
	 * e fechar� no final.
	 * 
	 * Ex.: ao editar um produto, fazemos um find() no mesmo para apresentar suas
	 * informa��es na tela de edi��o. Na tela de edi��o, precisamos iterar a lista
	 * de categorias. Contudo, a lista de categorias ainda n�o foi carregada. Se o
	 * EntityManager estiver fechado, receberemos uma exce��o do tipo Lazy, pois o
	 * Hibernate vai tentar fazer a busca das categorias mas o EM j� foi fechado.
	 * 
	 * Essa configura��o que fizemos ent�o resolveria isso: abre o EntityManager no
	 * in�cio da requisi��o e o fecha quando a p�gina de fato for apresentada ao
	 * usu�rio (final da requisi��o).
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addWebRequestInterceptor(getOpenEntityManagerInViewInterceptor());
	}

}
