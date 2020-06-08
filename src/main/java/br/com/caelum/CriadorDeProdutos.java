package br.com.caelum;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.caelum.model.Categoria;
import br.com.caelum.model.Loja;
import br.com.caelum.model.Produto;

@Component
public class CriadorDeProdutos {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JpaTransactionManager transactionManager;

	@PostConstruct
	public void init() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				Loja casaDoCodigo = new Loja();
				casaDoCodigo.setNome("Casa do Código");
				em.persist(casaDoCodigo);

				Loja modelViewController = new Loja();
				modelViewController.setNome("Model View Controller");
				em.persist(modelViewController);

				Categoria tecnologia = new Categoria("Tecnologia");
				em.persist(tecnologia);

				Categoria mvc = new Categoria("MVC");
				em.persist(mvc);

				cadastrarLivro1(casaDoCodigo, tecnologia);
				cadastrarLivro2(modelViewController, mvc);
				cadastrarLivro3(casaDoCodigo, tecnologia, mvc);
			}

			private void cadastrarLivro3(Loja casaDoCodigo, Categoria tecnologia, Categoria mvc) {
				Produto livroSpringFramework = new Produto();
				livroSpringFramework.setNome("Vire o jogo com Spring Framework");
				livroSpringFramework.setLoja(casaDoCodigo);
				livroSpringFramework.setPreco(30.0);
				livroSpringFramework.adicionarCategorias(tecnologia, mvc);
				livroSpringFramework.setDescricao("Criado para simplificar o desenvolvimento de aplicações Java, "
						+ "o Spring se tornou um dos frameworks de mais destaque dentro desse grande ambiente.  "
						+ "Aprenda muito mais que o básico do Spring, desde o tradicional Container de Inversão "
						+ "de Controle e Injeção de Dependências, passando pelos robustos módulos de segurança, "
						+ "transações, programação orientada a aspectos e tamb�m o fantástico módulo MVC, o SpringMVC.");

				livroSpringFramework.setLinkDaFoto(
						"http://cdn.shopify.com/s/files/1/0155/7645/products/spring-framework-featured_large.png?v=1411567960");

				em.persist(livroSpringFramework);
			}

			private void cadastrarLivro2(Loja modelViewController, Categoria mvc) {
				Produto livroSpringMVC = new Produto();
				livroSpringMVC.setNome("Spring MVC");
				livroSpringMVC.setLoja(modelViewController);
				livroSpringMVC.setPreco(30.0);
				livroSpringMVC.adicionarCategorias(mvc);
				livroSpringMVC
						.setDescricao("O Spring é o principal concorrente da especificação JavaEE. Com uma plataforma "
								+ "muito estável e com integração fina entre todas as suas extensões, fornece "
								+ "um ambiente muito propício para que o programador foque nas regras de negócio "
								+ "e esqueça dos problemas de infraestrutura.");

				livroSpringMVC.setLinkDaFoto(
						"https://cdn.shopify.com/s/files/1/0155/7645/products/spring-mvc-featured_large.png?v=1411567960");

				em.persist(livroSpringMVC);
			}

			private void cadastrarLivro1(Loja casaDoCodigo, Categoria tecnologia) {
				Produto livroSpringBoot = new Produto();
				livroSpringBoot.setNome("Spring Boot");
				livroSpringBoot.setLoja(casaDoCodigo);
				livroSpringBoot.setPreco(49.0);
				livroSpringBoot.setDescricao("Spring Boot é uma maneira eficiente e eficaz de criar uma aplicação "
						+ "em Spring e facilmente colocá-la no ar, funcionando sem depender de um "
						+ "servidor de aplicação. Não se trata de um simples framework, mas de um "
						+ "conceito totalmente novo de criar aplicações web. Além de impulsionar o "
						+ "desenvolvimento para microsserviços, o Spring Boot ajuda na configuração "
						+ "importando e configurando automaticamente todas as dependências.");

				livroSpringBoot.adicionarCategorias(tecnologia);
				livroSpringBoot.setLinkDaFoto(
						"https://cdn.shopify.com/s/files/1/0155/7645/products/7aXPAWM4TObeQ4OOv3mUY-mrVzqf23Ty6enIslrhXvM_large.jpg?v=1501874081");

				em.persist(livroSpringBoot);
			}
		});
	}

}
