package com.omnip.beans;

import com.omnip.constant.OmniConstants;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class Beans {

    private final JwtDecoder jwtDecoder;
    private final UsersRepository usersRepository;
    private final HttpServletRequest session;

    public Beans(JwtDecoder jwtDecoder, UsersRepository usersRepository, HttpServletRequest session) {
        this.jwtDecoder = jwtDecoder;
        this.usersRepository = usersRepository;
        this.session = session;
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST,
            proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserDTO userDTO(HttpServletRequest request, HttpServlet httpServlet) {
        UserDTO dto = new UserDTO();

        // 1) Cek header Authorization
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            Jwt jwt = this.jwtDecoder.decode(token);
            String email = (String) jwt.getClaims().get("email");
            Users user = this.usersRepository.findByEmail(email);

            dto.setUsername(user.getUsername());
            dto.setEmail(email);
            dto.setFullname(user.getFullname());
            dto.setReferalId(user.getReferalId());
            dto.setRoles(user.getRoles());
        } else if (this.session != null) {
            UserDTO userDTO = (UserDTO) this.session.getAttribute(OmniConstants.SESSION_USER_DTO);
            if (userDTO != null) {
                dto = userDTO;
            }

        }

        return dto;
    }
}
