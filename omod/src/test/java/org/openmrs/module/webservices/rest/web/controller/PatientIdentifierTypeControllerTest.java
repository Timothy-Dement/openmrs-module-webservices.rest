package org.openmrs.module.webservices.rest.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.patient.impl.VerhoeffIdentifierValidator;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Tests functionality of {@link PatientIdentifierTypeController}. This does not use @should annotations because
 * the controller inherits those methods from a subclass
 */
public class PatientIdentifierTypeControllerTest extends BaseModuleWebContextSensitiveTest {

	String idTypeUuid = "1a339fe9-38bc-4ab3-b180-320988c0b968";
	
	private PatientService service;
	private PatientIdentifierTypeController controller;
	private WebRequest request;
	private HttpServletResponse response;
	
	@Before
	public void before() {
		this.service = Context.getPatientService();
		this.controller = new PatientIdentifierTypeController();
		this.request = new ServletWebRequest(new MockHttpServletRequest());
		this.response = new MockHttpServletResponse();
	}
	
	private void log(String label, Object object) {
		String toPrint;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.getSerializationConfig().set(SerializationConfig.Feature.INDENT_OUTPUT, true);
			toPrint = mapper.writeValueAsString(object);
		} catch (Exception ex) {
			toPrint = "" + object;
		}
		if (label != null)
			toPrint = label + ": " + toPrint;
		System.out.println(toPrint);
	}	
	
	@Test
	public void shouldGetOne() throws Exception {
		Object result = controller.retrieve(idTypeUuid, request);
		Assert.assertNotNull(result);
		log("Patient Identifier Type fetched (default)", result);
		Assert.assertEquals(idTypeUuid, PropertyUtils.getProperty(result, "uuid"));
		Assert.assertEquals("OpenMRS Identification Number", PropertyUtils.getProperty(result, "name"));
		Assert.assertNull(PropertyUtils.getProperty(result, "auditInfo"));
	}

	@Test
	public void shouldListAll() throws Exception {
		List<Object> result = controller.getAll(request, response);
		log("All identifier types", result);
		Assert.assertNotNull(result);
		Assert.assertEquals(3, result.size());
	}
	
	@Test
	public void shouldCreate() throws Exception {
		int before = service.getAllPatientIdentifierTypes().size();
		String json = "{ \"name\":\"My Type\", \"description\":\"My Way\", \"required\":true, \"checkDigit\":true, \"validator\":\"" + VerhoeffIdentifierValidator.class.getName() + "\" }";
		SimpleObject post = new ObjectMapper().readValue(json, SimpleObject.class);
		Object created = controller.create(post, request, response);
		log("Created", created);
		int after = service.getAllPatientIdentifierTypes().size();
		Assert.assertEquals(before + 1, after);
	}
	
	@Test
	public void shouldUpdate() throws Exception {
		String json = "{ \"description\":\"something new\" }";
		SimpleObject post = new ObjectMapper().readValue(json, SimpleObject.class);
		controller.update(idTypeUuid, post, request, response);
		PatientIdentifierType updated = service.getPatientIdentifierTypeByUuid(idTypeUuid);
		log("Updated", updated);
		Assert.assertNotNull(updated);
		Assert.assertEquals("OpenMRS Identification Number", updated.getName());
		Assert.assertEquals("something new", updated.getDescription());
	}

	@Test
	public void shouldDelete() throws Exception {
		PatientIdentifierType idType = service.getPatientIdentifierTypeByUuid(idTypeUuid);
		Assert.assertFalse(idType.isRetired());
		controller.delete(idTypeUuid, "unit test", request, response);
		idType = service.getPatientIdentifierTypeByUuid(idTypeUuid);
		Assert.assertTrue(idType.isRetired());
		Assert.assertEquals("unit test", idType.getRetireReason());
	}
	
	@Test(expected=Exception.class)
	public void shouldFailToPurge() throws Exception {
		Number before = (Number) Context.getAdministrationService().executeSQL("select count(*) from patient_identifier_type", true).get(0).get(0);
		controller.purge(idTypeUuid, request, response);
		Context.flushSession();
		Number after = (Number) Context.getAdministrationService().executeSQL("select count(*) from patient_identifier_type", true).get(0).get(0);
		Assert.assertEquals(before.intValue() - 1, after.intValue());
	}
	
}
