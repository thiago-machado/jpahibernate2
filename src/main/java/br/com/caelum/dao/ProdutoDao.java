package br.com.caelum.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import br.com.caelum.model.Loja;
import br.com.caelum.model.Produto;

@Repository
public class ProdutoDao {

	@PersistenceContext
	private EntityManager em;

	public List<Produto> getProdutos() {
		/*
		 * O código comentado abaixo faz um SELECT na tabela de produtos já trazendo
		 * suas categorias.
		 * 
		 * Um distinct é necessário pois produtos que possuem mais de uma categoria,
		 * serão selecionados mais de uma vez.
		 * 
		 * Essa solução ainda evita a criação de outros SELECTs para buscar somente as
		 * categorias de cada produto. Se temos, por exemplo, 3 produtos e cada produto
		 * tem pelo menos uma categoria, apenas 1 SELECT é realizado.
		 * 
		 * Essa é uma solução para evitar o lazy load. Isso torna desnecessário o uso do
		 * OpenEntityManagerInView (outra solução comentada abaixo).
		 * 
		 * Outra forma de resolver o lazy load é a configuração feita na classe
		 * Configurador, através do interceptador (método addInterceptors()).
		 * 
		 * Mas com essa outra solução, se temos 3 produtos com pelo menos uma
		 * cateogoria, 4 SELECTs serão realizados: 1 para pegar todos os produtos + 3
		 * para pegar cada lista de categorias de cada produto.
		 * 
		 */

		// return em.createQuery("SELECT DISTINCT p FROM Produto p JOIN FETCH
		// p.categorias", Produto.class)
		return em.createQuery("FROM Produto", Produto.class).getResultList();
	}

	public Produto getProduto(Integer id) {
		Produto produto = em.find(Produto.class, id);
		return produto;
	}

	public List<Produto> getProdutos(String nome, Integer categoriaId, Integer lojaId) {

		/*
		 * CriteriaBuilder é uma fábrica auxiliar para criar expressões sobre as funções
		 * que utilizaremos na busca. A fábrica não executa a query, ela apenas ajuda a
		 * criá-la.
		 * 
		 * O CriteriaBuilder possui métodos que definem operações da busca, como, por
		 * exemplo:
		 * 
		 * equal(), greaterThan(), lesserThan(), like() ... sum(), max(), min(), avg(),
		 * count(), desc(), distinct() ...
		 * 
		 */
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

		/*
		 * Com um CriteriaBuilder criado, podemos começar a criar um critério de
		 * consulta. Inicialmente invocamos o método createQuery passando para ele a
		 * classe que é o tipo de retorno da nossa consulta. No caso, se buscamos os
		 * Produtos, devemos colocar como parâmetro Produto.class.
		 * 
		 * Utilizando a CriteriaQuery descrevemos a busca como algo parecido ao JPQL.
		 * Quando escrevermos JPQL, usaremos palavras chaves como select, from, where ou
		 * groupBy. As mesmas palavras chaves aparecem na CriteriaQuery, mas aqui são
		 * nomes de métodos. Resumindo, encontramos os seguintes métodos na interface
		 * CriteriaQuery:
		 * 
		 * "select" "from" "where" "orderBy" "groupBy" "having"
		 */
		CriteriaQuery<Produto> query = criteriaBuilder.createQuery(Produto.class);

		/*
		 * Temos que, explicitamente, informar de onde deve ser feito o select. Já vimos
		 * que existe um método no CriteriaQuery chamado from. Através deste método,
		 * podemos passar a classe que vai servir de base na consulta.
		 * 
		 * Agora, através do método where, do CriteriaQuery, vamos colocar algumas
		 * regras para a busca. Esse método recebe como parâmetro um Predicate ou mais.
		 * O Predicate (aplicável ao where), assim como Expression, podem ser obtidos
		 * através de métodos como o CriteriaBuilder.
		 * 
		 * A raiz (root) é usada para definir os caminhos (path) até os atributos do
		 * objeto. Por exemplo, se quisermos o caminho até o atributo "nome" do produto
		 * selecionado, usaremos: root.<String> get("nome");
		 * 
		 * ATENÇÃO: Como parâmetro do método get passamos o nome do atributo da Entity e
		 * para garantir o tipo do retorno, colocamos ele antes do método e entre os
		 * símbolos "< >".
		 */
		Root<Produto> root = query.from(Produto.class);

		Path<String> nomePath = root.<String>get("nome");

		/*
		 * Vamos adicionar "categoria" e "loja" à consulta e faremos isso da mesma
		 * maneira que fizemos com o nome, ou seja, através de Predicates. Antes disso,
		 * precisamos do caminho para chegar no id da loja. Como id é um atributo da
		 * classe Loja precisamos encontrar a loja, e a partir desta, encontrar o seu id
		 */
		Path<Integer> lojaPath = root.<Loja>get("loja").<Integer>get("id");

		/*
		 * Faremos o mesmo com "categoria". Com a pequena diferença de que o
		 * relacionamento de "categoria" é @ManyToMany. Ou seja, precisaremos de um join
		 * - a partir do produto - como já vimos.
		 */
		Path<Integer> categoriaPath = root.join("categorias").<Integer>get("id");

		/*
		 * Ou seja, precisamos fazer um join a partir do produto e se o id da categoria
		 * não for null, criamos o Predicate.
		 */
		List<Predicate> predicates = new ArrayList<>();

		// Criando uma cláusula para ser inserida no WHERE que verifica se o nome do
		// produto existe
		if (!nome.isEmpty()) {
			Predicate nomeIgual = criteriaBuilder.like(nomePath, "%" + nome + "%");
			predicates.add(nomeIgual);
		}

		// Criando uma cláusula para ser inserida no WHERE que verifica se a categoria é
		// igual a escolhida
		if (categoriaId != null) {
			Predicate categoriaIgual = criteriaBuilder.equal(categoriaPath, categoriaId);
			predicates.add(categoriaIgual);
		}

		// Criando uma cláusula para ser inserida no WHERE que verifica se o id da loja
		// é igual
		if (lojaId != null) {
			Predicate lojaIgual = criteriaBuilder.equal(lojaPath, lojaId);
			predicates.add(lojaIgual);
		}

		// Inserindo todos os Predicates (cláusulas para consulta) no WHERE
		query.where((Predicate[]) predicates.toArray(new Predicate[0]));

		TypedQuery<Produto> typedQuery = em.createQuery(query);
		
		// Permitindo fazer cache dessa query criada por nós
		typedQuery.setHint("org.hibernate.cacheable", "true");
		
		return typedQuery.getResultList();

	}

	public void insere(Produto produto) {
		if (produto.getId() == null)
			em.persist(produto);
		else
			em.merge(produto);
	}

}
