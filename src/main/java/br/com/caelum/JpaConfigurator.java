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
	 * Estamos usando o atributo destroyMethod na anotação @Bean. Esse atributo
	 * define o método (close) do Pool que o Spring chama quando o Tomcat é
	 * desligado. Assim garantimos que todas as conexões serão fechadas
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
		 * C3P0 = cria um pool de conexões
		 * 
		 * Documentação: http://www.mchange.com/projects/c3p0/
		 */
		ComboPooledDataSource dataSource = new ComboPooledDataSource();

		dataSource.setDriverClass("org.postgresql.Driver");
		dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/projeto_jpa");
		dataSource.setUser("usuarioteste");
		dataSource.setPassword("teste123");

		/*
		 * Quando todas as conexões em MinPoolSize estiverem em uso, o C3PO criará novas
		 * conexões. O número máximo de conexões a serem criadas não ultrapassará o
		 * máximo definido em MaxPoolSize.
		 * 
		 * Quando todas as conexões estiverem em uso e tivermos outras na fila para
		 * utilizar, essas que estão no aguardo serão de fato utilizadas conforme as
		 * conexões em uso forem sendo liberadas. Em resumo, caso não hajam mais
		 * conexões para serem usadas, o cliente precisará esperar por uma conexão
		 * disponível.
		 */
		dataSource.setMinPoolSize(5); // mínimo de 5 conexões
		dataSource.setMinPoolSize(10); // máxima de 10 conexões
		dataSource.setNumHelperThreads(5); // vai criar 5 threads para melhorar a performance

		/*
		 * Precisamos ensinar o pool a matar as conexões que ficam ociosas por muito
		 * tempo, eliminando o risco de escolher uma conexão quebrada.
		 * 
		 * O método setIdleConnectionTestPeriod(segundos) irá verificar por conexões
		 * ociosas por muito tempo e as eliminará.
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
		 * O que precisamos é de um espaço de "cache" que seja compartilhado entre os
		 * vários EntityManagers da nossa aplicação e que seja utilizado quando o cache
		 * de primeiro nível não detiver a informação desejada. Esse espaço chamamos de
		 * cache de segundo nível.
		 * 
		 * Em geral, lidar com um cache de segundo nível é bem mais complexo do que
		 * tratar com um de primeiro, uma vez que a possibilidade de trabalhar com dados
		 * desatualizados (stale) é bem maior. Os objetos desse cache são invalidados
		 * quando há alguma operação de escrita na entidade (como update). Se houver
		 * algum outro sistema atualizando os dados no banco sem passar pela JPA seu uso
		 * pode se tornar inexecutável.
		 * 
		 * Configurando o EhCache
		 * 
		 * Por padrão, o cache de segundo nível vem desabilitado. Para ativá-lo,
		 * precisamos adicionar uma chave a mais na configuração do Hibernate.
		 */
		props.setProperty("hibernate.cache.use_second_level_cache", "true");

		/*
		 * Conhecendo o provedor EhCache Além disso, é necessário informar ao Hibernate
		 * qual será o provedor de cache que usaremos. O JBoss Wildfly já possui um
		 * provider embarcado, ele se chama infinispan. Em nosso projeto, usaremos o
		 * EhCache que é um dos providers mais comuns de se trabalhar com Hibernate.
		 * 
		 * Com essa configuração já podemos começarmos a desfrutar do poder do cache de
		 * segundo nível. Porém, precisamos configurar nossas classes para usar cache.
		 * 
		 * Acessar a classe Produto e procurar a anotação @Cache para entender as outras
		 * configurações.
		 */
		props.setProperty("hibernate.cache.region.factory_class",
				"org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory");

		/*
		 * Permitindo fazer cache das queries criadas por nós (ex.: busca de produto por
		 * nome, loja e/ou categoria)
		 */
		props.setProperty("hibernate.cache.use_query_cache", "true");

		/*
		 * Habilitando trabalhar com as estatísiticas do Hibernate. Isso nos permitirá
		 * ter acesso a conexões abertas, ociosas, caches e etc.
		 * 
		 * Para utilizar essa ferramenta específica do Hibernate, precisa configurar o
		 * 
		 * @Bean que retorna uma instância de Statistcs (método abaixo).
		 * 
		 * Com isso configurado, podemos acessar ${statistics.<metodo>} em qualquer
		 * página JSP para ter acesso as informações da base de dados. Ver index.jsp
		 * 
		 * Blog da Caelum: http://blog.caelum.com.br/cacando-seus-gargalos-com-o-hibernate-statistics/
		 * 
		 * Documentação: http://docs.jboss.org/hibernate/core/4.3/javadocs/org/hibernate/stat/Statistics.html
		 * 
		 * Mais informações do blog da Caelum: http://blog.caelum.com.br/os-7-habitos-dos-desenvolvedores-hibernate-e-jpa-altamente-eficazes/
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
