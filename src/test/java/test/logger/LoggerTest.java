/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.logger;

import fr.hhdev.logger.LoggerName;
import fr.hhdev.logger.LoggerProducer;
import java.io.File;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 *
 * @author hhfrancois
 */
@RunWith(Arquillian.class)
public class LoggerTest {

	@Inject
	private BeanIntercepted bean;
	
	@Inject
	private Logger logger;
	@Inject
	@LoggerName("TEST")
	private Logger loggerTest;

	/**
	 * Pour tester l'api dans le contener JEE on crée un ear
	 *
	 * @return
	 */
	@Deployment
	public static EnterpriseArchive createEarArchive() {
		File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();
		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
				  .addAsLibraries(libs)
				  .addAsLibraries(createLoggerLibArchive())
				  .addAsModule(createTestArchive());
		System.out.println(ear.toString(true));
		return ear;
	}

	/**
	 * logger est ajouté à l'ear en tant que librairie
	 *
	 * @return
	 */
	private static JavaArchive createLoggerLibArchive() {
		File beans = new File("src/main/resources/META-INF/beans.xml");
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "logger.jar")
				  .addAsManifestResource(new FileAsset(beans), ArchivePaths.create("beans.xml"))
				  .addPackages(true, LoggerProducer.class.getPackage());
		System.out.println(jar.toString(true));
		return jar;
	}

	/**
	 * Les classes de tests sont ajoutées à l'ear comme module ejb, car la classe doit être managé
	 *
	 * @return
	 */
	private static JavaArchive createTestArchive() {
		File logback = new File("src/test/resources/logback-test.groovy");
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-ejb.jar")
				  .addAsManifestResource(new FileAsset(logback), "logback-test.xml")
				  .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
				  .addPackages(true, LoggerTest.class.getPackage());
		System.out.println(jar.toString(true));
		return jar;
	}

	/**
	 * On test juste voir si le logger a bien été injecté, il ne doit pas être null
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoggerInjected() throws Exception {
		assertNotNull(logger);
		logger.info("Logger {} injected", this.getClass().getName());
		assertEquals(this.getClass().getName(), logger.getName());
	}

	/**
	 * On test juste voir si le logger a bien été injecté, il ne doit pas être null
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoggerTestInjected() throws Exception {
		assertNotNull(loggerTest);
		logger.info("Logger TEST injected");
		assertEquals("TEST", loggerTest.getName());
	}


	/**
	 * On test si le MDC a bien été mis à jour par l'intercepteur
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoggerMDCOnBean() throws Exception {
		logger.info("Logger MDC on Bean");
		MDC.put("MDC", null);
		bean.methodWithMDCUpdated();
		String value = MDC.get("MDC");
		assertEquals("TEST", value);
	}

	/**
	 * On test si le MDC a bien été mis à jour par l'intercepteur
	 * Mais qu'il est bien repositionné en sortant
	 *
	 * @throws Exception
	 */
	@Test
	public void tesCascadetLoggerMDCOnBean() throws Exception {
		testLoggerMDCOnBean();
		bean.method2WithMDCUpdated();
		String value = MDC.get("MDC");
		assertEquals("TEST", value);
	}

	/**
	 * On test si le MDC a bien été mis à jour par l'intercepteur
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoggerMDCOnEjb() throws Exception {
		logger.info("Logger MDC on Ejb");
		MDC.put("MDC", null);
		bean.methodWithCallEJB();
		String value = MDC.get("MDC");
		assertEquals("TEST2", value);
	}
}
