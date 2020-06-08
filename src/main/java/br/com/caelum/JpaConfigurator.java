package br.com.caelum;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@EnableTransactionManagement
public class JpaConfigurator {

	/*
	 * Estamos usando o atributo destroyMethod na anota��o @Bean. Esse atributo
	 * define o m�todo (close) do Pool que o Spring chama quando o Tomcat �
	 * desligado. Assim garantimos que todas as conex�es ser�o fechadas
	 * corretamente.
	 */
	@Bean(destroyMethod = "close")
	public DataSource getDataSource() throws PropertyVetoException {
		/*
		 * DriverManagerDataSource dataSource = new DriverManagerDataSource();
		 * 
		 * dataSource.setDriverClassName("org.postgresql.Driver");
		 * dataSource.setUrl("jdbc:postgresql://localhost:5432/projeto_jpa");
		 * dataSource.setUsername("usuarioteste"); dataSource.setPassword("teste123");
		 */

		/*
		 * C3P0 = cria um pool de conex�es
		 * 
		 * Documenta��o: http://www.mchange.com/projects/c3p0/
		 */
		ComboPooledDataSource dataSource = new ComboPooledDataSource();

		dataSource.setDriverClass("org.postgresql.Driver");
		dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/projeto_jpa");
		dataSource.setUser("usuarioteste");
		dataSource.setPassword("teste123");

		/*
		 * Quando todas as conex�es em MinPoolSize estiverem em uso, o C3PO criar� novas
		 * conex�es. O n�mero m�ximo de conex�es a serem criadas n�o ultrapassar� o
		 * m�ximo definido em MaxPoolSize.
		 * 
		 * Quando todas as conex�es estiverem em uso e tivermos outras na fila para
		 * utilizar, essas que est�o no aguardo ser�o de fato utilizadas conforme as
		 * conex�es em uso forem sendo liberadas. Em resumo, caso n�o hajam mais
		 * conex�es para serem usadas, o cliente precisar� esperar por uma conex�o
		 * dispon�vel.
		 */
		dataSource.setMinPoolSize(5); // m�nimo de 5 conex�es
		dataSource.setMinPoolSize(10); // m�xima de 10 conex�es
		dataSource.setNumHelperThreads(5); // vai criar 5 threads para melhorar a performance

		/*
		 * Precisamos ensinar o pool a matar as conex�es que ficam ociosas por muito
		 * tempo, eliminando o risco de escolher uma conex�o quebrada.
		 * 
		 * O m�todo setIdleConnectionTestPeriod(segundos) ir� verificar por conex�es
		 * ociosas por muito tempo e as eliminar�.
		 */
		dataSource.setIdleConnectionTestPeriod(60);

		return dataSource;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean getEntityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

		entityManagerFactory.setPackagesToScan("br.com.caelum");
		entityManagerFactory.setDataSource(dataSource);

		entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		Properties props = new Properties();

		props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		props.setProperty("hibernate.show_sql", "true");
		props.setProperty("hibernate.hbm2ddl.auto", "create-drop");

		/*
		 * O que precisamos � de um espa�o de "cache" que seja compartilhado entre os
		 * v�rios EntityManagers da nossa aplica��o e que seja utilizado quando o cache
		 * de primeiro n�vel n�o detiver a informa��o desejada. Esse espa�o chamamos de
		 * cache de segundo n�vel.
		 * 
		 * Em geral, lidar com um cache de segundo n�vel � bem mais complexo do que
		 * tratar com um de primeiro, uma vez que a possibilidade de trabalhar com dados
		 * desatualizados (stale) � bem maior. Os objetos desse cache s�o invalidados
		 * quando h� alguma opera��o de escrita na entidade (como update). Se houver
		 * algum outro sistema atualizando os dados no banco sem passar pela JPA seu uso
		 * pode se tornar inexecut�vel.
		 * 
		 * Configurando o EhCache
		 * 
		 * Por padr�o, o cache de segundo n�vel vem desabilitado. Para ativ�-lo,
		 * precisamos adicionar uma chave a mais na configura��o do Hibernate.
		 */
		props.setProperty("hibernate.cache.use_second_level_cache", "true");

		/*
		 * Conhecendo o provedor EhCache Al�m disso, � necess�rio informar ao Hibernate
		 * qual ser� o provedor de cache que usaremos. O JBoss Wildfly j� possui um
		 * provider embarcado, ele se chama infinispan. Em nosso projeto, usaremos o
		 * EhCache que � um dos providers mais comuns de se trabalhar com Hibernate.
		 * 
		 * Com essa configura��o j� podemos come�armos a desfrutar do poder do cache de
		 * segundo n�vel. Por�m, precisamos configurar nossas classes para usar cache.
		 * 
		 * Acessar a classe Produto e procurar a anota��o @Cache para entender as outras
		 * configura��es.
		 */
		props.setProperty("hibernate.cache.region.factory_class",
				"org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory");

		/*
		 * Permitindo fazer cache das queries criadas por n�s (ex.: busca de produto por
		 * nome, loja e/ou categoria)
		 */
		props.setProperty("hibernate.cache.use_query_cache", "true");

		/*
		 * Habilitando trabalhar com as estat�siticas do Hibernate. Isso nos permitir�
		 * ter acesso a conex�es abertas, ociosas, caches e etc.
		 * 
		 * Para utilizar essa ferramenta espec�fica do Hibernate, precisa configurar o
		 * 
		 * @Bean que retorna uma inst�ncia de Statistcs (m�todo abaixo).
		 * 
		 * Com isso configurado, podemos acessar ${statistics.<metodo>} em qualquer
		 * p�gina JSP para ter acesso as informa��es da base de dados. Ver index.jsp
		 * 
		 * Blog da Caelum: http://blog.caelum.com.br/cacando-seus-gargalos-com-o-hibernate-statistics/
		 * 
		 * Documenta��o: http://docs.jboss.org/hibernate/core/4.3/javadocs/org/hibernate/stat/Statistics.html
		 * 
		 * Mais informa��es do blog da Caelum: http://blog.caelum.com.br/os-7-habitos-dos-desenvolvedores-hibernate-e-jpa-altamente-eficazes/
		 */
		props.setProperty("hibernate.generate_statistics", "true");

		entityManagerFactory.setJpaProperties(props);
		return entityManagerFactory;
	}

	@Bean
	public Statistics statistics(EntityManagerFactory emf) {
		return emf.unwrap(SessionFactory.class).getStatistics();
	}

	@Bean
	public JpaTransactionManager getTransactionManager(EntityManagerFactory emf) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(emf);

		return transactionManager;
	}

}
