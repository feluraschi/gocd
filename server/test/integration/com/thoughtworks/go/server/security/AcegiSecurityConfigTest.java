package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.FileUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.SecurityConfig;
import org.springframework.security.intercept.web.DefaultFilterInvocationDefinitionSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-rest-servlet.xml"
})
public class AcegiSecurityConfigTest {
    @Autowired
    private org.springframework.security.intercept.web.FilterSecurityInterceptor filterInvocationInterceptor;
    private DefaultFilterInvocationDefinitionSource objectDefinitionSource;

    @Before
    public void setUp() throws Exception {
        objectDefinitionSource = (DefaultFilterInvocationDefinitionSource) filterInvocationInterceptor.getObjectDefinitionSource();
    }

    @Test
    public void shouldAllowOnlyRoleUserToHaveAccessToWildcardUrls() {
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**", "ROLE_USER");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.js", "ROLE_USER");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.css", "ROLE_USER");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.png", "ROLE_USER");
    }

    @Test
    public void shouldAllowAnonymousAccessToAssets() {
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**", "IS_AUTHENTICATED_ANONYMOUSLY");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.js", "IS_AUTHENTICATED_ANONYMOUSLY");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.css", "IS_AUTHENTICATED_ANONYMOUSLY");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.jpg", "IS_AUTHENTICATED_ANONYMOUSLY");
    }

    @Test
    public void shouldAllowAnonymousAccessToBackupInProgressPage() {
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/static/backup_in_progress.html?param=foo", "IS_AUTHENTICATED_ANONYMOUSLY");
    }

    @Test
    public void shouldNotAllowAnonymousAccessToWildcardAuthUrl(){
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/auth/login", "IS_AUTHENTICATED_ANONYMOUSLY");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/auth/logout", "IS_AUTHENTICATED_ANONYMOUSLY");
    }

    @Test
    public void shouldHaveDummyUrlForFunctionalTestMarkedAsNotForProduction() throws IOException {
        // "_not_in_production" part of the url should not be changed. Check change_rails_env_to_production task in prepare_go_server_webapp.rake to know why.
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/dummy_not_in_production", "IS_AUTHENTICATED_ANONYMOUSLY");
    }

    private void verifyGetAccessToUrlPatternIsAvailableToRole(DefaultFilterInvocationDefinitionSource objectDefinitionSource, String urlPattern, String role) {
        ConfigAttributeDefinition definition = objectDefinitionSource.lookupAttributes(urlPattern, "get");
        Iterator iterator = definition.getConfigAttributes().iterator();
        StringBuilder allowedAccess = new StringBuilder();
        while (iterator.hasNext()) {
            SecurityConfig securityConfig = (SecurityConfig) iterator.next();
            if (securityConfig.getAttribute().equals(role))
                return;
            else
                allowedAccess.append(securityConfig.getAttribute() + ",");
        }
        fail(String.format("Expected access to url %s only by %s but found %s", urlPattern, role, allowedAccess.toString()));
    }
}
