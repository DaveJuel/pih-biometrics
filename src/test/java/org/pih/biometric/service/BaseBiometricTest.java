/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.pih.biometric.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.pih.biometric.service.api.BiometricMatchingEngine;
import org.pih.biometric.service.model.BiometricsConfig;
import org.pih.biometric.service.model.BiometricsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Base test for biometrics functionality.
 * This requires valid neurotechnology component licenses to be placed in ${user.home}/.biometrics
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {BaseBiometricTest.TestConfig.class})
@WebAppConfiguration
public abstract class BaseBiometricTest {

    protected static final File LICENSE_DIR = new File(System.getProperty("user.home"), ".biometrics");
    protected static final File DB_FILE = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + ".db");

    protected MockMvc mockMvc;

    @Autowired
    protected BiometricMatchingEngine matchingEngine;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BiometricsConfig config;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        if (DB_FILE.exists()) {
            DB_FILE.delete();
        }
        DB_FILE.createNewFile();
        DB_FILE.deleteOnExit();
        loadTemplatesToDb();
    }

    /**
     * Sub-classes can override this method to load the database with initial templates
     */
    protected List<BiometricsTemplate> loadTemplatesToDb() throws Exception {
        return new ArrayList<>();
    }

    /**
     * Convenience method to load a template string from the classpath
     */
    protected BiometricsTemplate loadTemplateFromResource(String subjectId) throws Exception {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream("org/pih/biometric/service/"+subjectId);
        String template = new String(IOUtils.toByteArray(templateStream));
        return new BiometricsTemplate(subjectId, template);
    }

    /**
     * Convenience method to load a template to the database from the classpath
     */
    protected BiometricsTemplate loadTemplateToDb(String subjectId) throws Exception {
        BiometricsTemplate template = loadTemplateFromResource(subjectId);
        return matchingEngine.enroll(template);
    }

    @Configuration
    @Import(BiometricService.class)
    public static class TestConfig {

        @Bean
        public BiometricsConfig getConfig() {
            BiometricsConfig config = new BiometricsConfig();
            config.setMatchingServiceEnabled(true);
            config.setFingerprintScanningEnabled(true);
            config.setMatchingSpeed(BiometricsConfig.MatchingSpeed.LOW);
            config.setMatchingThreshold(72);
            config.setTemplateSize(BiometricsConfig.TemplateSize.LARGE);
            config.setSqliteDatabasePath(DB_FILE.getAbsolutePath());
            List<File> licenseFiles = new ArrayList<>();
            if (LICENSE_DIR.exists()) {
                for (File f : LICENSE_DIR.listFiles()) {
                    licenseFiles.add(f);
                }
            }
            config.setLicenseFiles(licenseFiles);
            return config;
        }
    }
}
