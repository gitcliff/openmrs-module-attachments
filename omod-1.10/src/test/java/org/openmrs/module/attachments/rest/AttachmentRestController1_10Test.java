package org.openmrs.module.attachments.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.module.attachments.AttachmentsConstants.ContentFamily;
import org.openmrs.module.attachments.AttachmentsContext;
import org.openmrs.module.attachments.obs.TestHelper;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.openmrs.obs.ComplexData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

public class AttachmentRestController1_10Test extends MainResourceControllerTest {
	
	@Autowired
	protected ObsService obsService;
	
	@Autowired
	protected TestHelper testHelper;
	
	@Autowired
	private AttachmentsContext attachmentsContext;
	
	private byte[] randomData = new byte[20];
	
	private Obs obs;
	
	@Before
	public void setup() throws IOException {
		testHelper.init();
		obs = testHelper.getTestComplexObs();
		new Random().nextBytes(randomData);
	}
	
	@After
	public void tearDown() throws IOException {
		testHelper.tearDown();
	}
	
	@Override
	public String getURI() {
		return AttachmentsConstants.ATTACHMENT_URI;
	}
	
	@Override
	public String getUuid() {
		return obs.getUuid();
	}
	
	@Override
	public long getAllCount() {
		return 0;
	}
	
	@Override
	@Test
	public void shouldGetAll() {
	}
	
	@Override
	@Test
	public void shouldGetDefaultByUuid() {
	}
	
	@Override
	@Test
	public void shouldGetRefByUuid() {
	}
	
	@Override
	@Test
	public void shouldGetFullByUuid() {
	}
	
	@Test
	public void saveAttachment_shouldUpdateObsComment() throws Exception {
		// Setup
		String editedComment = "Hello world!";
		String json = "{\"uuid\":\"" + getUuid() + "\",\"comment\":\"" + editedComment + "\"}";
		
		// Replay
		Object doc = deserialize(handle(newPostRequest(getURI() + "/" + getUuid(), json)));
		
		// Verify
		String comment = (String) PropertyUtils.getProperty(doc, "comment");
		assertEquals(editedComment, comment);
	}
	
	@Test
	public void getAttachment_shouldContainBytesMimeTypeOfAttachment() throws Exception {
		// Setup
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		
		// Replay
		SimpleObject result = deserialize(handle(req));
		
		// Verify
		assertEquals("application/octet-stream", result.get("bytesMimeType"));
	}
	
	@Test
	public void getAttachment_shouldContainBytesContentFamilyOfAttachment() throws Exception {
		// Setup
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		
		// Replay
		SimpleObject result = deserialize(handle(req));
		
		// Verify
		assertEquals(ContentFamily.OTHER.toString(), result.get("bytesContentFamily"));
	}
	
