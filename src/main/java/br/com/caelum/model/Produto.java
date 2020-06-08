
package br.com.caelum.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;

/*
 * Faz o SELECT após atualizar o Produto
 */
//@SelectBeforeUpdate(true)

/**
 * Quando realizamos uma alteração do produto em apenas um campo, todos os
 * outros também entram na query. No nosso caso , se alterarmos o nome, por
 * exemplo, a query final será:
 * 
 * update Produto set descricao=?, linkDaFoto=?, loja_id=?, nome=?, preco=?,
 * version=? where id=? and version=?
 * 
 * Usando @DynamicUpdate(true), apenas os campos alterados serão isneridos na
 * query. Ex.: update Produto set preco=? where id=?
 * 
 * A anotação @Chace configura a classe Produto para ser armazenada no Cache de
 * segundo nível.
 * 
 * A anotação @Cache exive informar a estratégia de concorrência.
 * 
 * As opção são:
 * 
 * <ul>
 * 
 * <li>A estratégia READ_ONLY deve ser utilizada quando uma entidade não deve
 * ser modificada.</li>
 * 
 * <li>A estratégia READ_WRITE deve ser utilizada quando uma entidade pode ser
 * modificada e há grandes chances que modificações em seu estado ocorram
 * simultaneamente. Essa estratégia é a que mais consome recursos.</li>
 * 
 * <li>A estratégia NONSTRICT_READ_WRITE deve ser utilizada quando uma entidade
 * pode ser modificada, mas é incomum que as alterações ocorram ao mesmo tempo.
 * Ela consome menos recursos que a estratégia READ_WRITE e é ideal quando não
 * há problemas de dados inconsistentes serem lidos quando ocorrem alterações
 * simultâneas.</li>
 * 
 * <li>A estratégia TRANSACTIONAL deve ser utilizada em ambientes JTA, como por
 * exemplo em servidores de aplicação. Como utilizamos Tomcat com Spring (sem
 * JTA) essa opção não funcionará.</li>
 * </ul>
 * 
 * <i>O cache invalida seus dados quando ocorre alguma operação de escrita em
 * uma entidade, pois suas informações ficam desatualizados em relação ao banco
 * de dados.</i>
 * 
 */
@DynamicUpdate(true)
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Produto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotEmpty
	private String nome;

	@NotEmpty
	private String linkDaFoto;

	@NotEmpty
	@Column(columnDefinition = "TEXT")
	private String descricao;

	@Min(20)
	private double preco;

	/*
	 * Mais de um produto pode ter mais de uma categoria. N -> N [CORRETO]
	 * 
	 * Não podemos definir @OneToMany pois não conseguiremos usar a mesma categoria
	 * para produtos diferentes. 1 -> N [CORRETO]
	 * 
	 * Uma tabela "produto_categoria" será criada na base. Essa tabela terá somente
	 * os ids de produto e categoria.
	 * 
	 * @JoinTable = estamos definindo o nome da tabela de relacionamento. Caso não
	 * preenchamos, o próprio JPA criará a tabela com a nomenclatura que achar
	 * conveniente.
	 * 
	 * 
	 * Cache de Collections
	 * 
	 * Quando configuramos o cache para a entidade Produto não dissemos que
	 * queríamos cachear também suas associações. Assim, podemos passar para o
	 * Hibernate que desejamos armazenar também as categorias de cada produto
	 * anotando o seu relacionamento com @Cache
	 * 
	 * Além disso, é necessário que a entidade Categoria tenha a anotação @Cache
	 */
	@ManyToMany
	@JoinTable(name = "categoria_produto")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<Categoria> categorias = new ArrayList<>();

	@Valid
	@ManyToOne
	private Loja loja;

	/*
	 * JPA oferece suporte a lock otimista através da anotação @Version.
	 * 
	 * Para guardar a versão, podemos usar um campo numérico ou um timestamp
	 * (Calendar ou Date). Se a entidade possuir @Version em todo update feito, o
	 * hibernate irá verificar automaticamente o valor desse campo. Caso o registro
	 * no banco possua um valor menor do que o está sendo enviado para o campo
	 * versao, ele aceita a atualização e incrementa seu valor. Caso possua um valor
	 * maior, será disparado uma exceção do tipo StaleObjectStateException dentro de
	 * uma javax.persistence.OptimisticLockException.
	 * 
	 * Ou seja, se duas pessoas tentarem atualizar o mesmo registro, a primeira
	 * pessoa conseguirá atualizar incrementando seu valor. A última será impedida
	 * de atualizar já que tentará enviar um valor inferior do que está no banco.
	 * 
	 */
	@Version
	private Integer versao;

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public void adicionarCategorias(Categoria... categorias) {
		for (Categoria categoria : categorias) {
			this.categorias.add(categoria);
		}
	}

	public String getLinkDaFoto() {
		return linkDaFoto;
	}

	public double getPreco() {
		return preco;
	}

	public void setPreco(double preco) {
		this.preco = preco;
	}

	public void setLinkDaFoto(String linkDaFoto) {
		this.linkDaFoto = linkDaFoto;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setLoja(Loja loja) {
		this.loja = loja;
	}

	public Loja getLoja() {
		return loja;
	}

	public List<Categoria> getCategorias() {
		return categorias;
	}

	public void setCategorias(List<Categoria> categorias) {
		this.categorias = categorias;
	}

	public Integer getVersao() {
		return versao;
	}

	public void setVersao(Integer versao) {
		this.versao = versao;
	}

}
