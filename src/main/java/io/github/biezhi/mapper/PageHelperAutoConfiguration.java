package io.github.biezhi.mapper;

import com.github.pagehelper.PageInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
public class PageHelperAutoConfiguration implements EnvironmentAware {

    private static Log log = LogFactory.getLog(PageHelperAutoConfiguration.class);

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @Autowired
    private PageHelperProperties pageHelperProperties;

    private RelaxedPropertyResolver resolver;

    @PostConstruct
    public void addPageInterceptor() {
        log.info("Setting PageInterceptor");
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = pageHelperProperties.getProperties();
        Map<String, Object> subProperties = resolver.getSubProperties("");
        for (String key : subProperties.keySet()) {
            if (!properties.containsKey(key)) {
                properties.setProperty(key, resolver.getProperty(key));
            }
        }
        interceptor.setProperties(properties);
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        resolver = new RelaxedPropertyResolver(environment, "pagehelper.");
    }

}