package com.abernathyclinic.history.controller;

import com.abernathyclinic.history.bean.PatientBean;
import com.abernathyclinic.history.exception.NoteNotFoundException;
import com.abernathyclinic.history.exception.PatientNotFoundException;
import com.abernathyclinic.history.model.Note;
import com.abernathyclinic.history.proxy.PatientProxy;
import com.abernathyclinic.history.repository.NoteRepository;
import com.abernathyclinic.history.service.NoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = NoteController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NoteControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    NoteService noteService;
    @MockBean
    NoteRepository noteRepository;
    @MockBean
    PatientProxy patientProxy;
    private PatientBean patient;
    private Note note;
    private Note otherNote;
    private Note samePatientNote;
    private List<Note> allNotes;
    private List<Note> testNonesNotes;
    private ObjectMapper mapper;
    private final String baseURI = "/patHistory";

    @BeforeAll
    public void setObjectMapper() {
        mapper = new Jackson2ObjectMapperBuilder().build();
    }

    @BeforeEach
    public void setUp() {
        note = new Note("NOTE001", 1, "Patient: TestNone Practitioner's notes/recommendations: Patient states that they are 'feeling terrific' Weight at or below recommended level");
        otherNote = new Note("NOTE002", 2, "Patient: TestBorderline Practitioner's notes/recommendations: Patient states that they are feeling a great deal of stress at work Patient also complains that their hearing seems Abnormal as of late");
        samePatientNote = new Note("NOTE003", 1, "Patient: TestNone Practitioner's notes/recommendations: Patient states that they are 'feeling very strong'");
        allNotes = List.of(note, otherNote, samePatientNote);
        testNonesNotes = List.of(note, samePatientNote);
        patient = new PatientBean(1, "TestNone", "Test", LocalDate.of(1966, 12, 31), "F", "1 Brookside St", "100-222-3333");
    }

    @Test
    @DisplayName("createNoteForPatient() should add a note to patient history")
    void createNoteForPatientTest() throws Exception {
        Note newNoteForTest = new Note("NOTE004", 1, "Patient: TestNone Practitioner's notes/recommendations: Patient has it easy");
        when(patientProxy.getPatientById(any(Integer.class))).thenReturn(patient);
        when(noteRepository.insert(any(Note.class))).thenReturn(newNoteForTest);
        when(noteService.createNote(any(Note.class))).thenReturn(newNoteForTest);

        mockMvc.perform(post(baseURI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newNoteForTest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(newNoteForTest.getId())))
                .andExpect(jsonPath("$.patId", is(newNoteForTest.getPatId())))
                .andExpect(jsonPath("$.content", is(newNoteForTest.getContent())));
    }

    @Test
    @DisplayName("createNoteForPatient() should return InvalidFormException")
    void createNoteForPatientTest_withInvalidFields_shouldReturn_InvalidFormException() throws Exception {
        Note noteWithNullPatientId = new Note("NOTE004", null, null);
        when(noteService.getPatientHistory(patient.getId())).thenReturn(testNonesNotes);
        when(patientProxy.getPatientById(any(Integer.class))).thenReturn(patient);
        when(noteRepository.insert(any(Note.class))).thenReturn(noteWithNullPatientId);

        mockMvc.perform(post(baseURI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(noteWithNullPatientId)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getPatientHistory() returns list of patient's notes")
    void getPatientHistorySuccessful() throws Exception {
        when(patientProxy.getPatientById(any(Integer.class))).thenReturn(patient);
        when(noteRepository.findAllByPatId(any(Integer.class))).thenReturn(testNonesNotes);
        when(noteService.getPatientHistory(any(Integer.class))).thenReturn(testNonesNotes);

        mockMvc.perform(get(baseURI+ "/patient/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(note.getId())))
                .andExpect(jsonPath("$[1].id", is(samePatientNote.getId())));
    }

    @Test
    @DisplayName("getPatientHistory() returns PatientNotFoundException")
    void getPatientHistoryReturnsPatientNotFoundException() throws Exception {
        when(noteService.getPatientHistory(any(Integer.class))).thenThrow(PatientNotFoundException.class);

        mockMvc.perform(get(baseURI + "/patient/155"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getNotes() successful")
    void getNotesTest() throws Exception {
        when(noteService.getNotes()).thenReturn(allNotes);

        mockMvc.perform(get(baseURI))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(note.getId())))
                .andExpect(jsonPath("$[1].id", is(otherNote.getId())))
                .andExpect(jsonPath("$[2].id", is(samePatientNote.getId())));
    }

    @Test
    @DisplayName("getNoteById() successful")
    void getNoteByIdSuccessful() throws Exception {
        when(noteService.getNoteById(any(String.class))).thenReturn(Optional.ofNullable(note));

        mockMvc.perform(get(baseURI + "/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(note.getId())));
    }

    @Test
    @DisplayName("getNoteById() returns NoteNotFoundException")
    void getNoteByIdReturnsNoteNotFoundException() throws Exception {
        when(noteService.getNoteById(any(String.class))).thenReturn(Optional.empty());

        mockMvc.perform(get(baseURI + "/1"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updateNoteById() should update note")
    void updateNoteTest() throws Exception {
        String updatedContent = "Patient: Updated Patient Practitioner's notes/recommendations: Patient states that they are 'feeling terrific'";
        note.setContent(updatedContent);
        when(noteRepository.findById(anyString())).thenReturn(Optional.ofNullable(note));
        when(patientProxy.getPatientById(any(Integer.class))).thenReturn(patient);
        when(noteRepository.save(any(Note.class))).thenReturn(note);
        when(noteService.updateNote(any(Note.class))).thenReturn(note);

        mockMvc.perform(put(baseURI + "/NOTE001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(note)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is(updatedContent)));
    }

    @Test
    @DisplayName("updateNoteById() should return InvalidFormException")
    void updateNote_withInvalidFields_shouldReturn_InvalidFormException() throws Exception {
        note.setContent("");
        when(noteRepository.findById(anyString())).thenReturn(Optional.ofNullable(note));
        when(patientProxy.getPatientById(any(Integer.class))).thenReturn(patient);
        when(noteRepository.save(any(Note.class))).thenReturn(note);
        when(noteService.updateNote(any(Note.class))).thenReturn(note);

        mockMvc.perform(put(baseURI + "/NOTE001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(note)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deleteNoteById() returns not found status")
    void deleteNoteByIdFails() throws Exception {
        when(noteRepository.findById(anyString())).thenReturn(Optional.empty());
        doThrow(NoteNotFoundException.class).when(noteService).deleteNoteById(anyString());
        mockMvc.perform(delete(baseURI + "/123"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteNoteById() successful")
    void deleteNoteByIdSuccessful() throws Exception {
        when(noteRepository.findById(anyString())).thenReturn(Optional.of(note));
        mockMvc.perform(delete(baseURI + "/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

}
