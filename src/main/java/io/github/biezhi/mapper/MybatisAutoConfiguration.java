package io.github.biezhi.mapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for Mybatis. Contributes a
 * {@link SqlSessionFactory} and a {@link SqlSessionTemplate}.
 * <p>
 * If {@link org.mybatis.spring.annotation.MapperScan} is used, or a configuration file is
 * specified as a property, those will be considered, otherwise this auto-configuration
 * will attempt to register mappers based on the interface definitions in or under the
 * root auto-configuration package.
 *
 * @author Eddú Meléndez
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@EnableConfigurationProperties({MybatisProperties.class, PageHelperProperties.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MybatisAutoConfiguration {

    private static Log log = LogFactory.getLog(MybatisAutoConfiguration.class);

    @Autowired
    private MybatisProperties properties;

    @Autowired
    private PageHelperProperties pageHelperProperties;

    @Autowired(required = false)
    private Interceptor[] interceptors;

    @Autowired
    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @PostConstruct
    public void checkConfigFileExists() {
        if (this.properties.isCheckConfigLocation()) {
            Resource resource = this.resourceLoader
                    .getResource(this.properties.getConfig());
            Assert.state(resource.exists(),
                    "Cannot find config location: " + resource
                            + " (please add config file or check your Mybatis "
                            + "configuration)");
        }
    }

    @Bean(name = "sqlSessionFactory")
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        if (StringUtils.hasText(this.properties.getConfig())) {
            factory.setConfigLocation(
                    this.resourceLoader.getResource(this.properties.getConfig()));
        } else {
            if (this.interceptors != null && this.interceptors.length > 0) {
                factory.setPlugins(this.interceptors);
            }
            factory.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
            factory.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
            factory.setMapperLocations(this.properties.getMapperLocations());
        }
        return factory.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory,
                this.properties.getExecutorType());
    }

}