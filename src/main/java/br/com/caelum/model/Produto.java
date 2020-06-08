
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
 * Faz o SELECT ap�s atualizar o Produto
 */
//@SelectBeforeUpdate(true)

/**
 * Quando realizamos uma altera��o do produto em apenas um campo, todos os
 * outros tamb�m entram na query. No nosso caso , se alterarmos o nome, por
 * exemplo, a query final ser�:
 * 
 * update Produto set descricao=?, linkDaFoto=?, loja_id=?, nome=?, preco=?,
 * version=? where id=? and version=?
 * 
 * Usando @DynamicUpdate(true), apenas os campos alterados ser�o isneridos na
 * query. Ex.: update Produto set preco=? where id=?
 * 
 * A anota��o @Chace configura a classe Produto para ser armazenada no Cache de
 * segundo n�vel.
 * 
 * A anota��o @Cache exive informar a estrat�gia de concorr�ncia.
 * 
 * As op��o s�o:
 * 
 * <ul>
 * 
 * <li>A estrat�gia READ_ONLY deve ser utilizada quando uma entidade n�o deve
 * ser modificada.</li>
 * 
 * <li>A estrat�gia READ_WRITE deve ser utilizada quando uma entidade pode ser
 * modificada e h� grandes chances que modifica��es em seu estado ocorram
 * simultaneamente. Essa estrat�gia � a que mais consome recursos.</li>
 * 
 * <li>A estrat�gia NONSTRICT_READ_WRITE deve ser utilizada quando uma entidade
 * pode ser modificada, mas � incomum que as altera��es ocorram ao mesmo tempo.
 * Ela consome menos recursos que a estrat�gia READ_WRITE e � ideal quando n�o
 * h� problemas de dados inconsistentes serem lidos quando ocorrem altera��es
 * simult�neas.</li>
 * 
 * <li>A estrat�gia TRANSACTIONAL deve ser utilizada em ambientes JTA, como por
 * exemplo em servidores de aplica��o. Como utilizamos Tomcat com Spring (sem
 * JTA) essa op��o n�o funcionar�.</li>
 * </ul>
 * 
 * <i>O cache invalida seus dados quando ocorre alguma opera��o de escrita em
 * uma entidade, pois suas informa��es ficam desatualizados em rela��o ao banco
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
	 * N�o podemos definir @OneToMany pois n�o conseguiremos usar a mesma categoria
	 * para produtos diferentes. 1 -> N [CORRETO]
	 * 
	 * Uma tabela "produto_categoria" ser� criada na base. Essa tabela ter� somente
	 * os ids de produto e categoria.
	 * 
	 * @JoinTable = estamos definindo o nome da tabela de relacionamento. Caso n�o
	 * preenchamos, o pr�prio JPA criar� a tabela com a nomenclatura que achar
	 * conveniente.
	 * 
	 * 
	 * Cache de Collections
	 * 
	 * Quando configuramos o cache para a entidade Produto n�o dissemos que
	 * quer�amos cachear tamb�m suas associa��es. Assim, podemos passar para o
	 * Hibernate que desejamos armazenar tamb�m as categorias de cada produto
	 * anotando o seu relacionamento com @Cache
	 * 
	 * Al�m disso, � necess�rio que a entidade Categoria tenha a anota��o @Cache
	 */
	@ManyToMany
	@JoinTable(name = "categoria_produto")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<Categoria> categorias = new ArrayList<>();

	@Valid
	@ManyToOne
	private Loja loja;

	/*
	 * JPA oferece suporte a lock otimista atrav�s da anota��o @Version.
	 * 
	 * Para guardar a vers�o, podemos usar um campo num�rico ou um timestamp
	 * (Calendar ou Date). Se a entidade possuir @Version em todo update feito, o
	 * hibernate ir� verificar automaticamente o valor desse campo. Caso o registro
	 * no banco possua um valor menor do que o est� sendo enviado para o campo
	 * versao, ele aceita a atualiza��o e incrementa seu valor. Caso possua um valor
	 * maior, ser� disparado uma exce��o do tipo StaleObjectStateException dentro de
	 * uma javax.persistence.OptimisticLockException.
	 * 
	 * Ou seja, se duas pessoas tentarem atualizar o mesmo registro, a primeira
	 * pessoa conseguir� atualizar incrementando seu valor. A �ltima ser� impedida
	 * de atualizar j� que tentar� enviar um valor inferior do que est� no banco.
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