	@Test
	public void deleteAttachment_shouldVoidObs() throws Exception {
		
		// Setup
		File file = new File(testHelper.getTestComplexObsFilePath());
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + getUuid()));
		
		// Verify
		assertTrue(obsService.getObsByUuid(getUuid()).isVoided());
	}
	
	@Test
	public void deleteAttachmentWithMissingFile_shouldVoidObs() throws Exception {
		// Setup
		File file = new File(testHelper.getTestComplexObsFilePath());
		file.delete();
		assertFalse(file.exists());
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + getUuid()));
		
		// Verify
		assertTrue(obsService.getObsByUuid(getUuid()).isVoided());
	}
	
	@Test
	public void purgeAttachment_shouldPurgeObsAndRemoveFile() throws Exception {
		// Setup
		File file = new File(testHelper.getTestComplexObsFilePath());
		assertTrue(file.exists());
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + getUuid(), new Parameter("purge", "")));
		
		// Verify
		assertNull(obsService.getObsByUuid(getUuid()));
		assertFalse(file.exists());
	}
	
	@Test
	public void purgeAttachmentWithMissingFile_shouldPurgeObs() throws Exception {
		// Setup
		File file = new File(testHelper.getTestComplexObsFilePath());
		file.delete();
		assertFalse(file.exists());
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + getUuid(), new Parameter("purge", "")));
		
		// Verify
		assertNull(obsService.getObsByUuid(getUuid()));
	}
	
	@Test
	public void deleteAttachment_shouldVoidObsWithoutAssociatedEncounter() throws Exception {
		// Setup
		Obs obs = testHelper.getTestComplexObsWithoutAssociatedEncounterOrVisit();
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + obs.getUuid()));
		
		// Verify
		assertTrue(obsService.getObsByUuid(obs.getUuid()).isVoided());
	}
	
	@Test
	public void purgeAttachment_shouldPurgeObsWithoutAssociatedEncounter() throws Exception {
		// Setup
		Obs obs = testHelper.getTestComplexObsWithoutAssociatedEncounterOrVisit();
		
		// Replay
		handle(newDeleteRequest(getURI() + "/" + obs.getUuid(), new Parameter("purge", "")));
		
		// Verify
		assertNull(obsService.getObsByUuid(obs.getUuid()));
	}
	
	@Test
	public void postAttachment_shouldUploadFileToVisit() throws Exception {
		String fileCaption = "Test file caption";
		{
			// Setup
			String fileName = "testFile1.dat";
			Patient patient = Context.getPatientService().getPatient(2);
			Visit visit = Context.getVisitService().getVisit(1);
			
			MockMultipartHttpServletRequest request = newUploadRequest(getURI());
			MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", randomData);
			
			request.addFile(file);
			request.addParameter("patient", patient.getUuid());
			request.addParameter("visit", visit.getUuid());
			request.addParameter("fileCaption", fileCaption);
			
			// Replay
			SimpleObject response = deserialize(handle(request));
			
			Obs obs = Context.getObsService().getObsByUuid((String) response.get("uuid"));
			Obs complexObs = Context.getObsService().getComplexObs(obs.getObsId(), null);
			ComplexData complexData = complexObs.getComplexData();
			
			// Verify
			Assert.assertEquals(obs.getComment(), fileCaption);
			Assert.assertEquals(complexData.getTitle(), fileName);
			Assert.assertArrayEquals(randomData, (byte[]) complexData.getData());
			Assert.assertNotNull(obs.getEncounter());
			Assert.assertEquals(obs.getEncounter().getEncounterType(), attachmentsContext.getEncounterType());
		}
	}
	
	@Test
	public void postAttachment_shouldUploadFileAsEncounterless() throws Exception {
		String fileCaption = "Test file caption";
		
		// Setup
		String fileName = "testFile2.dat";
		Patient patient = Context.getPatientService().getPatient(2);
		
		MockMultipartHttpServletRequest request = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", randomData);
		
		request.addFile(file);
		request.addParameter("patient", patient.getUuid());
		request.addParameter("fileCaption", fileCaption);
		
		// Replay
		SimpleObject response = deserialize(handle(request));
		
		Obs obs = Context.getObsService().getObsByUuid((String) response.get("uuid"));
		Obs complexObs = Context.getObsService().getComplexObs(obs.getObsId(), null);
		ComplexData complexData = complexObs.getComplexData();
		
		// Verify
		Assert.assertEquals(obs.getComment(), fileCaption);
		Assert.assertEquals(complexData.getTitle(), fileName);
		Assert.assertArrayEquals(randomData, (byte[]) complexData.getData());
		Assert.assertNull(obs.getEncounter());
		
	}
	
	@Test
	public void postAttachment_shouldUploadFileToEncounter() throws Exception {
		String fileCaption = "Test file caption";
		// Setup
		String fileName = "testFile3.dat";
		Patient patient = Context.getPatientService().getPatient(2);
		Encounter encounter = testHelper.getTestEncounter();
		
		MockMultipartHttpServletRequest request = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", randomData);
		
		request.addFile(file);
		request.addParameter("patient", patient.getUuid());
		request.addParameter("encounter", encounter.getUuid());
		request.addParameter("fileCaption", fileCaption);
		
		// Replay
		SimpleObject response = deserialize(handle(request));
		
		Obs obs = Context.getObsService().getObsByUuid((String) response.get("uuid"));
		Obs complexObs = Context.getObsService().getComplexObs(obs.getObsId(), null);
		ComplexData complexData = complexObs.getComplexData();
		
		// Verify
		Assert.assertEquals(obs.getComment(), fileCaption);
		Assert.assertEquals(complexData.getTitle(), fileName);
		Assert.assertArrayEquals(randomData, (byte[]) complexData.getData());
		Assert.assertEquals(obs.getEncounter().getUuid(), encounter.getUuid());
	}
	
	@Test(expected = IllegalRequestException.class)
	public void postAttachment_shouldThrowWhenVisitAndEncounterDoNotMatch() throws Exception {
		// Setup
		String fileCaption = "Test file caption";
		String fileName = "testFile1.dat";
		Patient patient = Context.getPatientService().getPatient(2);
		Visit visit = Context.getVisitService().getVisit(1);
		Encounter encounter = Context.getEncounterService().getEncounter(3);
		
		MockMultipartHttpServletRequest request = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", randomData);
		
		request.addFile(file);
		request.addParameter("patient", patient.getUuid());
		request.addParameter("visit", visit.getUuid());
		request.addParameter("encounter", encounter.getUuid());
		request.addParameter("fileCaption", fileCaption);
		
		// Replay
		SimpleObject response = deserialize(handle(request));
	}
	
	@Test(expected = IllegalRequestException.class)
	public void postAttachment_shouldNotUploadFileAboveSizeLimit() throws Exception {
		// Setup
		String fileCaption = "Test file caption";
		String fileName = "testFile1.dat";
		Patient patient = Context.getPatientService().getPatient(2);
		Visit visit = Context.getVisitService().getVisit(1);
		
		byte[] maxData = new byte[(int) ((attachmentsContext.getMaxUploadFileSize() * 1024 * 1024) + 1)];
		
		MockMultipartHttpServletRequest request = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", maxData);
		
		request.addFile(file);
		request.addParameter("patient", patient.getUuid());
		request.addParameter("visit", visit.getUuid());
		request.addParameter("fileCaption", fileCaption);
		
		// Replay
		SimpleObject response = deserialize(handle(request));
	}
	
	@Test
	public void getAttachmentBytes_shouldDownloadFile() throws Exception {
		
		// Setup
		String fileCaption = "Test file caption";
		String uuid;
		String mimeType = "application/octet-stream";
		String fileExtension = "dat";
		String fileName = "testFile." + fileExtension;
		
		// Upload Test File
		Patient patient = Context.getPatientService().getPatient(2);
		MockMultipartHttpServletRequest uploadRequest = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, mimeType, randomData);
		
		uploadRequest.addFile(file);
		uploadRequest.addParameter("patient", patient.getUuid());
		uploadRequest.addParameter("fileCaption", fileCaption);
		
		SimpleObject uploadResponse = deserialize(handle(uploadRequest));
		uuid = uploadResponse.get("uuid");
		
		HttpServletRequest downloadRequest = newGetRequest(getURI() + "/" + uuid + "/bytes");
		
		// Replay
		MockHttpServletResponse downloadResponse = handle(downloadRequest);
		byte[] bytesContent = downloadResponse.getContentAsByteArray();
		
		// Verify
		Assert.assertArrayEquals(randomData, bytesContent);
		Assert.assertEquals(downloadResponse.getContentType(), mimeType);
		Assert.assertEquals(downloadResponse.getHeader("File-Name"), fileName);
		Assert.assertEquals(downloadResponse.getHeader("File-Ext"), fileExtension);
		
	}
	
	@Test
	public void doSearch() throws Exception {
		// First create a patient and save an attachment for patient
		
		String fileCaption = "Test file caption";
		String uuid;
		String mimeType = "application/octet-stream";
		String fileExtension = "dat";
		String fileName = "testFile." + fileExtension;
		
		Patient patient = createBasicPatient();
		MockMultipartHttpServletRequest uploadRequest = newUploadRequest(getURI());
		MockMultipartFile file = new MockMultipartFile("file", fileName, mimeType, randomData);
		
		uploadRequest.addFile(file);
		uploadRequest.addParameter("patient", patient.getUuid());
		uploadRequest.addParameter("fileCaption", fileCaption);
		
		// Save attachment
		deserialize(handle(uploadRequest));
		
		// Search attachments for patient
		MockHttpServletRequest request = request(RequestMethod.GET, getURI());
		request.addParameter("patient", patient.getUuid());
		SimpleObject response = deserialize(handle(request));
		LinkedHashMap<String, String> result = (LinkedHashMap<String, String>) ((ArrayList<LinkedHashMap>) response
		        .get("results")).get(0);
		
		// Delete created patient
		Context.getPatientService().purgePatient(patient);
		Assert.assertEquals("application/octet-stream", result.get("bytesMimeType"));
		Assert.assertEquals(ContentFamily.OTHER.toString(), result.get("bytesContentFamily"));
	}
	
	private Patient createBasicPatient() {
		Patient patient = new Patient();
		
		PersonName pName = new PersonName();
		pName.setGivenName("Tom");
		pName.setMiddleName("E.");
		pName.setFamilyName("Patient");
		patient.addName(pName);
		
		PersonAddress pAddress = new PersonAddress();
		pAddress.setAddress1("123 My street");
		pAddress.setAddress2("Apt 402");
		pAddress.setCityVillage("Anywhere city");
		pAddress.setCountry("Some Country");
		Set<PersonAddress> pAddressList = patient.getAddresses();
		pAddressList.add(pAddress);
		patient.setAddresses(pAddressList);
		patient.addAddress(pAddress);
		// patient.removeAddress(pAddress);
		
		patient.setBirthdateEstimated(true);
		patient.setBirthdate(new Date());
		patient.setBirthdateEstimated(true);
		patient.setDeathDate(new Date());
		patient.setCauseOfDeath(new Concept());
		patient.setGender("male");
		
		List<PatientIdentifierType> patientIdTypes = Context.getPatientService().getAllPatientIdentifierTypes();
		
		PatientIdentifier patientIdentifier = new PatientIdentifier();
		patientIdentifier.setIdentifier("123-0");
		patientIdentifier.setIdentifierType(patientIdTypes.get(0));
		patientIdentifier.setLocation(new Location(1));
		patientIdentifier.setPreferred(true);
		
		Set<PatientIdentifier> patientIdentifiers = new LinkedHashSet<>();
		patientIdentifiers.add(patientIdentifier);
		
		patient.setIdentifiers(patientIdentifiers);
		
		return Context.getPatientService().savePatient(patient);
	}
	
}
