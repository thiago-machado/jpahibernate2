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
		 * O c�digo comentado abaixo faz um SELECT na tabela de produtos j� trazendo
		 * suas categorias.
		 * 
		 * Um distinct � necess�rio pois produtos que possuem mais de uma categoria,
		 * ser�o selecionados mais de uma vez.
		 * 
		 * Essa solu��o ainda evita a cria��o de outros SELECTs para buscar somente as
		 * categorias de cada produto. Se temos, por exemplo, 3 produtos e cada produto
		 * tem pelo menos uma categoria, apenas 1 SELECT � realizado.
		 * 
		 * Essa � uma solu��o para evitar o lazy load. Isso torna desnecess�rio o uso do
		 * OpenEntityManagerInView (outra solu��o comentada abaixo).
		 * 
		 * Outra forma de resolver o lazy load � a configura��o feita na classe
		 * Configurador, atrav�s do interceptador (m�todo addInterceptors()).
		 * 
		 * Mas com essa outra solu��o, se temos 3 produtos com pelo menos uma
		 * cateogoria, 4 SELECTs ser�o realizados: 1 para pegar todos os produtos + 3
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
		 * CriteriaBuilder � uma f�brica auxiliar para criar express�es sobre as fun��es
		 * que utilizaremos na busca. A f�brica n�o executa a query, ela apenas ajuda a
		 * cri�-la.
		 * 
		 * O CriteriaBuilder possui m�todos que definem opera��es da busca, como, por
		 * exemplo:
		 * 
		 * equal(), greaterThan(), lesserThan(), like() ... sum(), max(), min(), avg(),
		 * count(), desc(), distinct() ...
		 * 
		 */
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

		/*
		 * Com um CriteriaBuilder criado, podemos come�ar a criar um crit�rio de
		 * consulta. Inicialmente invocamos o m�todo createQuery passando para ele a
		 * classe que � o tipo de retorno da nossa consulta. No caso, se buscamos os
		 * Produtos, devemos colocar como par�metro Produto.class.
		 * 
		 * Utilizando a CriteriaQuery descrevemos a busca como algo parecido ao JPQL.
		 * Quando escrevermos JPQL, usaremos palavras chaves como select, from, where ou
		 * groupBy. As mesmas palavras chaves aparecem na CriteriaQuery, mas aqui s�o
		 * nomes de m�todos. Resumindo, encontramos os seguintes m�todos na interface
		 * CriteriaQuery:
		 * 
		 * "select" "from" "where" "orderBy" "groupBy" "having"
		 */
		CriteriaQuery<Produto> query = criteriaBuilder.createQuery(Produto.class);

		/*
		 * Temos que, explicitamente, informar de onde deve ser feito o select. J� vimos
		 * que existe um m�todo no CriteriaQuery chamado from. Atrav�s deste m�todo,
		 * podemos passar a classe que vai servir de base na consulta.
		 * 
		 * Agora, atrav�s do m�todo where, do CriteriaQuery, vamos colocar algumas
		 * regras para a busca. Esse m�todo recebe como par�metro um Predicate ou mais.
		 * O Predicate (aplic�vel ao where), assim como Expression, podem ser obtidos
		 * atrav�s de m�todos como o CriteriaBuilder.
		 * 
		 * A raiz (root) � usada para definir os caminhos (path) at� os atributos do
		 * objeto. Por exemplo, se quisermos o caminho at� o atributo "nome" do produto
		 * selecionado, usaremos: root.<String> get("nome");
		 * 
		 * ATEN��O: Como par�metro do m�todo get passamos o nome do atributo da Entity e
		 * para garantir o tipo do retorno, colocamos ele antes do m�todo e entre os
		 * s�mbolos "< >".
		 */
		Root<Produto> root = query.from(Produto.class);

		Path<String> nomePath = root.<String>get("nome");

		/*
		 * Vamos adicionar "categoria" e "loja" � consulta e faremos isso da mesma
		 * maneira que fizemos com o nome, ou seja, atrav�s de Predicates. Antes disso,
		 * precisamos do caminho para chegar no id da loja. Como id � um atributo da
		 * classe Loja precisamos encontrar a loja, e a partir desta, encontrar o seu id
		 */
		Path<Integer> lojaPath = root.<Loja>get("loja").<Integer>get("id");

		/*
		 * Faremos o mesmo com "categoria". Com a pequena diferen�a de que o
		 * relacionamento de "categoria" � @ManyToMany. Ou seja, precisaremos de um join
		 * - a partir do produto - como j� vimos.
		 */
		Path<Integer> categoriaPath = root.join("categorias").<Integer>get("id");

		/*
		 * Ou seja, precisamos fazer um join a partir do produto e se o id da categoria
		 * n�o for null, criamos o Predicate.
		 */
		List<Predicate> predicates = new ArrayList<>();

		// Criando uma cl�usula para ser inserida no WHERE que verifica se o nome do
		// produto existe
		if (!nome.isEmpty()) {
			Predicate nomeIgual = criteriaBuilder.like(nomePath, "%" + nome + "%");
			predicates.add(nomeIgual);
		}

		// Criando uma cl�usula para ser inserida no WHERE que verifica se a categoria �
		// igual a escolhida
		if (categoriaId != null) {
			Predicate categoriaIgual = criteriaBuilder.equal(categoriaPath, categoriaId);
			predicates.add(categoriaIgual);
		}

		// Criando uma cl�usula para ser inserida no WHERE que verifica se o id da loja
		// � igual
		if (lojaId != null) {
			Predicate lojaIgual = criteriaBuilder.equal(lojaPath, lojaId);
			predicates.add(lojaIgual);
		}

		// Inserindo todos os Predicates (cl�usulas para consulta) no WHERE
		query.where((Predicate[]) predicates.toArray(new Predicate[0]));

		TypedQuery<Produto> typedQuery = em.createQuery(query);
		
		// Permitindo fazer cache dessa query criada por n�s
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
