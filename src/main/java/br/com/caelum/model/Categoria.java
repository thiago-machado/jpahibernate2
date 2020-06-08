package br.com.caelum.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Cache de Collections
 * 
 * Quando configuramos o cache para a entidade Produto não dissemos que
 * queríamos cachear também suas associações.
 * 
 * Mesmo informando na collection de Categoria em Produto que desejamos que as
 * associações também sejam cacheadas, se faz necessário inserir a anotação
 * aqui.
 * 
 * Pois o cache do collections armazena apenas os ids das associações, ou seja, os
 * ids das categorias associadas com o produto! O que de fato nos interessa, é o
 * nome da categoria. Portanto, com esses ids em mãos o Hibernate busca no banco
 * os dados das categorias relacionadas a esse produto (música e tecnologia, por
 * isso dois selects!). Esse problema é bem parecido com o problema do "N + 1"
 * onde para cada produto temos uma query que busca as categorias.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String nome;

	public Categoria(String nome) {
		this.nome = nome;
	}

	public Categoria() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

}
