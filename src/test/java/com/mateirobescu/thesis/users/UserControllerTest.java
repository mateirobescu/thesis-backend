package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.NotFoundException;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the HTTP layer of {@code UserController}: routes, status codes and JSON, with the real exception handler.
 * The security filters are switched off and the service is mocked, so nothing here touches a database or a token.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private User user(UUID id, String email, String firstName, String lastName) {
        return Instancio.of(User.class)
                .set(field(User::getId), id)
                .set(field(User::getEmail), email)
                .set(field(User::getFirstName), firstName)
                .set(field(User::getLastName), lastName)
                .create();
    }

    private User johnDoe(UUID id) {
        return user(id, "john.doe@example.com", "John", "Doe");
    }

    @Nested
    class GetAllUsers {

        @Test
        void should_returnTheUsersWithoutSensitiveFields_whenListingUsers() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(
                    johnDoe(UUID.randomUUID()),
                    user(UUID.randomUUID(), "jane.doe@example.com", "Jane", "Doe")));

            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.users[0].email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.users[1].firstName").value("Jane"))
                    .andExpect(jsonPath("$.users[0].passwordHash").doesNotExist());
        }
    }

    @Nested
    class GetUser {

        @Test
        void should_returnTheUser_whenUserExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(userService.getUserById(id)).thenReturn(johnDoe(id));

            mockMvc.perform(get("/users/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void should_return404_whenUserDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(userService.getUserById(id)).thenThrow(new NotFoundException("User", id));

            mockMvc.perform(get("/users/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenUserIdIsNotAUuid() throws Exception {
            mockMvc.perform(get("/users/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(userService);
        }
    }

    @Nested
    class PatchUser {

        private String body(UserPatchRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_passThePatchToTheServiceAndReturnTheUser_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UserPatchRequest request = new UserPatchRequest("jane.doe@example.com", "Jane", "Doe");
            when(userService.patchUser(any(), any())).thenReturn(user(id, "jane.doe@example.com", "Jane", "Doe"));

            mockMvc.perform(patch("/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                    .andExpect(jsonPath("$.firstName").value("Jane"));

            ArgumentCaptor<UserPatchCommand> commandCaptor = ArgumentCaptor.forClass(UserPatchCommand.class);
            verify(userService).patchUser(any(), commandCaptor.capture());
            assertEquals("jane.doe@example.com", commandCaptor.getValue().email());
            assertEquals("Jane", commandCaptor.getValue().firstName());
            assertEquals("Doe", commandCaptor.getValue().lastName());
        }

        @Test
        void should_return400WithFieldErrors_whenEmailIsInvalid() throws Exception {
            UserPatchRequest request = new UserPatchRequest("not-an-email", null, null);

            mockMvc.perform(patch("/users/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.email").isNotEmpty());

            verifyNoInteractions(userService);
        }

        @Test
        void should_return409WithFieldErrors_whenEmailIsAlreadyTaken() throws Exception {
            UserPatchRequest request = new UserPatchRequest("jane.doe@example.com", null, null);
            when(userService.patchUser(any(), any()))
                    .thenThrow(new ConflictException("email", "An account with this email already exists"));

            mockMvc.perform(patch("/users/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.email[0]").value("An account with this email already exists"));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void should_return204AndDeleteTheUser_whenUserExists() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/users/{id}", id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(userService).deleteUserById(id);
        }

        @Test
        void should_return404_whenUserDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new NotFoundException("User", id)).when(userService).deleteUserById(id);

            mockMvc.perform(delete("/users/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
