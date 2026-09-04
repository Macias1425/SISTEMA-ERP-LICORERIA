package com.licoreria.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licoreria.pos.dto.CambioPasswordDTO;
import com.licoreria.pos.dto.LoginRequestDTO;
import com.licoreria.pos.dto.LoginResponseDTO;
import com.licoreria.pos.dto.UsuarioCreateDTO;
import com.licoreria.pos.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAdminDevuelveToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void loginIgnoraTokenViejoYNoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer token-viejo-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void loginIncorrectoEs401SinRevelarUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("admin", "no-existe"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void meSinTokenEs401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void cajeroNoPuedeGestionarUsuarios() throws Exception {
        String token = login("cajero", "cajero123").getToken();

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UsuarioCreateDTO.builder()
                                .username("intruso")
                                .password("Clave1234")
                                .nombreCompleto("Intruso")
                                .rol(Rol.CAJERO)
                                .build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaUsuarioYCajeroCambiaPassword() throws Exception {
        String tokenAdmin = login("admin", "admin123").getToken();

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UsuarioCreateDTO.builder()
                                .username("caja02")
                                .password("Temporal1")
                                .nombreCompleto("Caja Dos")
                                .rol(Rol.CAJERO)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));

        String tokenCajero = login("caja02", "Temporal1").getToken();
        mockMvc.perform(get("/api/caja/abierta")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("DEBE_CAMBIAR_PASSWORD"));

        mockMvc.perform(post("/api/auth/cambiar-password")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambioPasswordDTO("Temporal1", "NuevaClave9", "NuevaClave9"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("caja02"))
                .andExpect(jsonPath("$.debeCambiarPassword").value(false));
    }

    private LoginResponseDTO login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponseDTO.class);
    }
}
